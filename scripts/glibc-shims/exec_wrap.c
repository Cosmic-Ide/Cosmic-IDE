#define _GNU_SOURCE

#include "exec_wrap.h"

#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <pthread.h>
#include <spawn.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

#ifndef PATH_MAX
#define PATH_MAX 4096
#endif

/*
 * Compile-time policy switches. Flip these here if a build needs different
 * behavior. Runtime env toggles are intentionally avoided. The wrapper derives
 * the custom linker path from this preload library location, and reads
 * normal Unix variables only: LD_LIBRARY_PATH, LD_PRELOAD, TMPDIR, SHELL, PATH.
 * Internal executable-identity variables are defined in exec_wrap.h and are
 * written only into wrapped child environments.
 */
#ifndef EXEC_WRAP_ENABLE
#define EXEC_WRAP_ENABLE 1
#endif

#ifndef EXEC_WRAP_ENABLE_SCRIPT_WRAP
#define EXEC_WRAP_ENABLE_SCRIPT_WRAP 1
#endif

#ifndef EXEC_WRAP_ENABLE_JAVA_TMPDIR_INJECTION
#define EXEC_WRAP_ENABLE_JAVA_TMPDIR_INJECTION 1
#endif

#ifndef EXEC_WRAP_ALLOW_ANDROID_SYSTEM_EXEC
#define EXEC_WRAP_ALLOW_ANDROID_SYSTEM_EXEC 0
#endif

extern char** environ;

typedef int (*execve_fn_t)(const char*, char* const[], char* const[]);
typedef int (*execvp_fn_t)(const char*, char* const[]);
typedef int (*execvpe_fn_t)(const char*, char* const[], char* const[]);
typedef int (*fexecve_fn_t)(int, char* const[], char* const[]);
typedef int (*posix_spawn_fn_t)(
    pid_t*,
    const char*,
    const posix_spawn_file_actions_t*,
    const posix_spawnattr_t*,
    char* const[],
    char* const[]
);

typedef struct {
    char** items;
    size_t count;
} owned_vec_t;

typedef enum {
    KIND_NONE = 0,
    KIND_ELF = 1,
    KIND_SCRIPT = 2
} exec_target_kind_t;

static void* lookup_next_symbol(const char* name) {
    dlerror();
    return dlsym(RTLD_NEXT, name);
}

static void require_symbol_or_abort(const char* name, void* symbol) {
    if (!symbol) {
        fprintf(stderr, "exec_wrap: failed to resolve symbol: %s\n", name);
        abort();
    }
}

#define DECLARE_REAL_SYMBOL(sym)                                      \
    static void* real_##sym##_ptr = NULL;                             \
    static pthread_once_t real_##sym##_once = PTHREAD_ONCE_INIT;       \
    static void init_##sym(void) {                                     \
        real_##sym##_ptr = lookup_next_symbol(#sym);                   \
    }                                                                  \
    static void* get_##sym(int required) {                             \
        pthread_once(&real_##sym##_once, init_##sym);                  \
        if (required) require_symbol_or_abort(#sym, real_##sym##_ptr); \
        return real_##sym##_ptr;                                       \
    }

#define REAL(sym, type) ((type) get_##sym(1))
#define OPT_REAL(sym, type) ((type) get_##sym(0))

DECLARE_REAL_SYMBOL(execve)
DECLARE_REAL_SYMBOL(execvp)
DECLARE_REAL_SYMBOL(execvpe)
DECLARE_REAL_SYMBOL(fexecve)
DECLARE_REAL_SYMBOL(posix_spawn)
DECLARE_REAL_SYMBOL(posix_spawnp)

static __thread int wrapping_now = 0;

static int trace_enabled(void) {
    const char* v = getenv("EXEC_WRAP_TRACE");
    return v && *v && strcmp(v, "0") != 0;
}

static void tracef(const char* fmt, ...) {
    if (!trace_enabled()) return;

    va_list ap;
    va_start(ap, fmt);
    fprintf(stderr, "exec_wrap: ");
    vfprintf(stderr, fmt, ap);
    fprintf(stderr, "\n");
    va_end(ap);
}

static const char* env_get_from(char* const envp[], const char* key) {
    if (!key || !*key) return NULL;

    size_t key_len = strlen(key);
    if (envp) {
        for (size_t i = 0; envp[i]; i++) {
            if (strncmp(envp[i], key, key_len) == 0 && envp[i][key_len] == '=') {
                return envp[i] + key_len + 1;
            }
        }
    }

    return getenv(key);
}

static int path_has_slash(const char* path) {
    return path && strchr(path, '/') != NULL;
}

static const char* base_name_const(const char* path) {
    if (!path) return "";
    const char* slash = strrchr(path, '/');
    return slash ? slash + 1 : path;
}

static int same_path_string(const char* a, const char* b) {
    return a && b && strcmp(a, b) == 0;
}

static int string_starts_with(const char* value, const char* prefix) {
    if (!value || !prefix) return 0;
    size_t prefix_len = strlen(prefix);
    return strncmp(value, prefix, prefix_len) == 0;
}

static int string_ends_with(const char* value, const char* suffix) {
    if (!value || !suffix) return 0;
    size_t value_len = strlen(value);
    size_t suffix_len = strlen(suffix);
    if (value_len < suffix_len) return 0;
    return strcmp(value + value_len - suffix_len, suffix) == 0;
}

static int is_android_linker_path(const char* path) {
    if (!path) return 0;
    const char* base = base_name_const(path);
    return strcmp(base, "linker") == 0 || strcmp(base, "linker64") == 0;
}

static int is_android_system_path(const char* path) {
    if (!path) return 0;

    return string_starts_with(path, "/system/") ||
           string_starts_with(path, "/apex/") ||
           string_starts_with(path, "/vendor/") ||
           string_starts_with(path, "/odm/") ||
           string_starts_with(path, "/product/");
}

static int read_file_prefix(const char* path, unsigned char* buffer, size_t capacity, ssize_t* out_count) {
    if (!path || !*path || !buffer || capacity == 0 || !out_count) return -1;

    int fd = open(path, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return -1;

    ssize_t n = read(fd, buffer, capacity);
    int saved_errno = errno;
    close(fd);
    errno = saved_errno;

    if (n < 0) return -1;
    *out_count = n;
    return 0;
}

static int is_readable_path(const char* path) {
    return path && *path && access(path, R_OK) == 0;
}

static int is_probably_elf_file(const char* path) {
    unsigned char magic[4];
    ssize_t n = 0;
    if (read_file_prefix(path, magic, sizeof(magic), &n) != 0) return 0;

    return n == 4 && magic[0] == 0x7f && magic[1] == 'E' && magic[2] == 'L' && magic[3] == 'F';
}

static int is_readable_elf_file(const char* path) {
    return is_readable_path(path) && is_probably_elf_file(path);
}

static int is_known_shell_name(const char* name) {
    if (!name || !*name) return 0;

    const char* base = base_name_const(name);
    return strcmp(base, "sh") == 0 ||
           strcmp(base, "bash") == 0 ||
           strcmp(base, "dash") == 0 ||
           strcmp(base, "ash") == 0 ||
           strcmp(base, "ksh") == 0 ||
           strcmp(base, "zsh") == 0;
}

static int shebang_uses_shell(const char* line) {
    if (!line || strncmp(line, "#!", 2) != 0) return 0;

    const char* p = line + 2;
    while (*p == ' ' || *p == '\t') p++;

    char interpreter[PATH_MAX];
    size_t len = 0;
    while (*p && *p != ' ' && *p != '\t' && *p != '\n' && *p != '\r' && len + 1 < sizeof(interpreter)) {
        interpreter[len++] = *p++;
    }
    interpreter[len] = '\0';

    while (*p == ' ' || *p == '\t') p++;

    if (is_known_shell_name(interpreter)) return 1;

    const char* interp_base = base_name_const(interpreter);
    if (strcmp(interp_base, "env") == 0) {
        while (*p && *p != '\n' && *p != '\r') {
            while (*p == ' ' || *p == '\t') p++;
            if (!*p || *p == '\n' || *p == '\r') break;

            char arg[128];
            size_t arg_len = 0;
            while (*p && *p != ' ' && *p != '\t' && *p != '\n' && *p != '\r' && arg_len + 1 < sizeof(arg)) {
                arg[arg_len++] = *p++;
            }
            arg[arg_len] = '\0';

            if (arg[0] == '-') continue;
            return is_known_shell_name(arg);
        }
    }

    return 0;
}

static int is_shell_script_file(const char* path) {
    unsigned char buffer[512];
    ssize_t n = 0;
    if (read_file_prefix(path, buffer, sizeof(buffer) - 1, &n) != 0) return 0;
    if (n < 2 || buffer[0] != '#' || buffer[1] != '!') return 0;

    buffer[n] = '\0';
    return shebang_uses_shell((const char*) buffer);
}

static char* duplicate_string(const char* value) {
    if (!value) return NULL;
    char* out = strdup(value);
    if (!out) errno = ENOMEM;
    return out;
}

static char* join_key_value(const char* key, const char* value) {
    if (!key || !value) return NULL;

    size_t key_len = strlen(key);
    size_t value_len = strlen(value);
    char* out = (char*) malloc(key_len + 1 + value_len + 1);
    if (!out) {
        errno = ENOMEM;
        return NULL;
    }

    memcpy(out, key, key_len);
    out[key_len] = '=';
    memcpy(out + key_len + 1, value, value_len);
    out[key_len + 1 + value_len] = '\0';
    return out;
}

static char* join_path2(const char* dir, const char* name) {
    if (!dir || !*dir || !name || !*name) return NULL;

    size_t dir_len = strlen(dir);
    size_t name_len = strlen(name);
    int needs_slash = dir[dir_len - 1] != '/';

    char* out = (char*) malloc(dir_len + (needs_slash ? 1 : 0) + name_len + 1);
    if (!out) {
        errno = ENOMEM;
        return NULL;
    }

    memcpy(out, dir, dir_len);
    size_t index = dir_len;
    if (needs_slash) out[index++] = '/';
    memcpy(out + index, name, name_len);
    out[index + name_len] = '\0';
    return out;
}

static char* first_path_component(const char* path_list) {
    if (!path_list || !*path_list) return NULL;

    const char* end = strchr(path_list, ':');
    size_t len = end ? (size_t) (end - path_list) : strlen(path_list);
    if (len == 0) return duplicate_string(".");

    char* out = (char*) malloc(len + 1);
    if (!out) {
        errno = ENOMEM;
        return NULL;
    }
    memcpy(out, path_list, len);
    out[len] = '\0';
    return out;
}

static char* directory_of_path(const char* path) {
    if (!path || !*path) return NULL;

    const char* slash = strrchr(path, '/');
    if (!slash) return duplicate_string(".");

    size_t len = (slash == path) ? 1 : (size_t) (slash - path);
    char* out = (char*) malloc(len + 1);
    if (!out) {
        errno = ENOMEM;
        return NULL;
    }

    memcpy(out, path, len);
    out[len] = '\0';
    return out;
}

static char* absolute_canonical_path(const char* path) {
    if (!path || !*path) return NULL;

    /*
     * /proc/self/exe normally exposes an absolute, symlink-resolved path. Use
     * realpath first so resource-relative launchers observe the same shape.
     */
    char* canonical = realpath(path, NULL);
    if (canonical) return canonical;

    int saved_errno = errno;

    if (path[0] == '/') {
        char* absolute = duplicate_string(path);
        if (absolute) errno = saved_errno;
        return absolute;
    }

    char cwd[PATH_MAX];
    if (!getcwd(cwd, sizeof(cwd))) {
        errno = saved_errno;
        return duplicate_string(path);
    }

    char* absolute = join_path2(cwd, path);
    if (absolute) errno = saved_errno;
    return absolute;
}

static char* resolve_ld_linux_path(void) {
    Dl_info info;
    memset(&info, 0, sizeof(info));

    if (dladdr((void*) &resolve_ld_linux_path, &info) == 0 || !info.dli_fname || !*info.dli_fname) {
        tracef("unable to resolve own preload library path with dladdr");
        return NULL;
    }

    char* dir = directory_of_path(info.dli_fname);
    if (!dir) return NULL;

    char* ld_linux = join_path2(dir, "libld_linux.so");
    free(dir);

    if (!ld_linux) return NULL;
    if (access(ld_linux, X_OK) != 0) {
        tracef("derived linker is not executable: %s", ld_linux);
        free(ld_linux);
        return NULL;
    }

    return ld_linux;
}

static size_t argv_count(char* const argv[]) {
    size_t count = 0;
    if (!argv) return 0;
    while (argv[count]) count++;
    return count;
}

static size_t env_count(char* const envp[]) {
    size_t count = 0;
    if (!envp) return 0;
    while (envp[count]) count++;
    return count;
}

static int env_key_matches(const char* entry, const char* key) {
    if (!entry || !key) return 0;
    size_t key_len = strlen(key);
    return strncmp(entry, key, key_len) == 0 && entry[key_len] == '=';
}

static int env_index_of(char** env, size_t count, const char* key) {
    for (size_t i = 0; i < count; i++) {
        if (env_key_matches(env[i], key)) return (int) i;
    }
    return -1;
}

static int env_set_owned(char*** env_ref, size_t* count_ref, const char* key, const char* value) {
    if (!value) return 0;

    char* entry = join_key_value(key, value);
    if (!entry) return -1;

    int index = env_index_of(*env_ref, *count_ref, key);
    if (index >= 0) {
        free((*env_ref)[index]);
        (*env_ref)[index] = entry;
        return 0;
    }

    char** grown = (char**) realloc(*env_ref, sizeof(char*) * (*count_ref + 2));
    if (!grown) {
        free(entry);
        errno = ENOMEM;
        return -1;
    }

    *env_ref = grown;
    (*env_ref)[*count_ref] = entry;
    (*count_ref)++;
    (*env_ref)[*count_ref] = NULL;
    return 0;
}

static int env_copy_owned(char* const input_envp[], owned_vec_t* out) {
    memset(out, 0, sizeof(*out));

    if (!input_envp) input_envp = environ;

    size_t count = env_count(input_envp);
    char** copy = (char**) calloc(count + 1, sizeof(char*));
    if (!copy) {
        errno = ENOMEM;
        return -1;
    }

    for (size_t i = 0; i < count; i++) {
        copy[i] = duplicate_string(input_envp[i]);
        if (!copy[i]) {
            for (size_t j = 0; j < i; j++) free(copy[j]);
            free(copy);
            return -1;
        }
    }

    copy[count] = NULL;
    out->items = copy;
    out->count = count;
    return 0;
}

static void owned_vec_free(owned_vec_t* vec) {
    if (!vec || !vec->items) return;

    for (size_t i = 0; vec->items[i]; i++) free(vec->items[i]);
    free(vec->items);
    vec->items = NULL;
    vec->count = 0;
}

static int build_child_env(
    char* const envp[],
    const char* library_path,
    const char* preload,
    const char* executable_identity,
    const char* loader_identity,
    owned_vec_t* out
) {
    if (env_copy_owned(envp, out) != 0) return -1;

    if (library_path && *library_path) {
        if (env_set_owned(&out->items, &out->count, "LD_LIBRARY_PATH", library_path) != 0) goto fail;
    }
    if (preload && *preload) {
        if (env_set_owned(&out->items, &out->count, "LD_PRELOAD", preload) != 0) goto fail;
    }
    if (executable_identity && *executable_identity) {
        if (env_set_owned(
                &out->items,
                &out->count,
                EXEC_WRAP_EXECUTABLE_ENV,
                executable_identity
            ) != 0) goto fail;
    }
    if (loader_identity && *loader_identity) {
        if (env_set_owned(
                &out->items,
                &out->count,
                EXEC_WRAP_LOADER_ENV,
                loader_identity
            ) != 0) goto fail;
    }
    return 0;

fail:
    owned_vec_free(out);
    return -1;
}

static char* resolve_from_path(const char* file, char* const envp[]) {
    if (!file || !*file) return NULL;

    if (path_has_slash(file)) {
        return duplicate_string(file);
    }

    const char* path_env = env_get_from(envp, "PATH");
    if (!path_env || !*path_env) path_env = "/bin:/usr/bin:/system/bin";

    char* paths = duplicate_string(path_env);
    if (!paths) return NULL;

    char* save = NULL;
    for (char* dir = strtok_r(paths, ":", &save); dir; dir = strtok_r(NULL, ":", &save)) {
        if (!*dir) dir = ".";

        char* candidate = join_path2(dir, file);
        if (!candidate) {
            free(paths);
            return NULL;
        }

        /*
         * Do not require X_OK here.
         *
         * For normal Unix execvp, X_OK would be correct. But this wrapper may
         * feed the resolved file to ld-linux as an argument, not execute it
         * directly. Symlinked/read-only ELF files are valid in that mode.
         */
        if (access(candidate, R_OK) == 0) {
            free(paths);
            return candidate;
        }

        free(candidate);
    }

    free(paths);
    return NULL;
}

static char* resolve_for_exec_lookup(const char* file, char* const envp[], int search_path) {
    if (!file || !*file) return NULL;
    return search_path ? resolve_from_path(file, envp) : duplicate_string(file);
}

static char* resolve_glibc_bin_replacement(const char* resolved_path, const char* library_path) {
    if (!resolved_path || !*resolved_path || !library_path || !*library_path) return NULL;

    /*
     * If a script asks for /system/bin/uname, /system/bin/sh, etc. but an
     * equivalent glibc-side binary exists, prefer the glibc one. This prevents
     * shell scripts from escaping to Android/bionic tools just because they used
     * an absolute /system path or PATH resolved there first.
     */
    if (!is_android_system_path(resolved_path)) return NULL;

    const char* base = base_name_const(resolved_path);
    if (!base || !*base || strchr(base, '/') != NULL) return NULL;

    char* glibc_root = first_path_component(library_path);
    if (!glibc_root) return NULL;

    char* bin_dir = join_path2(glibc_root, "bin");
    free(glibc_root);
    if (!bin_dir) return NULL;

    char* candidate = join_path2(bin_dir, base);
    free(bin_dir);
    if (!candidate) return NULL;

    if (is_readable_elf_file(candidate)) {
        tracef("replace Android tool %s with glibc tool %s", resolved_path, candidate);
        return candidate;
    }

    free(candidate);
    return NULL;
}

static int env_unset_owned(char*** env_ref, size_t* count_ref, const char* key) {
    int index = env_index_of(*env_ref, *count_ref, key);
    if (index < 0) return 0;

    free((*env_ref)[index]);
    for (size_t i = (size_t) index; i + 1 < *count_ref; i++) {
        (*env_ref)[i] = (*env_ref)[i + 1];
    }
    (*count_ref)--;
    (*env_ref)[*count_ref] = NULL;
    return 0;
}

static int build_android_delegate_env(char* const envp[], owned_vec_t* out) {
    if (env_copy_owned(envp, out) != 0) return -1;

    const char* remove_keys[] = {
        "LD_PRELOAD",
        "LD_LIBRARY_PATH",
        "LD_AUDIT",
        "LD_DEBUG",
        "NSS_WEAK_ROUTE_CONFIG",
        "RESOLV_CONF_PATH",
        "HOSTS_PATH",
        "NSSWITCH_CONF_PATH",
        "GAI_CONF_PATH",
        EXEC_WRAP_EXECUTABLE_ENV,
        EXEC_WRAP_LOADER_ENV,
    };

    for (size_t i = 0; i < sizeof(remove_keys) / sizeof(remove_keys[0]); i++) {
        if (env_unset_owned(&out->items, &out->count, remove_keys[i]) != 0) {
            owned_vec_free(out);
            return -1;
        }
    }

    return 0;
}

static int delegate_android_execve_if_needed(
    execve_fn_t real_execve,
    const char* requested_path,
    char* const argv[],
    char* const envp[],
    int search_path
) {
#if EXEC_WRAP_ALLOW_ANDROID_SYSTEM_EXEC
    (void) real_execve;
    (void) requested_path;
    (void) argv;
    (void) envp;
    (void) search_path;
    return 0;
#else
    char* resolved = resolve_for_exec_lookup(requested_path, envp, search_path);
    if (!resolved) return 0;

    if (!is_android_system_path(resolved) && !is_android_linker_path(resolved)) {
        free(resolved);
        return 0;
    }

    owned_vec_t clean_env;
    if (build_android_delegate_env(envp, &clean_env) != 0) {
        free(resolved);
        return -1;
    }

    tracef("delegate Android executable with sanitized env: %s -> %s", requested_path, resolved);

    int rc = real_execve(resolved, argv, clean_env.items);
    int saved_errno = errno;

    owned_vec_free(&clean_env);
    free(resolved);
    errno = saved_errno;
    return rc == -1 ? -1 : 1;
#endif
}

static int delegate_android_spawn_if_needed(
    posix_spawn_fn_t real_posix_spawn,
    pid_t* pid,
    const char* requested_path,
    const posix_spawn_file_actions_t* file_actions,
    const posix_spawnattr_t* attrp,
    char* const argv[],
    char* const envp[],
    int search_path
) {
#if EXEC_WRAP_ALLOW_ANDROID_SYSTEM_EXEC
    (void) real_posix_spawn;
    (void) pid;
    (void) requested_path;
    (void) file_actions;
    (void) attrp;
    (void) argv;
    (void) envp;
    (void) search_path;
    return 0;
#else
    char* resolved = resolve_for_exec_lookup(requested_path, envp, search_path);
    if (!resolved) return 0;

    if (!is_android_system_path(resolved) && !is_android_linker_path(resolved)) {
        free(resolved);
        return 0;
    }

    owned_vec_t clean_env;
    if (build_android_delegate_env(envp, &clean_env) != 0) {
        free(resolved);
        return errno ? errno : ENOMEM;
    }

    tracef("delegate Android spawn with sanitized env: %s -> %s", requested_path, resolved);

    int rc = real_posix_spawn(pid, resolved, file_actions, attrp, argv, clean_env.items);

    owned_vec_free(&clean_env);
    free(resolved);
    return rc ? rc : 1;
#endif
}

static char* first_existing_elf_path(const char* a, const char* b) {
    if (a && *a && is_readable_elf_file(a)) return duplicate_string(a);
    if (b && *b && is_readable_elf_file(b)) return duplicate_string(b);
    return NULL;
}

static char* resolve_script_shell(char* const envp[], const char* glibc_root) {
    const char* explicit_shell = env_get_from(envp, "SHELL");
    if (explicit_shell && *explicit_shell) {
        if (is_readable_elf_file(explicit_shell)) {
            return duplicate_string(explicit_shell);
        }
        tracef("SHELL ignored for script wrapper, not readable ELF: %s", explicit_shell);
    }

    if (!glibc_root || !*glibc_root) return NULL;

    char* bin = join_path2(glibc_root, "bin");
    if (!bin) return NULL;

    char* bash = join_path2(bin, "bash");
    char* sh = join_path2(bin, "sh");
    free(bin);

    char* shell = first_existing_elf_path(bash, sh);
    free(bash);
    free(sh);
    return shell;
}

static int should_skip_common(const char* target_path, const char* ld_linux) {
    if (!target_path || !*target_path) return 1;
    if (same_path_string(target_path, ld_linux)) return 1;
    if (is_android_linker_path(target_path)) return 1;

#if !EXEC_WRAP_ALLOW_ANDROID_SYSTEM_EXEC
    if (is_android_system_path(target_path)) return 1;
#endif

    return 0;
}

static int is_java_target(const char* target_path) {
    const char* base = base_name_const(target_path);
    return strcmp(base, "java") == 0 || string_ends_with(base, "java.exe");
}

static int argv_contains_java_tmpdir(char* const original_argv[]) {
    if (!original_argv) return 0;

    for (size_t i = 1; original_argv[i]; i++) {
        if (string_starts_with(original_argv[i], "-Djava.io.tmpdir=")) return 1;
    }

    return 0;
}

static char* make_java_tmpdir_arg(const char* tmpdir) {
    if (!tmpdir || !*tmpdir) return NULL;

    const char* prefix = "-Djava.io.tmpdir=";
    size_t prefix_len = strlen(prefix);
    size_t tmp_len = strlen(tmpdir);

    char* out = (char*) malloc(prefix_len + tmp_len + 1);
    if (!out) {
        errno = ENOMEM;
        return NULL;
    }

    memcpy(out, prefix, prefix_len);
    memcpy(out + prefix_len, tmpdir, tmp_len);
    out[prefix_len + tmp_len] = '\0';
    return out;
}

static const char* choose_java_tmpdir(char* const envp[]) {
    const char* tmp = env_get_from(envp, "TMPDIR");
    if (tmp && *tmp) return tmp;
    tmp = env_get_from(envp, "TMP");
    if (tmp && *tmp) return tmp;
    tmp = env_get_from(envp, "TEMP");
    if (tmp && *tmp) return tmp;
    return NULL;
}

static int build_wrapped_argv(
    const char* ld_linux,
    const char* loader_argv0,
    const char* library_path,
    const char* preload,
    const char* program_path,
    const char* script_path,
    char* const original_argv[],
    const char* java_tmpdir,
    owned_vec_t* out
) {
    memset(out, 0, sizeof(*out));

    size_t original_count = argv_count(original_argv);
    int has_preload = preload && *preload;
    int has_loader_argv0 = loader_argv0 && *loader_argv0;
    int inject_java_tmpdir = 0;

#if EXEC_WRAP_ENABLE_JAVA_TMPDIR_INJECTION
    inject_java_tmpdir = script_path == NULL &&
                         java_tmpdir && *java_tmpdir &&
                         is_java_target(program_path) &&
                         !argv_contains_java_tmpdir(original_argv);
#else
    (void) java_tmpdir;
#endif

    size_t total = 0;
    total += 1;                    /* ld-linux argv[0] */
    if (has_loader_argv0) total += 2; /* --argv0 <original argv[0]> */
    total += 2;                    /* --library-path <path> */
    if (has_preload) total += 2;    /* --preload <libs> */
    total += 1;                    /* program path */
    if (script_path) total += 1;    /* script path passed to shell */
    if (inject_java_tmpdir) total += 1;
    total += original_count > 0 ? original_count - 1 : 0;

    char** argv = (char**) calloc(total + 1, sizeof(char*));
    if (!argv) {
        errno = ENOMEM;
        return -1;
    }

    size_t index = 0;
    argv[index++] = duplicate_string(ld_linux);

    if (has_loader_argv0) {
        argv[index++] = duplicate_string("--argv0");
        argv[index++] = duplicate_string(loader_argv0);
    }

    argv[index++] = duplicate_string("--library-path");
    argv[index++] = duplicate_string(library_path);

    if (has_preload) {
        argv[index++] = duplicate_string("--preload");
        argv[index++] = duplicate_string(preload);
    }

    argv[index++] = duplicate_string(program_path);

    if (script_path) {
        argv[index++] = duplicate_string(script_path);
    }

    if (inject_java_tmpdir) {
        argv[index++] = make_java_tmpdir_arg(java_tmpdir);
        tracef("inject java.io.tmpdir for %s -> %s", program_path, java_tmpdir);
    }

    for (size_t i = 1; i < original_count; i++) {
        argv[index++] = duplicate_string(original_argv[i]);
    }

    argv[index] = NULL;

    for (size_t i = 0; i < index; i++) {
        if (!argv[i]) {
            for (size_t j = 0; j < index; j++) free(argv[j]);
            free(argv);
            errno = ENOMEM;
            return -1;
        }
    }

    out->items = argv;
    out->count = index;
    return 0;
}

static int prepare_wrap(
    const char* requested_path,
    char* const argv[],
    char* const envp[],
    int search_path,
    char** resolved_target_out,
    owned_vec_t* wrapped_argv_out,
    owned_vec_t* wrapped_env_out
) {
    memset(wrapped_argv_out, 0, sizeof(*wrapped_argv_out));
    memset(wrapped_env_out, 0, sizeof(*wrapped_env_out));
    *resolved_target_out = NULL;

#if !EXEC_WRAP_ENABLE
    (void) requested_path;
    (void) argv;
    (void) envp;
    (void) search_path;
    return 0;
#else
    if (wrapping_now) return 0;

    char* ld_linux = resolve_ld_linux_path();
    const char* library_path = env_get_from(envp, "LD_LIBRARY_PATH");
    const char* preload = env_get_from(envp, "LD_PRELOAD");
    const char* java_tmpdir = choose_java_tmpdir(envp);

    if (!ld_linux || !*ld_linux || !library_path || !*library_path) {
        tracef("skip %s: derived linker/LD_LIBRARY_PATH missing", requested_path ? requested_path : "(null)");
        free(ld_linux);
        return 0;
    }

    char* resolved = search_path ? resolve_from_path(requested_path, envp) : duplicate_string(requested_path);
    if (!resolved) {
        tracef("skip %s: unable to resolve target path", requested_path ? requested_path : "(null)");
        free(ld_linux);
        return 0;
    }

    char* replacement = resolve_glibc_bin_replacement(resolved, library_path);
    if (replacement) {
        free(resolved);
        resolved = replacement;
    }

    if (should_skip_common(resolved, ld_linux)) {
        tracef("skip %s -> %s: explicitly skipped", requested_path, resolved);
        free(resolved);
        free(ld_linux);
        return 0;
    }

    exec_target_kind_t kind = KIND_NONE;
    char* program_path = NULL;
    const char* script_path = NULL;

    if (is_probably_elf_file(resolved)) {
        kind = KIND_ELF;
        program_path = duplicate_string(resolved);
    }

#if EXEC_WRAP_ENABLE_SCRIPT_WRAP
    if (kind == KIND_NONE && is_shell_script_file(resolved)) {
        char* glibc_root = first_path_component(library_path);
        program_path = resolve_script_shell(envp, glibc_root);
        free(glibc_root);
        if (program_path) {
            kind = KIND_SCRIPT;
            script_path = resolved;
        }
    }
#endif

    if (kind == KIND_NONE || !program_path) {
        tracef("skip %s -> %s: non-ELF target delegated", requested_path, resolved);
        free(program_path);
        free(resolved);
        free(ld_linux);
        return 0;
    }

    const char* loader_argv0 = NULL;
    if (kind == KIND_ELF) {
        loader_argv0 = argv && argv[0] && *argv[0]
            ? argv[0]
            : program_path;
    }

    char* executable_identity = absolute_canonical_path(program_path);
    char* loader_identity = absolute_canonical_path(ld_linux);
    if (!executable_identity || !loader_identity) {
        free(executable_identity);
        free(loader_identity);
        free(program_path);
        free(resolved);
        free(ld_linux);
        errno = ENOMEM;
        return -1;
    }

    if (build_wrapped_argv(
            ld_linux,
            loader_argv0,
            library_path,
            preload,
            executable_identity,
            script_path,
            argv,
            java_tmpdir,
            wrapped_argv_out
        ) != 0) {
        free(executable_identity);
        free(loader_identity);
        free(program_path);
        free(resolved);
        free(ld_linux);
        return -1;
    }

    if (build_child_env(
            envp,
            library_path,
            preload,
            executable_identity,
            loader_identity,
            wrapped_env_out
        ) != 0) {
        owned_vec_free(wrapped_argv_out);
        free(executable_identity);
        free(loader_identity);
        free(program_path);
        free(resolved);
        free(ld_linux);
        return -1;
    }

    if (kind == KIND_SCRIPT) {
        tracef(
            "wrap script %s -> %s via shell %s and linker %s; identity=%s",
            requested_path,
            resolved,
            program_path,
            ld_linux,
            executable_identity
        );
    } else {
        tracef(
            "wrap ELF %s -> %s via %s; argv0=%s identity=%s",
            requested_path,
            resolved,
            ld_linux,
            loader_argv0,
            executable_identity
        );
    }

    free(executable_identity);
    free(loader_identity);
    free(program_path);
    free(ld_linux);
    *resolved_target_out = resolved;
    return 1;
#endif
}

static void cleanup_prepare(char* resolved_target, owned_vec_t* wrapped_argv, owned_vec_t* wrapped_env) {
    free(resolved_target);
    owned_vec_free(wrapped_argv);
    owned_vec_free(wrapped_env);
}

int execve(const char* pathname, char* const argv[], char* const envp[]) {
    execve_fn_t real_execve = REAL(execve, execve_fn_t);

    char* resolved = NULL;
    owned_vec_t wrapped_argv;
    owned_vec_t wrapped_env;

    int prep = prepare_wrap(pathname, argv, envp, 0, &resolved, &wrapped_argv, &wrapped_env);
    if (prep < 0) return -1;
    if (prep == 0) {
        int delegated = delegate_android_execve_if_needed(real_execve, pathname, argv, envp, 0);
        if (delegated != 0) return delegated;
        return real_execve(pathname, argv, envp);
    }

    const char* ld_linux = wrapped_argv.items[0];

    wrapping_now = 1;
    int rc = real_execve(ld_linux, wrapped_argv.items, wrapped_env.items);
    int saved_errno = errno;
    wrapping_now = 0;

    cleanup_prepare(resolved, &wrapped_argv, &wrapped_env);
    errno = saved_errno;
    return rc;
}

int execv(const char* pathname, char* const argv[]) {
    return execve(pathname, argv, environ);
}

int execvp(const char* file, char* const argv[]) {
    execve_fn_t real_execve = REAL(execve, execve_fn_t);
    execvp_fn_t real_execvp = REAL(execvp, execvp_fn_t);

    char* resolved = NULL;
    owned_vec_t wrapped_argv;
    owned_vec_t wrapped_env;

    int prep = prepare_wrap(file, argv, environ, !path_has_slash(file), &resolved, &wrapped_argv, &wrapped_env);
    if (prep < 0) return -1;
    if (prep == 0) {
        int delegated = delegate_android_execve_if_needed(real_execve, file, argv, environ, !path_has_slash(file));
        if (delegated != 0) return delegated;
        return real_execvp(file, argv);
    }

    const char* ld_linux = wrapped_argv.items[0];

    wrapping_now = 1;
    int rc = real_execve(ld_linux, wrapped_argv.items, wrapped_env.items);
    int saved_errno = errno;
    wrapping_now = 0;

    cleanup_prepare(resolved, &wrapped_argv, &wrapped_env);
    errno = saved_errno;
    return rc;
}

int execvpe(const char* file, char* const argv[], char* const envp[]) {
    execve_fn_t real_execve = REAL(execve, execve_fn_t);
    execvpe_fn_t real_execvpe = OPT_REAL(execvpe, execvpe_fn_t);

    char* resolved = NULL;
    owned_vec_t wrapped_argv;
    owned_vec_t wrapped_env;

    int prep = prepare_wrap(file, argv, envp, !path_has_slash(file), &resolved, &wrapped_argv, &wrapped_env);
    if (prep < 0) return -1;
    if (prep == 0) {
        int delegated = delegate_android_execve_if_needed(real_execve, file, argv, envp, !path_has_slash(file));
        if (delegated != 0) return delegated;

        if (real_execvpe) return real_execvpe(file, argv, envp);
        if (path_has_slash(file)) return real_execve(file, argv, envp);

        char* fallback = resolve_from_path(file, envp);
        if (!fallback) {
            errno = ENOENT;
            return -1;
        }
        int rc = real_execve(fallback, argv, envp);
        int saved_errno = errno;
        free(fallback);
        errno = saved_errno;
        return rc;
    }

    const char* ld_linux = wrapped_argv.items[0];

    wrapping_now = 1;
    int rc = real_execve(ld_linux, wrapped_argv.items, wrapped_env.items);
    int saved_errno = errno;
    wrapping_now = 0;

    cleanup_prepare(resolved, &wrapped_argv, &wrapped_env);
    errno = saved_errno;
    return rc;
}

static char** collect_exec_args_from_va(const char* arg0, va_list ap, char*** envp_out, int has_env_after_null) {
    size_t capacity = 8;
    size_t count = 0;
    char** argv = (char**) calloc(capacity, sizeof(char*));
    if (!argv) {
        errno = ENOMEM;
        return NULL;
    }

    if (arg0) argv[count++] = (char*) arg0;

    while (1) {
        char* arg = va_arg(ap, char*);
        if (!arg) break;

        if (count + 2 > capacity) {
            capacity *= 2;
            char** grown = (char**) realloc(argv, capacity * sizeof(char*));
            if (!grown) {
                free(argv);
                errno = ENOMEM;
                return NULL;
            }
            argv = grown;
        }

        argv[count++] = arg;
    }

    argv[count] = NULL;

    if (has_env_after_null && envp_out) {
        *envp_out = va_arg(ap, char**);
    }

    return argv;
}

int execl(const char* pathname, const char* arg, ...) {
    va_list ap;
    va_start(ap, arg);
    char** argv = collect_exec_args_from_va(arg, ap, NULL, 0);
    va_end(ap);
    if (!argv) return -1;

    int rc = execve(pathname, argv, environ);
    int saved_errno = errno;
    free(argv);
    errno = saved_errno;
    return rc;
}

int execlp(const char* file, const char* arg, ...) {
    va_list ap;
    va_start(ap, arg);
    char** argv = collect_exec_args_from_va(arg, ap, NULL, 0);
    va_end(ap);
    if (!argv) return -1;

    int rc = execvp(file, argv);
    int saved_errno = errno;
    free(argv);
    errno = saved_errno;
    return rc;
}

int execle(const char* pathname, const char* arg, ...) {
    va_list ap;
    va_start(ap, arg);
    char** envp = NULL;
    char** argv = collect_exec_args_from_va(arg, ap, &envp, 1);
    va_end(ap);
    if (!argv) return -1;

    int rc = execve(pathname, argv, envp);
    int saved_errno = errno;
    free(argv);
    errno = saved_errno;
    return rc;
}

int fexecve(int fd, char* const argv[], char* const envp[]) {
    fexecve_fn_t real_fexecve = OPT_REAL(fexecve, fexecve_fn_t);
    if (real_fexecve) return real_fexecve(fd, argv, envp);

    errno = ENOSYS;
    return -1;
}

int posix_spawn(
    pid_t* pid,
    const char* path,
    const posix_spawn_file_actions_t* file_actions,
    const posix_spawnattr_t* attrp,
    char* const argv[],
    char* const envp[]
) {
    posix_spawn_fn_t real_posix_spawn = REAL(posix_spawn, posix_spawn_fn_t);

    char* resolved = NULL;
    owned_vec_t wrapped_argv;
    owned_vec_t wrapped_env;

    int prep = prepare_wrap(path, argv, envp, 0, &resolved, &wrapped_argv, &wrapped_env);
    if (prep < 0) return errno ? errno : ENOMEM;
    if (prep == 0) {
        int delegated = delegate_android_spawn_if_needed(real_posix_spawn, pid, path, file_actions, attrp, argv, envp, 0);
        if (delegated != 0) return delegated == 1 ? 0 : delegated;
        return real_posix_spawn(pid, path, file_actions, attrp, argv, envp);
    }

    const char* ld_linux = wrapped_argv.items[0];

    wrapping_now = 1;
    int rc = real_posix_spawn(pid, ld_linux, file_actions, attrp, wrapped_argv.items, wrapped_env.items);
    wrapping_now = 0;

    cleanup_prepare(resolved, &wrapped_argv, &wrapped_env);
    return rc;
}

int posix_spawnp(
    pid_t* pid,
    const char* file,
    const posix_spawn_file_actions_t* file_actions,
    const posix_spawnattr_t* attrp,
    char* const argv[],
    char* const envp[]
) {
    posix_spawn_fn_t real_posix_spawn = REAL(posix_spawn, posix_spawn_fn_t);
    posix_spawn_fn_t real_posix_spawnp = REAL(posix_spawnp, posix_spawn_fn_t);

    char* resolved = NULL;
    owned_vec_t wrapped_argv;
    owned_vec_t wrapped_env;

    int prep = prepare_wrap(file, argv, envp, !path_has_slash(file), &resolved, &wrapped_argv, &wrapped_env);
    if (prep < 0) return errno ? errno : ENOMEM;
    if (prep == 0) {
        int delegated = delegate_android_spawn_if_needed(real_posix_spawn, pid, file, file_actions, attrp, argv, envp, !path_has_slash(file));
        if (delegated != 0) return delegated == 1 ? 0 : delegated;
        return real_posix_spawnp(pid, file, file_actions, attrp, argv, envp);
    }

    const char* ld_linux = wrapped_argv.items[0];

    wrapping_now = 1;
    int rc = real_posix_spawn(pid, ld_linux, file_actions, attrp, wrapped_argv.items, wrapped_env.items);
    wrapping_now = 0;

    cleanup_prepare(resolved, &wrapped_argv, &wrapped_env);
    return rc;
}
