#define _GNU_SOURCE
#define _LARGEFILE64_SOURCE

#include "exec_wrap.h"

#include <dlfcn.h>
#include <dirent.h>
#include <elf.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <stddef.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/auxv.h>
#include <sys/inotify.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/xattr.h>
#include <sys/stat.h>
#include <sys/statfs.h>
#include <sys/statvfs.h>
#include <sys/syscall.h>
#include <sys/time.h>
#include <sys/types.h>
#include <time.h>
#include <unistd.h>
#include <utime.h>

#ifndef PATH_MAX
#define PATH_MAX 4096
#endif

#define REDIR_BUF_SIZE PATH_MAX
#define ARRAY_LEN(a) (sizeof(a) / sizeof((a)[0]))

#define TERMUX_FILES_PREFIX   "/data/data/com.termux/files"
#define TERMUX_GLIBC_PREFIX   "/data/data/com.termux/files/usr/glibc"
#define APP_FILES_DIR_DEFAULT "/data/data/org.cosmicide/files/glibc"

/* ------------------------------------------------------------------------- */
/* RTLD_NEXT symbol lookup                                                    */
/* ------------------------------------------------------------------------- */

static void* lookup_next_symbol(const char* name) {
    dlerror();
    return dlsym(RTLD_NEXT, name);
}

static void require_symbol_or_abort(const char* name, void* symbol) {
    if (symbol != NULL) return;
    fprintf(stderr, "path_redirect: failed to resolve symbol: %s\n", name);
    abort();
}

#define DECLARE_REAL_SYMBOL(sym)                                      \
    static void* real_##sym##_ptr = NULL;                             \
    static pthread_once_t real_##sym##_once = PTHREAD_ONCE_INIT;      \
    static void init_##sym(void) {                                    \
        real_##sym##_ptr = lookup_next_symbol(#sym);                  \
    }                                                                 \
    static void* get_##sym(int required) {                            \
        pthread_once(&real_##sym##_once, init_##sym);                 \
        if (required) require_symbol_or_abort(#sym, real_##sym##_ptr);\
        return real_##sym##_ptr;                                      \
    }

#define REAL(sym, type) ((type)get_##sym(1))
#define OPT_REAL(sym, type) ((type)get_##sym(0))

DECLARE_REAL_SYMBOL(access)
DECLARE_REAL_SYMBOL(bind)
DECLARE_REAL_SYMBOL(connect)
DECLARE_REAL_SYMBOL(canonicalize_file_name)
DECLARE_REAL_SYMBOL(chdir)
DECLARE_REAL_SYMBOL(chmod)
DECLARE_REAL_SYMBOL(chown)
DECLARE_REAL_SYMBOL(creat)
DECLARE_REAL_SYMBOL(creat64)
DECLARE_REAL_SYMBOL(faccessat)
DECLARE_REAL_SYMBOL(faccessat2)
DECLARE_REAL_SYMBOL(fchmodat)
DECLARE_REAL_SYMBOL(fchownat)
DECLARE_REAL_SYMBOL(fopen)
DECLARE_REAL_SYMBOL(fopen64)
DECLARE_REAL_SYMBOL(freopen)
DECLARE_REAL_SYMBOL(freopen64)
DECLARE_REAL_SYMBOL(fstat)
DECLARE_REAL_SYMBOL(fstat64)
DECLARE_REAL_SYMBOL(fstatat)
DECLARE_REAL_SYMBOL(fstatat64)
DECLARE_REAL_SYMBOL(futimesat)
DECLARE_REAL_SYMBOL(getauxval)
DECLARE_REAL_SYMBOL(getxattr)
DECLARE_REAL_SYMBOL(lgetxattr)
DECLARE_REAL_SYMBOL(listxattr)
DECLARE_REAL_SYMBOL(llistxattr)
DECLARE_REAL_SYMBOL(inotify_add_watch)
DECLARE_REAL_SYMBOL(lchown)
DECLARE_REAL_SYMBOL(link)
DECLARE_REAL_SYMBOL(linkat)
DECLARE_REAL_SYMBOL(lstat)
DECLARE_REAL_SYMBOL(lstat64)
DECLARE_REAL_SYMBOL(lutimes)
DECLARE_REAL_SYMBOL(mkdir)
DECLARE_REAL_SYMBOL(mkdirat)
DECLARE_REAL_SYMBOL(mkfifo)
DECLARE_REAL_SYMBOL(mkfifoat)
DECLARE_REAL_SYMBOL(mknod)
DECLARE_REAL_SYMBOL(mknodat)
DECLARE_REAL_SYMBOL(open)
DECLARE_REAL_SYMBOL(open64)
DECLARE_REAL_SYMBOL(openat)
DECLARE_REAL_SYMBOL(openat64)
DECLARE_REAL_SYMBOL(opendir)
DECLARE_REAL_SYMBOL(readlink)
DECLARE_REAL_SYMBOL(readlinkat)
DECLARE_REAL_SYMBOL(realpath)
DECLARE_REAL_SYMBOL(remove)
DECLARE_REAL_SYMBOL(removexattr)
DECLARE_REAL_SYMBOL(lremovexattr)
DECLARE_REAL_SYMBOL(rename)
DECLARE_REAL_SYMBOL(renameat)
DECLARE_REAL_SYMBOL(renameat2)
DECLARE_REAL_SYMBOL(rmdir)
DECLARE_REAL_SYMBOL(scandir)
DECLARE_REAL_SYMBOL(scandirat)
DECLARE_REAL_SYMBOL(sendmsg)
DECLARE_REAL_SYMBOL(sendto)
DECLARE_REAL_SYMBOL(setxattr)
DECLARE_REAL_SYMBOL(lsetxattr)
DECLARE_REAL_SYMBOL(stat)
DECLARE_REAL_SYMBOL(stat64)
DECLARE_REAL_SYMBOL(statfs)
DECLARE_REAL_SYMBOL(statfs64)
DECLARE_REAL_SYMBOL(statvfs)
DECLARE_REAL_SYMBOL(statvfs64)
DECLARE_REAL_SYMBOL(statx)
DECLARE_REAL_SYMBOL(syscall)
DECLARE_REAL_SYMBOL(symlink)
DECLARE_REAL_SYMBOL(symlinkat)
DECLARE_REAL_SYMBOL(truncate)
DECLARE_REAL_SYMBOL(truncate64)
DECLARE_REAL_SYMBOL(unlink)
DECLARE_REAL_SYMBOL(unlinkat)
DECLARE_REAL_SYMBOL(utime)
DECLARE_REAL_SYMBOL(utimensat)
DECLARE_REAL_SYMBOL(utimes)
DECLARE_REAL_SYMBOL(__fxstat)
DECLARE_REAL_SYMBOL(__fxstat64)
DECLARE_REAL_SYMBOL(__fxstatat)
DECLARE_REAL_SYMBOL(__fxstatat64)
DECLARE_REAL_SYMBOL(__lxstat)
DECLARE_REAL_SYMBOL(__lxstat64)
DECLARE_REAL_SYMBOL(__rmdir)
DECLARE_REAL_SYMBOL(__unlink)
DECLARE_REAL_SYMBOL(__unlinkat)
DECLARE_REAL_SYMBOL(__xstat)
DECLARE_REAL_SYMBOL(__xstat64)

/*
 * Always use the next libc syscall implementation from internal fallbacks.
 * Calling syscall() here would re-enter this interposer, redirect paths twice,
 * and (for calls with fewer than six arguments) used to trigger varargs UB.
 */
static long real_syscall_call(
    long number,
    long argument1,
    long argument2,
    long argument3,
    long argument4,
    long argument5,
    long argument6
) {
    long (*fn)(long, ...) = REAL(syscall, long (*)(long, ...));
    return fn(
        number,
        argument1,
        argument2,
        argument3,
        argument4,
        argument5,
        argument6
    );
}

/* ------------------------------------------------------------------------- */
/* Debugging                                                                  */
/* ------------------------------------------------------------------------- */

static int path_redirect_debug_enabled(void) {
    const char* value = getenv("PATH_REDIRECT_DEBUG");
    return value != NULL && value[0] != '\0' && strcmp(value, "0") != 0;
}

static void debug_redirect(const char* operation, const char* from, const char* to) {
    if (!path_redirect_debug_enabled()) return;
    if (from == NULL || to == NULL || strcmp(from, to) == 0) return;
    fprintf(stderr, "path_redirect: %s %s -> %s\n", operation, from, to);
}

/* Kept for compatibility with the original implementation and its log labels. */
static void debug_path_operation(const char* operation, const char* from, const char* to) {
    debug_redirect(operation, from, to);
}

/* ------------------------------------------------------------------------- */
/* Executable identity virtualization                                         */
/* ------------------------------------------------------------------------- */

static int is_current_process_exe_path(const char* path) {
    if (path == NULL || path[0] == '\0') return 0;

    if (strcmp(path, "/proc/self/exe") == 0 ||
        strcmp(path, "/proc/thread-self/exe") == 0) {
        return 1;
    }

    if (strncmp(path, "/proc/", 6) != 0) return 0;

    const char* pid_start = path + 6;
    char* pid_end = NULL;
    errno = 0;
    long pid = strtol(pid_start, &pid_end, 10);
    if (errno != 0 || pid_end == pid_start || pid <= 0) return 0;
    if (strcmp(pid_end, "/exe") != 0) return 0;
    return pid == (long)getpid();
}

static ssize_t copy_readlink_result(const char* target, char* out, size_t out_size) {
    if (target == NULL) {
        errno = ENOENT;
        return -1;
    }
    if (out == NULL) {
        errno = EFAULT;
        return -1;
    }
    if (out_size == 0) {
        errno = EINVAL;
        return -1;
    }

    size_t target_len = strlen(target);
    size_t copy_len = target_len < out_size ? target_len : out_size;
    memcpy(out, target, copy_len);
    return (ssize_t)copy_len;
}

static pthread_once_t expected_loader_once = PTHREAD_ONCE_INIT;
static char expected_loader_path[REDIR_BUF_SIZE];

static void init_expected_loader_path(void) {
    const char* configured_loader = getenv(EXEC_WRAP_LOADER_ENV);
    if (configured_loader != NULL && configured_loader[0] == '/') {
        snprintf(expected_loader_path, sizeof(expected_loader_path), "%s", configured_loader);
        return;
    }

    Dl_info info;
    memset(&info, 0, sizeof(info));
    if (dladdr((void*)&init_expected_loader_path, &info) == 0 ||
        info.dli_fname == NULL || info.dli_fname[0] == '\0') {
        return;
    }

    const char* slash = strrchr(info.dli_fname, '/');
    if (slash == NULL) return;

    size_t directory_len = (size_t)(slash - info.dli_fname);
    const char loader_name[] = "/libld_linux.so";
    if (directory_len + sizeof(loader_name) > sizeof(expected_loader_path)) return;

    memcpy(expected_loader_path, info.dli_fname, directory_len);
    memcpy(expected_loader_path + directory_len, loader_name, sizeof(loader_name));
}

static const char* active_executable_identity(void) {
    const char* executable = getenv(EXEC_WRAP_EXECUTABLE_ENV);
    if (executable == NULL || executable[0] != '/') return NULL;

    pthread_once(&expected_loader_once, init_expected_loader_path);
    if (expected_loader_path[0] == '\0') return NULL;

    ssize_t (*real_readlink_fn)(const char*, char*, size_t) =
        REAL(readlink, ssize_t (*)(const char*, char*, size_t));

    char actual[REDIR_BUF_SIZE];
    ssize_t count = real_readlink_fn("/proc/self/exe", actual, sizeof(actual) - 1);
    if (count <= 0 || (size_t)count >= sizeof(actual)) return NULL;
    actual[count] = '\0';

    if (strcmp(actual, expected_loader_path) != 0) return NULL;
    return executable;
}

static const char* executable_identity_for_path(const char* path) {
    return is_current_process_exe_path(path) ? active_executable_identity() : NULL;
}

/* ------------------------------------------------------------------------- */
/* Path redirection                                                           */
/* ------------------------------------------------------------------------- */

typedef struct {
    const char* suffix;
    const char* environment;
} suffix_redirect_t;

static const suffix_redirect_t suffix_redirects[] = {
    { "/etc/resolv.conf",   "RESOLV_CONF_PATH" },
    { "/etc/hosts",         "HOSTS_PATH" },
    { "/etc/nsswitch.conf", "NSSWITCH_CONF_PATH" },
    { "/etc/gai.conf",      "GAI_CONF_PATH" },
};

static const char* app_files_dir(void) {
    const char* value = getenv("APP_FILES_DIR");
    return value != NULL && value[0] != '\0' ? value : APP_FILES_DIR_DEFAULT;
}

static int path_starts_with_component(const char* path, const char* prefix) {
    if (path == NULL || prefix == NULL) return 0;
    size_t length = strlen(prefix);
    if (strncmp(path, prefix, length) != 0) return 0;
    return path[length] == '\0' || path[length] == '/';
}

static int path_ends_with(const char* path, const char* suffix) {
    if (path == NULL || suffix == NULL) return 0;
    size_t path_length = strlen(path);
    size_t suffix_length = strlen(suffix);
    return path_length >= suffix_length &&
           strcmp(path + path_length - suffix_length, suffix) == 0;
}

static const char* redirect_prefix_path(
    const char* path,
    const char* source_prefix,
    const char* destination_prefix,
    char* buffer,
    size_t buffer_size
) {
    const char* remainder = path + strlen(source_prefix);
    int result = snprintf(buffer, buffer_size, "%s%s", destination_prefix, remainder);
    if (result < 0 || (size_t)result >= buffer_size) {
        errno = ENAMETOOLONG;
        return NULL;
    }
    return buffer;
}

static const char* redirect_virtual_root(
    const char* path,
    const char* virtual_prefix,
    const char* physical_suffix,
    char* buffer,
    size_t buffer_size
) {
    char physical_prefix[REDIR_BUF_SIZE];
    int result = snprintf(
        physical_prefix,
        sizeof(physical_prefix),
        "%s%s",
        app_files_dir(),
        physical_suffix
    );
    if (result < 0 || (size_t)result >= sizeof(physical_prefix)) {
        errno = ENAMETOOLONG;
        return NULL;
    }
    return redirect_prefix_path(path, virtual_prefix, physical_prefix, buffer, buffer_size);
}

static const char* redirect_path(const char* path, char* buffer, size_t buffer_size) {
    if (path == NULL || path[0] == '\0' || path[0] != '/') return path;

    /* Never redirect an already-physical app path a second time. */
    if (path_starts_with_component(path, app_files_dir())) return path;

    /* Special config files override the generic /etc mapping. */
    for (size_t i = 0; i < ARRAY_LEN(suffix_redirects); ++i) {
        if (!path_ends_with(path, suffix_redirects[i].suffix)) continue;
        const char* replacement = getenv(suffix_redirects[i].environment);
        if (replacement == NULL || replacement[0] == '\0') break;
        int result = snprintf(buffer, buffer_size, "%s", replacement);
        if (result < 0 || (size_t)result >= buffer_size) {
            errno = ENAMETOOLONG;
            return NULL;
        }
        debug_redirect("config", path, buffer);
        return buffer;
    }

    if (path_starts_with_component(path, TERMUX_GLIBC_PREFIX)) {
        const char* redirected = redirect_virtual_root(
            path,
            TERMUX_GLIBC_PREFIX,
            "/usr",
            buffer,
            buffer_size
        );
        debug_redirect("termux-glibc", path, redirected);
        return redirected;
    }

    if (path_starts_with_component(path, TERMUX_FILES_PREFIX)) {
        const char* redirected = redirect_prefix_path(
            path,
            TERMUX_FILES_PREFIX,
            app_files_dir(),
            buffer,
            buffer_size
        );
        debug_redirect("termux", path, redirected);
        return redirected;
    }

    if (path_starts_with_component(path, "/etc")) {
        const char* redirected = redirect_virtual_root(path, "/etc", "/usr/etc", buffer, buffer_size);
        debug_redirect("etc", path, redirected);
        return redirected;
    }

    /* Avoid absolute /var/run -> /run and /var/lock -> /run/lock symlinks. */
    if (path_starts_with_component(path, "/var/run")) {
        const char* redirected = redirect_virtual_root(path, "/var/run", "/usr/run", buffer, buffer_size);
        debug_redirect("var-run", path, redirected);
        return redirected;
    }

    if (path_starts_with_component(path, "/var/lock")) {
        const char* redirected = redirect_virtual_root(path, "/var/lock", "/usr/run/lock", buffer, buffer_size);
        debug_redirect("var-lock", path, redirected);
        return redirected;
    }

    if (path_starts_with_component(path, "/var")) {
        const char* redirected = redirect_virtual_root(path, "/var", "/usr/var", buffer, buffer_size);
        debug_redirect("var", path, redirected);
        return redirected;
    }

    if (path_starts_with_component(path, "/run")) {
        const char* redirected = redirect_virtual_root(path, "/run", "/usr/run", buffer, buffer_size);
        debug_redirect("run", path, redirected);
        return redirected;
    }

    if (path_starts_with_component(path, "/usr")) {
        const char* redirected = redirect_virtual_root(path, "/usr", "/usr", buffer, buffer_size);
        debug_redirect("usr", path, redirected);
        return redirected;
    }

    if (path_starts_with_component(path, "/bin")) {
        const char* redirected = redirect_virtual_root(path, "/bin", "/usr/bin", buffer, buffer_size);
        debug_redirect("bin", path, redirected);
        return redirected;
    }

    if (path_starts_with_component(path, "/sbin")) {
        const char* redirected = redirect_virtual_root(path, "/sbin", "/usr/sbin", buffer, buffer_size);
        debug_redirect("sbin", path, redirected);
        return redirected;
    }

    if (path_starts_with_component(path, "/lib64")) {
        const char* redirected = redirect_virtual_root(path, "/lib64", "/lib64", buffer, buffer_size);
        debug_redirect("lib64", path, redirected);
        return redirected;
    }

    if (path_starts_with_component(path, "/lib")) {
        const char* redirected = redirect_virtual_root(path, "/lib", "/lib", buffer, buffer_size);
        debug_redirect("lib", path, redirected);
        return redirected;
    }

    const char* tmpdir = getenv("TMPDIR");
    if (tmpdir != NULL && tmpdir[0] != '\0' && path_starts_with_component(path, "/tmp")) {
        const char* redirected = redirect_prefix_path(path, "/tmp", tmpdir, buffer, buffer_size);
        debug_redirect("tmp", path, redirected);
        return redirected;
    }

    return path;
}

static int read_dirfd_path(int dirfd, char* buffer, size_t buffer_size) {
    if (dirfd == AT_FDCWD || buffer == NULL || buffer_size < 2) return 0;

    char proc_path[64];
    int count = snprintf(proc_path, sizeof(proc_path), "/proc/self/fd/%d", dirfd);
    if (count < 0 || (size_t)count >= sizeof(proc_path)) return 0;

    ssize_t (*real_readlink_fn)(const char*, char*, size_t) =
        REAL(readlink, ssize_t (*)(const char*, char*, size_t));
    ssize_t length = real_readlink_fn(proc_path, buffer, buffer_size - 1);
    if (length <= 0 || (size_t)length >= buffer_size) return 0;
    buffer[length] = '\0';

    static const char deleted_suffix[] = " (deleted)";
    size_t path_length = (size_t)length;
    size_t suffix_length = sizeof(deleted_suffix) - 1;
    if (path_length >= suffix_length &&
        strcmp(buffer + path_length - suffix_length, deleted_suffix) == 0) {
        buffer[path_length - suffix_length] = '\0';
    }
    return buffer[0] == '/';
}

static int dirfd_path_can_contain_virtual_root(const char* path) {
    if (path == NULL || path[0] != '/') return 0;
    if (strcmp(path, "/") == 0) return 1;
    if (path_starts_with_component(path, "/etc")) return 1;
    if (path_starts_with_component(path, "/var")) return 1;
    if (path_starts_with_component(path, "/run")) return 1;
    if (path_starts_with_component(path, "/usr")) return 1;
    if (path_starts_with_component(path, "/bin")) return 1;
    if (path_starts_with_component(path, "/sbin")) return 1;
    if (path_starts_with_component(path, "/lib")) return 1;
    if (path_starts_with_component(path, "/lib64")) return 1;
    if (path_starts_with_component(path, "/tmp")) return 1;
    if (path_starts_with_component(path, TERMUX_FILES_PREFIX)) return 1;
    return 0;
}

static const char* redirect_at_path(
    int dirfd,
    const char* path,
    char* buffer,
    size_t buffer_size
) {
    if (path == NULL || path[0] == '\0') return path;
    if (path[0] == '/') return redirect_path(path, buffer, buffer_size);
    if (dirfd == AT_FDCWD) return path;

    char directory[REDIR_BUF_SIZE];
    if (!read_dirfd_path(dirfd, directory, sizeof(directory))) return path;

    /* A relative path below an already redirected physical directory is fine. */
    if (path_starts_with_component(directory, app_files_dir())) return path;
    if (!dirfd_path_can_contain_virtual_root(directory)) return path;

    char absolute[REDIR_BUF_SIZE];
    int result;
    if (strcmp(directory, "/") == 0) {
        result = snprintf(absolute, sizeof(absolute), "/%s", path);
    } else {
        result = snprintf(absolute, sizeof(absolute), "%s/%s", directory, path);
    }
    if (result < 0 || (size_t)result >= sizeof(absolute)) {
        errno = ENAMETOOLONG;
        return NULL;
    }

    const char* redirected = redirect_path(absolute, buffer, buffer_size);
    debug_redirect("dirfd-relative", absolute, redirected);
    return redirected;
}

static const struct sockaddr* redirect_unix_socket_address(
    const struct sockaddr* address,
    socklen_t address_length,
    struct sockaddr_un* redirected_address,
    socklen_t* redirected_length,
    char* path_buffer,
    size_t path_buffer_size
) {
    if (redirected_length != NULL) *redirected_length = address_length;
    if (address == NULL || address->sa_family != AF_UNIX) return address;

    const size_t path_offset = offsetof(struct sockaddr_un, sun_path);
    if ((size_t)address_length <= path_offset) return address;

    const struct sockaddr_un* unix_address = (const struct sockaddr_un*)address;
    size_t available = (size_t)address_length - path_offset;
    if (available > sizeof(unix_address->sun_path)) available = sizeof(unix_address->sun_path);
    if (available == 0 || unix_address->sun_path[0] == '\0') {
        /* Linux abstract-namespace socket: it is not a filesystem path. */
        return address;
    }

    size_t original_length = strnlen(unix_address->sun_path, available);
    if (original_length == available && available == sizeof(unix_address->sun_path)) {
        errno = ENAMETOOLONG;
        return NULL;
    }

    char original_path[sizeof(unix_address->sun_path) + 1];
    memcpy(original_path, unix_address->sun_path, original_length);
    original_path[original_length] = '\0';

    const char* redirected_path = redirect_path(
        original_path,
        path_buffer,
        path_buffer_size
    );
    if (redirected_path == NULL) return NULL;
    if (redirected_path == original_path || strcmp(redirected_path, original_path) == 0) {
        return address;
    }

    size_t redirected_path_length = strlen(redirected_path);
    if (redirected_path_length >= sizeof(redirected_address->sun_path)) {
        errno = ENAMETOOLONG;
        return NULL;
    }

    memset(redirected_address, 0, sizeof(*redirected_address));
    redirected_address->sun_family = AF_UNIX;
    memcpy(redirected_address->sun_path, redirected_path, redirected_path_length + 1);
    if (redirected_length != NULL) {
        *redirected_length = (socklen_t)(path_offset + redirected_path_length + 1);
    }
    debug_redirect("unix-socket", original_path, redirected_path);
    return (const struct sockaddr*)redirected_address;
}

/* ------------------------------------------------------------------------- */
/* Unix-domain socket path wrappers                                           */
/* ------------------------------------------------------------------------- */

int connect(int socket_fd, const struct sockaddr* address, socklen_t address_length) {
    int (*fn)(int, const struct sockaddr*, socklen_t) =
        REAL(connect, int (*)(int, const struct sockaddr*, socklen_t));
    struct sockaddr_un redirected_address;
    socklen_t redirected_length = address_length;
    char path_buffer[REDIR_BUF_SIZE];
    const struct sockaddr* redirected = redirect_unix_socket_address(
        address,
        address_length,
        &redirected_address,
        &redirected_length,
        path_buffer,
        sizeof(path_buffer)
    );
    if (redirected == NULL) return -1;
    return fn(socket_fd, redirected, redirected_length);
}

int bind(int socket_fd, const struct sockaddr* address, socklen_t address_length) {
    int (*fn)(int, const struct sockaddr*, socklen_t) =
        REAL(bind, int (*)(int, const struct sockaddr*, socklen_t));
    struct sockaddr_un redirected_address;
    socklen_t redirected_length = address_length;
    char path_buffer[REDIR_BUF_SIZE];
    const struct sockaddr* redirected = redirect_unix_socket_address(
        address,
        address_length,
        &redirected_address,
        &redirected_length,
        path_buffer,
        sizeof(path_buffer)
    );
    if (redirected == NULL) return -1;
    return fn(socket_fd, redirected, redirected_length);
}

ssize_t sendto(
    int socket_fd,
    const void* data,
    size_t data_length,
    int flags,
    const struct sockaddr* destination,
    socklen_t destination_length
) {
    ssize_t (*fn)(int, const void*, size_t, int, const struct sockaddr*, socklen_t) =
        REAL(sendto, ssize_t (*)(int, const void*, size_t, int, const struct sockaddr*, socklen_t));
    struct sockaddr_un redirected_address;
    socklen_t redirected_length = destination_length;
    char path_buffer[REDIR_BUF_SIZE];
    const struct sockaddr* redirected = redirect_unix_socket_address(
        destination,
        destination_length,
        &redirected_address,
        &redirected_length,
        path_buffer,
        sizeof(path_buffer)
    );
    if (redirected == NULL) return -1;
    return fn(socket_fd, data, data_length, flags, redirected, redirected_length);
}

ssize_t sendmsg(int socket_fd, const struct msghdr* message, int flags) {
    ssize_t (*fn)(int, const struct msghdr*, int) =
        REAL(sendmsg, ssize_t (*)(int, const struct msghdr*, int));
    if (message == NULL || message->msg_name == NULL) return fn(socket_fd, message, flags);

    struct sockaddr_un redirected_address;
    socklen_t redirected_length = (socklen_t)message->msg_namelen;
    char path_buffer[REDIR_BUF_SIZE];
    const struct sockaddr* redirected = redirect_unix_socket_address(
        (const struct sockaddr*)message->msg_name,
        (socklen_t)message->msg_namelen,
        &redirected_address,
        &redirected_length,
        path_buffer,
        sizeof(path_buffer)
    );
    if (redirected == NULL) return -1;
    if (redirected == (const struct sockaddr*)message->msg_name) {
        return fn(socket_fd, message, flags);
    }

    struct msghdr redirected_message = *message;
    redirected_message.msg_name = (void*)redirected;
    redirected_message.msg_namelen = redirected_length;
    return fn(socket_fd, &redirected_message, flags);
}

/* ------------------------------------------------------------------------- */
/* HotSpot hsperfdata compatibility                                           */
/* ------------------------------------------------------------------------- */

static int is_perf_path(const char* path) {
    return path != NULL && strstr(path, "hsperfdata_") != NULL;
}

static int is_perf_path_either(const char* original, const char* redirected) {
    return is_perf_path(original) || is_perf_path(redirected);
}

static int fd_points_to_perf_data(int fd) {
    char proc_path[64];
    char target[REDIR_BUF_SIZE];
    snprintf(proc_path, sizeof(proc_path), "/proc/self/fd/%d", fd);

    ssize_t (*real_readlink_fn)(const char*, char*, size_t) =
        REAL(readlink, ssize_t (*)(const char*, char*, size_t));
    ssize_t length = real_readlink_fn(proc_path, target, sizeof(target) - 1);
    if (length <= 0 || (size_t)length >= sizeof(target)) return 0;
    target[length] = '\0';
    return is_perf_path(target);
}

static void spoof_stat_if_perf_data(struct stat* st) {
    if (st == NULL) return;
    st->st_uid = geteuid();
    st->st_mode &= ~(S_IWGRP | S_IWOTH);
    if (S_ISREG(st->st_mode)) st->st_nlink = 1;
}

#if defined(__USE_LARGEFILE64) || defined(_LARGEFILE64_SOURCE)
static void spoof_stat64_if_perf_data(struct stat64* st) {
    if (st == NULL) return;
    st->st_uid = geteuid();
    st->st_mode &= ~(S_IWGRP | S_IWOTH);
    if (S_ISREG(st->st_mode)) st->st_nlink = 1;
}
#endif

static void spoof_statx_if_perf_data(struct statx* st) {
    if (st == NULL) return;
    st->stx_uid = geteuid();
    st->stx_mode &= (uint16_t)~(S_IWGRP | S_IWOTH);
    if ((st->stx_mode & S_IFMT) == S_IFREG) st->stx_nlink = 1;
}

/* ------------------------------------------------------------------------- */
/* General wrappers                                                           */
/* ------------------------------------------------------------------------- */

int access(const char* pathname, int mode) {
    int (*fn)(const char*, int) = REAL(access, int (*)(const char*, int));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, mode);
}

int faccessat(int dirfd, const char* pathname, int mode, int flags) {
    int (*fn)(int, const char*, int, int) =
        REAL(faccessat, int (*)(int, const char*, int, int));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(dirfd, path, mode, flags);
}

int faccessat2(int dirfd, const char* pathname, int mode, int flags) {
    int caller_errno = errno;
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;

    int result = -1;
    int (*fn)(int, const char*, int, int) =
        OPT_REAL(faccessat2, int (*)(int, const char*, int, int));

    if (fn != NULL) {
        result = fn(dirfd, path, mode, flags);
    } else {
#ifdef SYS_faccessat2
        result = (int)real_syscall_call(
            SYS_faccessat2,
            dirfd,
            (long)path,
            mode,
            flags,
            0,
            0
        );
#else
        errno = ENOSYS;
#endif
    }

    if (result != -1 || errno != ENOSYS) return result;

    /* libc's faccessat wrapper can emulate flags on older kernels. */
    int (*fallback)(int, const char*, int, int) =
        REAL(faccessat, int (*)(int, const char*, int, int));
    result = fallback(dirfd, path, mode, flags);
    if (result == 0) errno = caller_errno;
    return result;
}

int chdir(const char* pathname) {
    int (*fn)(const char*) = REAL(chdir, int (*)(const char*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path);
}

ssize_t readlink(const char* pathname, char* out, size_t out_size) {
    const char* identity = executable_identity_for_path(pathname);
    if (identity != NULL) return copy_readlink_result(identity, out, out_size);

    ssize_t (*fn)(const char*, char*, size_t) =
        REAL(readlink, ssize_t (*)(const char*, char*, size_t));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, out, out_size);
}

ssize_t readlinkat(int dirfd, const char* pathname, char* out, size_t out_size) {
    if (pathname[0] == '/') {
        const char* identity = executable_identity_for_path(pathname);
        if (identity != NULL) return copy_readlink_result(identity, out, out_size);
    }

    ssize_t (*fn)(int, const char*, char*, size_t) =
        REAL(readlinkat, ssize_t (*)(int, const char*, char*, size_t));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(dirfd, path, out, out_size);
}

unsigned long getauxval(unsigned long type) {
    if (type == AT_EXECFN) {
        const char* identity = active_executable_identity();
        if (identity != NULL) return (unsigned long)(uintptr_t)identity;
    }
    unsigned long (*fn)(unsigned long) =
        REAL(getauxval, unsigned long (*)(unsigned long));
    return fn(type);
}

/* ------------------------------------------------------------------------- */
/* stdio and directory wrappers                                               */
/* ------------------------------------------------------------------------- */

FILE* fopen(const char* pathname, const char* mode) {
    FILE* (*fn)(const char*, const char*) = REAL(fopen, FILE* (*)(const char*, const char*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return NULL;
    return fn(path, mode);
}

FILE* fopen64(const char* pathname, const char* mode) {
    FILE* (*fn)(const char*, const char*) = OPT_REAL(fopen64, FILE* (*)(const char*, const char*));
    if (fn == NULL) fn = REAL(fopen, FILE* (*)(const char*, const char*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return NULL;
    return fn(path, mode);
}

FILE* freopen(const char* pathname, const char* mode, FILE* stream) {
    FILE* (*fn)(const char*, const char*, FILE*) =
        REAL(freopen, FILE* (*)(const char*, const char*, FILE*));
    if (pathname == NULL) return fn(NULL, mode, stream);
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return NULL;
    return fn(path, mode, stream);
}

FILE* freopen64(const char* pathname, const char* mode, FILE* stream) {
    FILE* (*fn)(const char*, const char*, FILE*) =
        OPT_REAL(freopen64, FILE* (*)(const char*, const char*, FILE*));
    if (fn == NULL) fn = REAL(freopen, FILE* (*)(const char*, const char*, FILE*));
    if (pathname == NULL) return fn(NULL, mode, stream);
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return NULL;
    return fn(path, mode, stream);
}

DIR* opendir(const char* pathname) {
    DIR* (*fn)(const char*) = REAL(opendir, DIR* (*)(const char*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return NULL;
    return fn(path);
}

int scandir(
    const char* pathname,
    struct dirent*** namelist,
    int (*filter)(const struct dirent*),
    int (*compare)(const struct dirent**, const struct dirent**)
) {
    int (*fn)(const char*, struct dirent***, int (*)(const struct dirent*),
              int (*)(const struct dirent**, const struct dirent**)) =
        REAL(scandir, int (*)(const char*, struct dirent***,
                             int (*)(const struct dirent*),
                             int (*)(const struct dirent**, const struct dirent**)));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, namelist, filter, compare);
}

int scandirat(
    int dirfd,
    const char* pathname,
    struct dirent*** namelist,
    int (*filter)(const struct dirent*),
    int (*compare)(const struct dirent**, const struct dirent**)
) {
    int (*fn)(int, const char*, struct dirent***, int (*)(const struct dirent*),
              int (*)(const struct dirent**, const struct dirent**)) =
        OPT_REAL(scandirat, int (*)(int, const char*, struct dirent***,
                                   int (*)(const struct dirent*),
                                   int (*)(const struct dirent**, const struct dirent**)));
    if (fn == NULL) {
        errno = ENOSYS;
        return -1;
    }
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(dirfd, path, namelist, filter, compare);
}

/* ------------------------------------------------------------------------- */
/* open/create wrappers                                                       */
/* ------------------------------------------------------------------------- */

static int open_needs_mode(int flags) {
    if ((flags & O_CREAT) != 0) return 1;
#ifdef O_TMPFILE
    if ((flags & O_TMPFILE) == O_TMPFILE) return 1;
#endif
    return 0;
}

int open(const char* pathname, int flags, ...) {
    int (*fn)(const char*, int, ...) = REAL(open, int (*)(const char*, int, ...));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;

    if (open_needs_mode(flags)) {
        va_list arguments;
        va_start(arguments, flags);
        mode_t mode = va_arg(arguments, mode_t);
        va_end(arguments);
        return fn(path, flags, mode);
    }
    return fn(path, flags);
}

int open64(const char* pathname, int flags, ...) {
    int (*fn)(const char*, int, ...) = OPT_REAL(open64, int (*)(const char*, int, ...));
    if (fn == NULL) fn = REAL(open, int (*)(const char*, int, ...));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;

    if (open_needs_mode(flags)) {
        va_list arguments;
        va_start(arguments, flags);
        mode_t mode = va_arg(arguments, mode_t);
        va_end(arguments);
        return fn(path, flags, mode);
    }
    return fn(path, flags);
}

int openat(int dirfd, const char* pathname, int flags, ...) {
    int (*fn)(int, const char*, int, ...) =
        REAL(openat, int (*)(int, const char*, int, ...));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;

    if (open_needs_mode(flags)) {
        va_list arguments;
        va_start(arguments, flags);
        mode_t mode = va_arg(arguments, mode_t);
        va_end(arguments);
        return fn(dirfd, path, flags, mode);
    }
    return fn(dirfd, path, flags);
}

int openat64(int dirfd, const char* pathname, int flags, ...) {
    int (*fn)(int, const char*, int, ...) =
        OPT_REAL(openat64, int (*)(int, const char*, int, ...));
    if (fn == NULL) fn = REAL(openat, int (*)(int, const char*, int, ...));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;

    if (open_needs_mode(flags)) {
        va_list arguments;
        va_start(arguments, flags);
        mode_t mode = va_arg(arguments, mode_t);
        va_end(arguments);
        return fn(dirfd, path, flags, mode);
    }
    return fn(dirfd, path, flags);
}

int creat(const char* pathname, mode_t mode) {
    int (*fn)(const char*, mode_t) = REAL(creat, int (*)(const char*, mode_t));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, mode);
}

int creat64(const char* pathname, mode_t mode) {
    int (*fn)(const char*, mode_t) = OPT_REAL(creat64, int (*)(const char*, mode_t));
    if (fn == NULL) fn = REAL(creat, int (*)(const char*, mode_t));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, mode);
}

/* ------------------------------------------------------------------------- */
/* stat wrappers                                                              */
/* ------------------------------------------------------------------------- */

int statx(int dirfd, const char* pathname, int flags, unsigned int mask, struct statx* st) {
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;

    int (*fn)(int, const char*, int, unsigned int, struct statx*) =
        OPT_REAL(statx, int (*)(int, const char*, int, unsigned int, struct statx*));
    int result;
    if (fn != NULL) {
        result = fn(dirfd, path, flags, mask, st);
    } else {
#ifdef SYS_statx
        result = (int)real_syscall_call(
            SYS_statx,
            dirfd,
            (long)path,
            flags,
            mask,
            (long)st,
            0
        );
#else
        errno = ENOSYS;
        return -1;
#endif
    }

    if (result == 0 && is_perf_path_either(pathname, path)) spoof_statx_if_perf_data(st);
    return result;
}

int stat(const char* pathname, struct stat* st) {
    int (*fn)(const char*, struct stat*) = REAL(stat, int (*)(const char*, struct stat*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    int result = fn(path, st);
    if (result == 0 && is_perf_path_either(pathname, path)) spoof_stat_if_perf_data(st);
    return result;
}

int lstat(const char* pathname, struct stat* st) {
    int (*fn)(const char*, struct stat*) = REAL(lstat, int (*)(const char*, struct stat*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    int result = fn(path, st);
    if (result == 0 && is_perf_path_either(pathname, path)) spoof_stat_if_perf_data(st);
    return result;
}

int fstat(int fd, struct stat* st) {
    int (*fn)(int, struct stat*) = REAL(fstat, int (*)(int, struct stat*));
    int result = fn(fd, st);
    if (result == 0 && fd_points_to_perf_data(fd)) spoof_stat_if_perf_data(st);
    return result;
}

int fstatat(int dirfd, const char* pathname, struct stat* st, int flags) {
    int (*fn)(int, const char*, struct stat*, int) =
        REAL(fstatat, int (*)(int, const char*, struct stat*, int));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    int result = fn(dirfd, path, st, flags);
    if (result == 0 && is_perf_path_either(pathname, path)) spoof_stat_if_perf_data(st);
    return result;
}

int newfstatat(int dirfd, const char* pathname, struct stat* st, int flags) {
    return fstatat(dirfd, pathname, st, flags);
}

int __newfstatat(int dirfd, const char* pathname, struct stat* st, int flags) {
    return fstatat(dirfd, pathname, st, flags);
}

#if defined(__USE_LARGEFILE64) || defined(_LARGEFILE64_SOURCE)
int stat64(const char* pathname, struct stat64* st) {
    int (*fn)(const char*, struct stat64*) =
        OPT_REAL(stat64, int (*)(const char*, struct stat64*));
    if (fn == NULL) {
        errno = ENOSYS;
        return -1;
    }
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    int result = fn(path, st);
    if (result == 0 && is_perf_path_either(pathname, path)) spoof_stat64_if_perf_data(st);
    return result;
}

int lstat64(const char* pathname, struct stat64* st) {
    int (*fn)(const char*, struct stat64*) =
        OPT_REAL(lstat64, int (*)(const char*, struct stat64*));
    if (fn == NULL) {
        errno = ENOSYS;
        return -1;
    }
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    int result = fn(path, st);
    if (result == 0 && is_perf_path_either(pathname, path)) spoof_stat64_if_perf_data(st);
    return result;
}

int fstat64(int fd, struct stat64* st) {
    int (*fn)(int, struct stat64*) = OPT_REAL(fstat64, int (*)(int, struct stat64*));
    if (fn == NULL) {
        errno = ENOSYS;
        return -1;
    }
    int result = fn(fd, st);
    if (result == 0 && fd_points_to_perf_data(fd)) spoof_stat64_if_perf_data(st);
    return result;
}

int fstatat64(int dirfd, const char* pathname, struct stat64* st, int flags) {
    int (*fn)(int, const char*, struct stat64*, int) =
        OPT_REAL(fstatat64, int (*)(int, const char*, struct stat64*, int));
    if (fn == NULL) {
        errno = ENOSYS;
        return -1;
    }
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    int result = fn(dirfd, path, st, flags);
    if (result == 0 && is_perf_path_either(pathname, path)) spoof_stat64_if_perf_data(st);
    return result;
}
#endif

int __xstat(int version, const char* pathname, struct stat* st) {
    int (*fn)(int, const char*, struct stat*) =
        OPT_REAL(__xstat, int (*)(int, const char*, struct stat*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    int result = fn != NULL ? fn(version, path, st)
                            : REAL(stat, int (*)(const char*, struct stat*))(path, st);
    if (result == 0 && is_perf_path_either(pathname, path)) spoof_stat_if_perf_data(st);
    return result;
}

int __lxstat(int version, const char* pathname, struct stat* st) {
    int (*fn)(int, const char*, struct stat*) =
        OPT_REAL(__lxstat, int (*)(int, const char*, struct stat*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    int result = fn != NULL ? fn(version, path, st)
                            : REAL(lstat, int (*)(const char*, struct stat*))(path, st);
    if (result == 0 && is_perf_path_either(pathname, path)) spoof_stat_if_perf_data(st);
    return result;
}

int __fxstat(int version, int fd, struct stat* st) {
    int (*fn)(int, int, struct stat*) =
        OPT_REAL(__fxstat, int (*)(int, int, struct stat*));
    int result = fn != NULL ? fn(version, fd, st)
                            : REAL(fstat, int (*)(int, struct stat*))(fd, st);
    if (result == 0 && fd_points_to_perf_data(fd)) spoof_stat_if_perf_data(st);
    return result;
}

int __fxstatat(int version, int dirfd, const char* pathname, struct stat* st, int flags) {
    int (*fn)(int, int, const char*, struct stat*, int) =
        OPT_REAL(__fxstatat, int (*)(int, int, const char*, struct stat*, int));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    int result = fn != NULL ? fn(version, dirfd, path, st, flags)
                            : REAL(fstatat, int (*)(int, const char*, struct stat*, int))(
                                  dirfd, path, st, flags
                              );
    if (result == 0 && is_perf_path_either(pathname, path)) spoof_stat_if_perf_data(st);
    return result;
}

#if defined(__USE_LARGEFILE64) || defined(_LARGEFILE64_SOURCE)
int __xstat64(int version, const char* pathname, struct stat64* st) {
    int (*fn)(int, const char*, struct stat64*) =
        OPT_REAL(__xstat64, int (*)(int, const char*, struct stat64*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    int result = fn != NULL ? fn(version, path, st) : stat64(path, st);
    if (result == 0 && is_perf_path_either(pathname, path)) spoof_stat64_if_perf_data(st);
    return result;
}

int __lxstat64(int version, const char* pathname, struct stat64* st) {
    int (*fn)(int, const char*, struct stat64*) =
        OPT_REAL(__lxstat64, int (*)(int, const char*, struct stat64*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    int result = fn != NULL ? fn(version, path, st) : lstat64(path, st);
    if (result == 0 && is_perf_path_either(pathname, path)) spoof_stat64_if_perf_data(st);
    return result;
}

int __fxstat64(int version, int fd, struct stat64* st) {
    int (*fn)(int, int, struct stat64*) =
        OPT_REAL(__fxstat64, int (*)(int, int, struct stat64*));
    int result = fn != NULL ? fn(version, fd, st) : fstat64(fd, st);
    if (result == 0 && fd_points_to_perf_data(fd)) spoof_stat64_if_perf_data(st);
    return result;
}

int __fxstatat64(
    int version,
    int dirfd,
    const char* pathname,
    struct stat64* st,
    int flags
) {
    int (*fn)(int, int, const char*, struct stat64*, int) =
        OPT_REAL(__fxstatat64, int (*)(int, int, const char*, struct stat64*, int));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    int result = fn != NULL ? fn(version, dirfd, path, st, flags)
                            : fstatat64(dirfd, path, st, flags);
    if (result == 0 && is_perf_path_either(pathname, path)) spoof_stat64_if_perf_data(st);
    return result;
}
#endif

/* ------------------------------------------------------------------------- */
/* Directory and node creation                                                */
/* ------------------------------------------------------------------------- */

int mkdir(const char* pathname, mode_t mode) {
    int (*fn)(const char*, mode_t) = REAL(mkdir, int (*)(const char*, mode_t));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, mode);
}

int mkdirat(int dirfd, const char* pathname, mode_t mode) {
    int (*fn)(int, const char*, mode_t) =
        REAL(mkdirat, int (*)(int, const char*, mode_t));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(dirfd, path, mode);
}

int mkfifo(const char* pathname, mode_t mode) {
    int (*fn)(const char*, mode_t) = REAL(mkfifo, int (*)(const char*, mode_t));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, mode);
}

int mkfifoat(int dirfd, const char* pathname, mode_t mode) {
    int (*fn)(int, const char*, mode_t) =
        REAL(mkfifoat, int (*)(int, const char*, mode_t));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(dirfd, path, mode);
}

int mknod(const char* pathname, mode_t mode, dev_t device) {
    int (*fn)(const char*, mode_t, dev_t) =
        REAL(mknod, int (*)(const char*, mode_t, dev_t));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, mode, device);
}

int mknodat(int dirfd, const char* pathname, mode_t mode, dev_t device) {
    int (*fn)(int, const char*, mode_t, dev_t) =
        REAL(mknodat, int (*)(int, const char*, mode_t, dev_t));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(dirfd, path, mode, device);
}

int rmdir(const char* pathname) {
    int (*fn)(const char*) = REAL(rmdir, int (*)(const char*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path);
}

int __rmdir(const char* pathname) {
    int (*fn)(const char*) = OPT_REAL(__rmdir, int (*)(const char*));
    if (fn == NULL) fn = REAL(rmdir, int (*)(const char*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path);
}

/* ------------------------------------------------------------------------- */
/* Permissions and ownership                                                  */
/* ------------------------------------------------------------------------- */

int chmod(const char* pathname, mode_t mode) {
    int (*fn)(const char*, mode_t) = REAL(chmod, int (*)(const char*, mode_t));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, mode);
}

int fchmodat(int dirfd, const char* pathname, mode_t mode, int flags) {
    int (*fn)(int, const char*, mode_t, int) =
        REAL(fchmodat, int (*)(int, const char*, mode_t, int));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(dirfd, path, mode, flags);
}

int chown(const char* pathname, uid_t owner, gid_t group) {
    int (*fn)(const char*, uid_t, gid_t) =
        REAL(chown, int (*)(const char*, uid_t, gid_t));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, owner, group);
}

int lchown(const char* pathname, uid_t owner, gid_t group) {
    int (*fn)(const char*, uid_t, gid_t) =
        REAL(lchown, int (*)(const char*, uid_t, gid_t));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, owner, group);
}

int fchownat(int dirfd, const char* pathname, uid_t owner, gid_t group, int flags) {
    int (*fn)(int, const char*, uid_t, gid_t, int) =
        REAL(fchownat, int (*)(int, const char*, uid_t, gid_t, int));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(dirfd, path, owner, group, flags);
}

/* ------------------------------------------------------------------------- */
/* Removal, links and renames                                                 */
/* ------------------------------------------------------------------------- */

int unlink(const char* pathname) {
    int (*fn)(const char*) = REAL(unlink, int (*)(const char*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path);
}

int __unlink(const char* pathname) {
    int (*fn)(const char*) = OPT_REAL(__unlink, int (*)(const char*));
    if (fn == NULL) fn = REAL(unlink, int (*)(const char*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path);
}

int unlinkat(int dirfd, const char* pathname, int flags) {
    int (*fn)(int, const char*, int) = REAL(unlinkat, int (*)(int, const char*, int));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(dirfd, path, flags);
}

int __unlinkat(int dirfd, const char* pathname, int flags) {
    int (*fn)(int, const char*, int) =
        OPT_REAL(__unlinkat, int (*)(int, const char*, int));
    if (fn == NULL) fn = REAL(unlinkat, int (*)(int, const char*, int));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(dirfd, path, flags);
}

int remove(const char* pathname) {
    int (*fn)(const char*) = REAL(remove, int (*)(const char*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path);
}

static int path_is_android_external_storage(const char* path) {
    if (path == NULL || path[0] != '/') return 0;

    return path_starts_with_component(path, "/storage/emulated") ||
           path_starts_with_component(path, "/sdcard") ||
           path_starts_with_component(path, "/mnt/user") ||
           path_starts_with_component(path, "/mnt/runtime");
}

static int path_reference_is_android_external_storage(
    int dirfd,
    const char* path
) {
    if (path == NULL || path[0] == '\0') return 0;
    if (path[0] == '/') return path_is_android_external_storage(path);

    char directory[REDIR_BUF_SIZE];
    if (dirfd == AT_FDCWD) {
        if (getcwd(directory, sizeof(directory)) == NULL) return 0;
    } else if (!read_dirfd_path(dirfd, directory, sizeof(directory))) {
        return 0;
    }

    return path_is_android_external_storage(directory);
}

static int should_emulate_hardlink(
    int error,
    int source_dirfd,
    const char* source,
    int destination_dirfd,
    const char* destination
) {
    int saved_errno = errno;
    int external =
        path_reference_is_android_external_storage(source_dirfd, source) ||
        path_reference_is_android_external_storage(destination_dirfd, destination);

    int should_emulate = external &&
        (error == EACCES ||
         error == EPERM ||
         error == EXDEV ||
         error == EOPNOTSUPP);

    errno = saved_errno;
    return should_emulate;
}

static int copy_regular_file_at(
    int source_dirfd,
    const char* source,
    int destination_dirfd,
    const char* destination,
    int follow_source_symlink
) {
    int (*real_openat_fn)(int, const char*, int, ...) =
        REAL(openat, int (*)(int, const char*, int, ...));

    int (*real_unlinkat_fn)(int, const char*, int) =
        REAL(unlinkat, int (*)(int, const char*, int));

    int source_flags = O_RDONLY | O_CLOEXEC;
#ifdef O_NOFOLLOW
    if (!follow_source_symlink) source_flags |= O_NOFOLLOW;
#endif

    int source_fd = real_openat_fn(
        source_dirfd,
        source,
        source_flags
    );

    if (source_fd < 0) {
        return -1;
    }

    struct stat source_stat;

    if (fstat(source_fd, &source_stat) < 0) {
        int saved_errno = errno;
        close(source_fd);
        errno = saved_errno;
        return -1;
    }

    if (!S_ISREG(source_stat.st_mode)) {
        close(source_fd);
        errno = EOPNOTSUPP;
        return -1;
    }

    int destination_fd = real_openat_fn(
        destination_dirfd,
        destination,
        O_WRONLY | O_CREAT | O_EXCL | O_CLOEXEC,
        source_stat.st_mode & 07777
    );

    if (destination_fd < 0) {
        int saved_errno = errno;
        close(source_fd);
        errno = saved_errno;
        return -1;
    }

    char buffer[64 * 1024];
    int result = 0;

    for (;;) {
        ssize_t count = read(source_fd, buffer, sizeof(buffer));

        if (count == 0) {
            break;
        }

        if (count < 0) {
            if (errno == EINTR) {
                continue;
            }

            result = -1;
            break;
        }

        ssize_t offset = 0;

        while (offset < count) {
            ssize_t written = write(
                destination_fd,
                buffer + offset,
                (size_t)(count - offset)
            );

            if (written < 0) {
                if (errno == EINTR) {
                    continue;
                }

                result = -1;
                break;
            }

            if (written == 0) {
                errno = EIO;
                result = -1;
                break;
            }

            offset += written;
        }

        if (result < 0) {
            break;
        }
    }

    int saved_errno = errno;

    if (result == 0) {
        /*
         * Android may reject some mode bits, so do not fail extraction merely
         * because setuid/setgid bits cannot be restored.
         */
        (void)fchmod(destination_fd, source_stat.st_mode & 07777);

#if defined(__linux__)
        const struct timespec source_times[2] = {
            source_stat.st_atim,
            source_stat.st_mtim
        };
        (void)futimens(destination_fd, source_times);
#endif
    }

    if (close(destination_fd) < 0 && result == 0) {
        result = -1;
        saved_errno = errno;
    }

    close(source_fd);

    if (result < 0) {
        real_unlinkat_fn(destination_dirfd, destination, 0);
        errno = saved_errno;
    }

    return result;
}

static int emulate_hardlink_at(
    int source_dirfd,
    const char* source,
    int destination_dirfd,
    const char* destination,
    int follow_source_symlink
) {
    int result = copy_regular_file_at(
        source_dirfd,
        source,
        destination_dirfd,
        destination,
        follow_source_symlink
    );

    if (result == 0 && path_redirect_debug_enabled()) {
        fprintf(
            stderr,
            "path_redirect: emulated hardlink by copying %s -> %s\n",
            source,
            destination
        );
    }

    return result;
}

int link(const char* oldpath, const char* newpath) {
    int caller_errno = errno;
    int (*fn)(const char*, const char*) =
        REAL(link, int (*)(const char*, const char*));

    char old_buffer[REDIR_BUF_SIZE];
    char new_buffer[REDIR_BUF_SIZE];

    const char* old_redirected =
        redirect_path(oldpath, old_buffer, sizeof(old_buffer));

    const char* new_redirected =
        redirect_path(newpath, new_buffer, sizeof(new_buffer));

    if (old_redirected == NULL || new_redirected == NULL) {
        return -1;
    }

    int result = fn(old_redirected, new_redirected);

    if (result == 0) {
        return 0;
    }

    int link_errno = errno;

    if (!should_emulate_hardlink(
            link_errno,
            AT_FDCWD,
            old_redirected,
            AT_FDCWD,
            new_redirected
        )) {
        return -1;
    }

    if (emulate_hardlink_at(
            AT_FDCWD,
            old_redirected,
            AT_FDCWD,
            new_redirected,
            0
        ) == 0) {
        errno = caller_errno;
        return 0;
    }

    /*
     * Prefer the emulation error when it describes a real copy failure.
     * Otherwise preserve the original link failure.
     */
    if (errno == EOPNOTSUPP) {
        errno = link_errno;
    }

    return -1;
}

int linkat(
    int olddirfd,
    const char* oldpath,
    int newdirfd,
    const char* newpath,
    int flags
) {
    int caller_errno = errno;
    int (*fn)(int, const char*, int, const char*, int) =
        REAL(linkat, int (*)(int, const char*, int, const char*, int));

    char old_buffer[REDIR_BUF_SIZE];
    char new_buffer[REDIR_BUF_SIZE];

    const char* old_redirected = redirect_at_path(
        olddirfd,
        oldpath,
        old_buffer,
        sizeof(old_buffer)
    );

    const char* new_redirected = redirect_at_path(
        newdirfd,
        newpath,
        new_buffer,
        sizeof(new_buffer)
    );

    if (old_redirected == NULL || new_redirected == NULL) {
        return -1;
    }

    int result = fn(
        olddirfd,
        old_redirected,
        newdirfd,
        new_redirected,
        flags
    );

    if (result == 0) {
        return 0;
    }

    int link_errno = errno;

    if (!should_emulate_hardlink(
            link_errno,
            olddirfd,
            old_redirected,
            newdirfd,
            new_redirected
        )) {
        return -1;
    }

    /*
     * Tar's normal hard-link extraction uses path-based linkat without
     * AT_EMPTY_PATH. Leave unusual fd-only operations untouched.
     */
    if ((flags & AT_EMPTY_PATH) != 0 || oldpath[0] == '\0') {
        errno = link_errno;
        return -1;
    }

    if (emulate_hardlink_at(
            olddirfd,
            old_redirected,
            newdirfd,
            new_redirected,
            (flags & AT_SYMLINK_FOLLOW) != 0
        ) == 0) {
        errno = caller_errno;
        return 0;
    }

    if (errno == EOPNOTSUPP) {
        errno = link_errno;
    }

    return -1;
}

int symlink(const char* target, const char* linkpath) {
    int (*fn)(const char*, const char*) =
        REAL(symlink, int (*)(const char*, const char*));
    char link_buffer[REDIR_BUF_SIZE];
    const char* redirected_link = redirect_path(linkpath, link_buffer, sizeof(link_buffer));
    if (redirected_link == NULL) return -1;
    /* Keep the target text virtual; redirect only where the symlink is created. */
    return fn(target, redirected_link);
}

int symlinkat(const char* target, int newdirfd, const char* linkpath) {
    int (*fn)(const char*, int, const char*) =
        REAL(symlinkat, int (*)(const char*, int, const char*));
    char link_buffer[REDIR_BUF_SIZE];
    const char* redirected_link =
        redirect_at_path(newdirfd, linkpath, link_buffer, sizeof(link_buffer));
    if (redirected_link == NULL) return -1;
    return fn(target, newdirfd, redirected_link);
}

int rename(const char* oldpath, const char* newpath) {
    int (*fn)(const char*, const char*) =
        REAL(rename, int (*)(const char*, const char*));
    char old_buffer[REDIR_BUF_SIZE];
    char new_buffer[REDIR_BUF_SIZE];
    const char* old_redirected = redirect_path(oldpath, old_buffer, sizeof(old_buffer));
    const char* new_redirected = redirect_path(newpath, new_buffer, sizeof(new_buffer));
    if (old_redirected == NULL || new_redirected == NULL) return -1;
    return fn(old_redirected, new_redirected);
}

int renameat(int olddirfd, const char* oldpath, int newdirfd, const char* newpath) {
    int (*fn)(int, const char*, int, const char*) =
        REAL(renameat, int (*)(int, const char*, int, const char*));
    char old_buffer[REDIR_BUF_SIZE];
    char new_buffer[REDIR_BUF_SIZE];
    const char* old_redirected = redirect_at_path(olddirfd, oldpath, old_buffer, sizeof(old_buffer));
    const char* new_redirected = redirect_at_path(newdirfd, newpath, new_buffer, sizeof(new_buffer));
    if (old_redirected == NULL || new_redirected == NULL) return -1;
    return fn(olddirfd, old_redirected, newdirfd, new_redirected);
}

static int renameat2_can_fall_back(int error, unsigned int flags) {
    if (flags != 0) return 0;

    return error == ENOSYS ||
           error == EINVAL ||
           error == EOPNOTSUPP;
}

static int call_renameat2_compat(
    int olddirfd,
    const char* oldpath,
    int newdirfd,
    const char* newpath,
    unsigned int flags
) {
    int caller_errno = errno;
    int result;
    int (*fn)(int, const char*, int, const char*, unsigned int) =
        OPT_REAL(renameat2, int (*)(int, const char*, int, const char*, unsigned int));

    if (fn != NULL) {
        result = fn(olddirfd, oldpath, newdirfd, newpath, flags);
    } else {
#ifdef SYS_renameat2
        result = (int)real_syscall_call(
            SYS_renameat2,
            olddirfd,
            (long)oldpath,
            newdirfd,
            (long)newpath,
            flags,
            0
        );
#else
        errno = ENOSYS;
        result = -1;
#endif
    }

    if (result != -1 || !renameat2_can_fall_back(errno, flags)) {
        return result;
    }

    /* renameat2(..., flags = 0) is exactly renameat(). */
    int original_errno = errno;
    int (*fallback)(int, const char*, int, const char*) =
        REAL(renameat, int (*)(int, const char*, int, const char*));

    result = fallback(olddirfd, oldpath, newdirfd, newpath);
    if (result == 0) {
        if (path_redirect_debug_enabled()) {
            fprintf(
                stderr,
                "path_redirect: renameat2(flags=0) fell back to renameat after errno=%d\n",
                original_errno
            );
        }
        errno = caller_errno;
    }
    return result;
}

int renameat2(
    int olddirfd,
    const char* oldpath,
    int newdirfd,
    const char* newpath,
    unsigned int flags
) {
    char old_buffer[REDIR_BUF_SIZE];
    char new_buffer[REDIR_BUF_SIZE];
    const char* old_redirected =
        redirect_at_path(olddirfd, oldpath, old_buffer, sizeof(old_buffer));
    const char* new_redirected =
        redirect_at_path(newdirfd, newpath, new_buffer, sizeof(new_buffer));
    if (old_redirected == NULL || new_redirected == NULL) return -1;

    return call_renameat2_compat(
        olddirfd,
        old_redirected,
        newdirfd,
        new_redirected,
        flags
    );
}


/* ------------------------------------------------------------------------- */
/* Direct syscall wrapper                                                     */
/*                                                                           */
/* Keep this in addition to the public libc interposers above. Some programs  */
/* call syscall(2) directly and would otherwise bypass path redirection.      */
/* ------------------------------------------------------------------------- */

__attribute__((visibility("hidden"), noinline, used))
long path_redirect_syscall_dispatch(
    long number,
    long argument1,
    long argument2,
    long argument3,
    long argument4,
    long argument5,
    long argument6
) {
    char path_buffer[REDIR_BUF_SIZE];
    char second_path_buffer[REDIR_BUF_SIZE];

#ifdef SYS_readlink
    if (number == SYS_readlink) {
        const char* original = (const char*)argument1;
        const char* identity = executable_identity_for_path(original);
        if (identity != NULL) {
            return copy_readlink_result(
                identity,
                (char*)argument2,
                (size_t)argument3
            );
        }

        const char* path = redirect_path(
            original,
            path_buffer,
            sizeof(path_buffer)
        );
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_readlink)", original, path);
        return real_syscall_call(
            number,
            (long)path,
            argument2,
            argument3,
            argument4,
            argument5,
            argument6
        );
    }
#endif

#ifdef SYS_readlinkat
    if (number == SYS_readlinkat) {
        int dirfd = (int)argument1;
        const char* original = (const char*)argument2;

        if (original != NULL && original[0] == '/') {
            const char* identity = executable_identity_for_path(original);
            if (identity != NULL) {
                return copy_readlink_result(
                    identity,
                    (char*)argument3,
                    (size_t)argument4
                );
            }
        }

        const char* path = redirect_at_path(
            dirfd,
            original,
            path_buffer,
            sizeof(path_buffer)
        );
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_readlinkat)", original, path);
        return real_syscall_call(
            number,
            argument1,
            (long)path,
            argument3,
            argument4,
            argument5,
            argument6
        );
    }
#endif

#ifdef SYS_statx
    if (number == SYS_statx) {
        int dirfd = (int)argument1;
        const char* original = (const char*)argument2;
        const char* path = redirect_at_path(
            dirfd,
            original,
            path_buffer,
            sizeof(path_buffer)
        );
        if (path == NULL) return -1;

        debug_path_operation("syscall(SYS_statx)", original, path);
        long result = real_syscall_call(
            number,
            argument1,
            (long)path,
            argument3,
            argument4,
            argument5,
            argument6
        );

        if (result == 0 && is_perf_path_either(original, path)) {
            spoof_statx_if_perf_data((struct statx*)argument5);
        }
        return result;
    }
#endif

#ifdef SYS_newfstatat
    if (number == SYS_newfstatat) {
        int dirfd = (int)argument1;
        const char* original = (const char*)argument2;
        const char* path = redirect_at_path(
            dirfd,
            original,
            path_buffer,
            sizeof(path_buffer)
        );
        if (path == NULL) return -1;

        debug_path_operation("syscall(SYS_newfstatat)", original, path);
        long result = real_syscall_call(
            number,
            argument1,
            (long)path,
            argument3,
            argument4,
            argument5,
            argument6
        );
        if (result == 0 && is_perf_path_either(original, path)) {
            spoof_stat_if_perf_data((struct stat*)argument3);
        }
        return result;
    }
#endif

#if defined(SYS_fstatat64) && (!defined(SYS_newfstatat) || SYS_fstatat64 != SYS_newfstatat)
    if (number == SYS_fstatat64) {
        int dirfd = (int)argument1;
        const char* original = (const char*)argument2;
        const char* path = redirect_at_path(
            dirfd,
            original,
            path_buffer,
            sizeof(path_buffer)
        );
        if (path == NULL) return -1;

        debug_path_operation("syscall(SYS_fstatat64)", original, path);
        long result = real_syscall_call(
            number,
            argument1,
            (long)path,
            argument3,
            argument4,
            argument5,
            argument6
        );
        if (result == 0 && is_perf_path_either(original, path)) {
            spoof_stat64_if_perf_data((struct stat64*)argument3);
        }
        return result;
    }
#endif

#ifdef SYS_stat
    if (number == SYS_stat) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_stat)", original, path);
        long result = real_syscall_call(
            number,
            (long)path,
            argument2,
            argument3,
            argument4,
            argument5,
            argument6
        );
        if (result == 0 && is_perf_path_either(original, path)) {
            spoof_stat_if_perf_data((struct stat*)argument2);
        }
        return result;
    }
#endif

#ifdef SYS_lstat
    if (number == SYS_lstat) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_lstat)", original, path);
        long result = real_syscall_call(
            number,
            (long)path,
            argument2,
            argument3,
            argument4,
            argument5,
            argument6
        );
        if (result == 0 && is_perf_path_either(original, path)) {
            spoof_stat_if_perf_data((struct stat*)argument2);
        }
        return result;
    }
#endif

#ifdef SYS_access
    if (number == SYS_access) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_access)", original, path);
        return real_syscall_call(
            number,
            (long)path,
            argument2,
            argument3,
            argument4,
            argument5,
            argument6
        );
    }
#endif

#ifdef SYS_faccessat
    if (number == SYS_faccessat) {
        int dirfd = (int)argument1;
        const char* original = (const char*)argument2;
        const char* path = redirect_at_path(dirfd, original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_faccessat)", original, path);
        return real_syscall_call(
            number,
            argument1,
            (long)path,
            argument3,
            argument4,
            argument5,
            argument6
        );
    }
#endif

#ifdef SYS_faccessat2
    if (number == SYS_faccessat2) {
        int dirfd = (int)argument1;
        const char* original = (const char*)argument2;
        const char* path = redirect_at_path(dirfd, original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_faccessat2)", original, path);
        return real_syscall_call(
            number,
            argument1,
            (long)path,
            argument3,
            argument4,
            argument5,
            argument6
        );
    }
#endif

#ifdef SYS_chdir
    if (number == SYS_chdir) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_chdir)", original, path);
        return real_syscall_call(
            number,
            (long)path,
            argument2,
            argument3,
            argument4,
            argument5,
            argument6
        );
    }
#endif

#ifdef SYS_mkdir
    if (number == SYS_mkdir) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_mkdir)", original, path);
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_mkdirat
    if (number == SYS_mkdirat) {
        int dirfd = (int)argument1;
        const char* original = (const char*)argument2;
        const char* path = redirect_at_path(dirfd, original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_mkdirat)", original, path);
        return real_syscall_call(number, argument1, (long)path, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_mknod
    if (number == SYS_mknod) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_mknod)", original, path);
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_mknodat
    if (number == SYS_mknodat) {
        int dirfd = (int)argument1;
        const char* original = (const char*)argument2;
        const char* path = redirect_at_path(dirfd, original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_mknodat)", original, path);
        return real_syscall_call(number, argument1, (long)path, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_rmdir
    if (number == SYS_rmdir) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_rmdir)", original, path);
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_unlink
    if (number == SYS_unlink) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_unlink)", original, path);
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_unlinkat
    if (number == SYS_unlinkat) {
        int dirfd = (int)argument1;
        const char* original = (const char*)argument2;
        const char* path = redirect_at_path(dirfd, original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_unlinkat)", original, path);
        return real_syscall_call(number, argument1, (long)path, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_open
    if (number == SYS_open) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_open)", original, path);
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_openat
    if (number == SYS_openat) {
        int dirfd = (int)argument1;
        const char* original = (const char*)argument2;
        const char* path = redirect_at_path(dirfd, original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_openat)", original, path);
        return real_syscall_call(number, argument1, (long)path, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_openat2
    if (number == SYS_openat2) {
        int dirfd = (int)argument1;
        const char* original = (const char*)argument2;
        const char* path = redirect_at_path(dirfd, original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_openat2)", original, path);
        return real_syscall_call(number, argument1, (long)path, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_link
    if (number == SYS_link) {
        int caller_errno = errno;
        const char* old_original = (const char*)argument1;
        const char* new_original = (const char*)argument2;

        const char* old_path = redirect_path(
            old_original,
            path_buffer,
            sizeof(path_buffer)
        );

        const char* new_path = redirect_path(
            new_original,
            second_path_buffer,
            sizeof(second_path_buffer)
        );

        if (old_path == NULL || new_path == NULL) {
            return -1;
        }

        long result = real_syscall_call(
            number,
            (long)old_path,
            (long)new_path,
            argument3,
            argument4,
            argument5,
            argument6
        );

        if (result == 0) {
            return 0;
        }

        int link_errno = errno;

        if (should_emulate_hardlink(
                link_errno,
                AT_FDCWD,
                old_path,
                AT_FDCWD,
                new_path
            ) &&
            emulate_hardlink_at(
                AT_FDCWD,
                old_path,
                AT_FDCWD,
                new_path,
                0
            ) == 0) {
            errno = caller_errno;
            return 0;
        }

        if (errno == EOPNOTSUPP) {
            errno = link_errno;
        }

        return -1;
    }
#endif

#ifdef SYS_linkat
    if (number == SYS_linkat) {
        int caller_errno = errno;
        int olddirfd = (int)argument1;
        const char* old_original = (const char*)argument2;
        int newdirfd = (int)argument3;
        const char* new_original = (const char*)argument4;
        int flags = (int)argument5;

        const char* old_path = redirect_at_path(
            olddirfd,
            old_original,
            path_buffer,
            sizeof(path_buffer)
        );

        const char* new_path = redirect_at_path(
            newdirfd,
            new_original,
            second_path_buffer,
            sizeof(second_path_buffer)
        );

        if (old_path == NULL || new_path == NULL) {
            return -1;
        }

        long result = real_syscall_call(
            number,
            argument1,
            (long)old_path,
            argument3,
            (long)new_path,
            argument5,
            argument6
        );

        if (result == 0) {
            return 0;
        }

        int link_errno = errno;

        if ((flags & AT_EMPTY_PATH) == 0 &&
            old_original != NULL &&
            old_original[0] != '\0' &&
            should_emulate_hardlink(
                link_errno,
                olddirfd,
                old_path,
                newdirfd,
                new_path
            ) &&
            emulate_hardlink_at(
                olddirfd,
                old_path,
                newdirfd,
                new_path,
                (flags & AT_SYMLINK_FOLLOW) != 0
            ) == 0) {
            errno = caller_errno;
            return 0;
        }

        if (errno == EOPNOTSUPP) {
            errno = link_errno;
        }

        return -1;
    }
#endif

#ifdef SYS_symlink
    if (number == SYS_symlink) {
        const char* target = (const char*)argument1;
        const char* link_original = (const char*)argument2;
        const char* link_path = redirect_path(link_original, path_buffer, sizeof(path_buffer));
        if (link_path == NULL) return -1;
        debug_path_operation("syscall(SYS_symlink link)", link_original, link_path);
        return real_syscall_call(number, (long)target, (long)link_path, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_symlinkat
    if (number == SYS_symlinkat) {
        const char* target = (const char*)argument1;
        int newdirfd = (int)argument2;
        const char* link_original = (const char*)argument3;
        const char* link_path = redirect_at_path(newdirfd, link_original, path_buffer, sizeof(path_buffer));
        if (link_path == NULL) return -1;
        debug_path_operation("syscall(SYS_symlinkat link)", link_original, link_path);
        return real_syscall_call(number, (long)target, argument2, (long)link_path, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_rename
    if (number == SYS_rename) {
        const char* old_original = (const char*)argument1;
        const char* new_original = (const char*)argument2;
        const char* old_path = redirect_path(old_original, path_buffer, sizeof(path_buffer));
        const char* new_path = redirect_path(new_original, second_path_buffer, sizeof(second_path_buffer));
        if (old_path == NULL || new_path == NULL) return -1;
        debug_path_operation("syscall(SYS_rename old)", old_original, old_path);
        debug_path_operation("syscall(SYS_rename new)", new_original, new_path);
        return real_syscall_call(number, (long)old_path, (long)new_path, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_renameat
    if (number == SYS_renameat) {
        int olddirfd = (int)argument1;
        const char* old_original = (const char*)argument2;
        int newdirfd = (int)argument3;
        const char* new_original = (const char*)argument4;
        const char* old_path = redirect_at_path(olddirfd, old_original, path_buffer, sizeof(path_buffer));
        const char* new_path = redirect_at_path(newdirfd, new_original, second_path_buffer, sizeof(second_path_buffer));
        if (old_path == NULL || new_path == NULL) return -1;
        debug_path_operation("syscall(SYS_renameat old)", old_original, old_path);
        debug_path_operation("syscall(SYS_renameat new)", new_original, new_path);
        return real_syscall_call(number, argument1, (long)old_path, argument3, (long)new_path, argument5, argument6);
    }
#endif

#ifdef SYS_renameat2
    if (number == SYS_renameat2) {
        int olddirfd = (int)argument1;
        const char* old_original = (const char*)argument2;
        int newdirfd = (int)argument3;
        const char* new_original = (const char*)argument4;
        const char* old_path = redirect_at_path(olddirfd, old_original, path_buffer, sizeof(path_buffer));
        const char* new_path = redirect_at_path(newdirfd, new_original, second_path_buffer, sizeof(second_path_buffer));
        if (old_path == NULL || new_path == NULL) return -1;
        debug_path_operation("syscall(SYS_renameat2 old)", old_original, old_path);
        debug_path_operation("syscall(SYS_renameat2 new)", new_original, new_path);
        return call_renameat2_compat(
            olddirfd,
            old_path,
            newdirfd,
            new_path,
            (unsigned int)argument5
        );
    }
#endif

#ifdef SYS_chmod
    if (number == SYS_chmod) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_chmod)", original, path);
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_fchmodat
    if (number == SYS_fchmodat) {
        int dirfd = (int)argument1;
        const char* original = (const char*)argument2;
        const char* path = redirect_at_path(dirfd, original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_fchmodat)", original, path);
        return real_syscall_call(number, argument1, (long)path, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_fchmodat2
    if (number == SYS_fchmodat2) {
        int dirfd = (int)argument1;
        const char* original = (const char*)argument2;
        const char* path = redirect_at_path(dirfd, original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_fchmodat2)", original, path);
        return real_syscall_call(number, argument1, (long)path, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_chown
    if (number == SYS_chown) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_chown)", original, path);
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_lchown
    if (number == SYS_lchown) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_lchown)", original, path);
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_fchownat
    if (number == SYS_fchownat) {
        int dirfd = (int)argument1;
        const char* original = (const char*)argument2;
        const char* path = redirect_at_path(dirfd, original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_fchownat)", original, path);
        return real_syscall_call(number, argument1, (long)path, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_truncate
    if (number == SYS_truncate) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_truncate)", original, path);
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_truncate64
    if (number == SYS_truncate64) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_truncate64)", original, path);
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_utimensat
    if (number == SYS_utimensat) {
        int dirfd = (int)argument1;
        const char* original = (const char*)argument2;
        if (original == NULL) {
            return real_syscall_call(number, argument1, argument2, argument3, argument4, argument5, argument6);
        }
        const char* path = redirect_at_path(dirfd, original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_utimensat)", original, path);
        return real_syscall_call(number, argument1, (long)path, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_statfs
    if (number == SYS_statfs) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_statfs)", original, path);
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_inotify_add_watch
    if (number == SYS_inotify_add_watch) {
        int caller_errno = errno;
        const char* original = (const char*)argument2;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        debug_path_operation("syscall(SYS_inotify_add_watch)", original, path);

        long result = real_syscall_call(
            number,
            argument1,
            (long)path,
            argument3,
            argument4,
            argument5,
            argument6
        );

        if (result == -1 && errno == EACCES && original != NULL && strcmp(original, "/") == 0) {
            int saved_errno = errno;
            result = real_syscall_call(
                number,
                argument1,
                (long)app_files_dir(),
                argument3,
                argument4,
                argument5,
                argument6
            );
            if (result != -1) {
                errno = caller_errno;
                return result;
            }
            errno = saved_errno;
        }
        return result;
    }
#endif

#ifdef SYS_connect
    if (number == SYS_connect) {
        struct sockaddr_un redirected_address;
        socklen_t redirected_length = (socklen_t)argument3;
        const struct sockaddr* redirected = redirect_unix_socket_address(
            (const struct sockaddr*)argument2,
            (socklen_t)argument3,
            &redirected_address,
            &redirected_length,
            path_buffer,
            sizeof(path_buffer)
        );
        if (redirected == NULL) return -1;
        return real_syscall_call(number, argument1, (long)redirected, redirected_length, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_bind
    if (number == SYS_bind) {
        struct sockaddr_un redirected_address;
        socklen_t redirected_length = (socklen_t)argument3;
        const struct sockaddr* redirected = redirect_unix_socket_address(
            (const struct sockaddr*)argument2,
            (socklen_t)argument3,
            &redirected_address,
            &redirected_length,
            path_buffer,
            sizeof(path_buffer)
        );
        if (redirected == NULL) return -1;
        return real_syscall_call(number, argument1, (long)redirected, redirected_length, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_sendto
    if (number == SYS_sendto) {
        struct sockaddr_un redirected_address;
        socklen_t redirected_length = (socklen_t)argument6;
        const struct sockaddr* redirected = redirect_unix_socket_address(
            (const struct sockaddr*)argument5,
            (socklen_t)argument6,
            &redirected_address,
            &redirected_length,
            path_buffer,
            sizeof(path_buffer)
        );
        if (redirected == NULL) return -1;
        return real_syscall_call(number, argument1, argument2, argument3, argument4, (long)redirected, redirected_length);
    }
#endif

#ifdef SYS_sendmsg
    if (number == SYS_sendmsg) {
        const struct msghdr* message = (const struct msghdr*)argument2;
        if (message == NULL || message->msg_name == NULL) {
            return real_syscall_call(number, argument1, argument2, argument3, argument4, argument5, argument6);
        }
        struct sockaddr_un redirected_address;
        socklen_t redirected_length = (socklen_t)message->msg_namelen;
        const struct sockaddr* redirected = redirect_unix_socket_address(
            (const struct sockaddr*)message->msg_name,
            (socklen_t)message->msg_namelen,
            &redirected_address,
            &redirected_length,
            path_buffer,
            sizeof(path_buffer)
        );
        if (redirected == NULL) return -1;
        if (redirected == (const struct sockaddr*)message->msg_name) {
            return real_syscall_call(number, argument1, argument2, argument3, argument4, argument5, argument6);
        }
        struct msghdr redirected_message = *message;
        redirected_message.msg_name = (void*)redirected;
        redirected_message.msg_namelen = redirected_length;
        return real_syscall_call(number, argument1, (long)&redirected_message, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_getxattr
    if (number == SYS_getxattr) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_lgetxattr
    if (number == SYS_lgetxattr) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_setxattr
    if (number == SYS_setxattr) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_lsetxattr
    if (number == SYS_lsetxattr) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_listxattr
    if (number == SYS_listxattr) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_llistxattr
    if (number == SYS_llistxattr) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_removexattr
    if (number == SYS_removexattr) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

#ifdef SYS_lremovexattr
    if (number == SYS_lremovexattr) {
        const char* original = (const char*)argument1;
        const char* path = redirect_path(original, path_buffer, sizeof(path_buffer));
        if (path == NULL) return -1;
        return real_syscall_call(number, (long)path, argument2, argument3, argument4, argument5, argument6);
    }
#endif

    return real_syscall_call(
        number,
        argument1,
        argument2,
        argument3,
        argument4,
        argument5,
        argument6
    );
}

/*
 * On AArch64 the syscall number and six possible arguments arrive in x0-x6,
 * exactly matching path_redirect_syscall_dispatch(). A naked tail branch keeps
 * those registers intact and avoids reading varargs that the caller did not
 * provide. The old implementation unconditionally consumed six va_args, which
 * is undefined behaviour for common calls such as syscall(SYS_gettid).
 */
#if defined(__aarch64__)
/*
 * Define the public variadic symbol in assembly. Clang still emits a variadic
 * register-save sequence for a C "naked" function, which would write below the
 * caller's stack pointer before our branch and corrupt memory.
 */
__asm__(
    ".text\n"
    ".global syscall\n"
    ".type syscall, %function\n"
    "syscall:\n"
    "b path_redirect_syscall_dispatch\n"
    ".size syscall, .-syscall\n"
);
#elif defined(__x86_64__)
__asm__(
    ".text\n"
    ".global syscall\n"
    ".type syscall, @function\n"
    "syscall:\n"
    "jmp path_redirect_syscall_dispatch\n"
    ".size syscall, .-syscall\n"
);
#else
long syscall(long number, ...) {
    va_list arguments;
    va_start(arguments, number);

    long argument1 = va_arg(arguments, long);
    long argument2 = va_arg(arguments, long);
    long argument3 = va_arg(arguments, long);
    long argument4 = va_arg(arguments, long);
    long argument5 = va_arg(arguments, long);
    long argument6 = va_arg(arguments, long);

    va_end(arguments);

    return path_redirect_syscall_dispatch(
        number,
        argument1,
        argument2,
        argument3,
        argument4,
        argument5,
        argument6
    );
}
#endif

/* ------------------------------------------------------------------------- */
/* Timestamps, sizes and filesystem metadata                                  */
/* ------------------------------------------------------------------------- */

int utime(const char* pathname, const struct utimbuf* times) {
    int (*fn)(const char*, const struct utimbuf*) =
        REAL(utime, int (*)(const char*, const struct utimbuf*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, times);
}

int utimes(const char* pathname, const struct timeval times[2]) {
    int (*fn)(const char*, const struct timeval[2]) =
        REAL(utimes, int (*)(const char*, const struct timeval[2]));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, times);
}

int lutimes(const char* pathname, const struct timeval times[2]) {
    int (*fn)(const char*, const struct timeval[2]) =
        REAL(lutimes, int (*)(const char*, const struct timeval[2]));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, times);
}

int futimesat(int dirfd, const char* pathname, const struct timeval times[2]) {
    int (*fn)(int, const char*, const struct timeval[2]) =
        REAL(futimesat, int (*)(int, const char*, const struct timeval[2]));
    if (pathname == NULL) return fn(dirfd, NULL, times);
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(dirfd, path, times);
}

int utimensat(int dirfd, const char* pathname, const struct timespec times[2], int flags) {
    int (*fn)(int, const char*, const struct timespec[2], int) =
        REAL(utimensat, int (*)(int, const char*, const struct timespec[2], int));
    if (pathname == NULL) return fn(dirfd, NULL, times, flags);
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(dirfd, path, times, flags);
}

int truncate(const char* pathname, off_t length) {
    int (*fn)(const char*, off_t) = REAL(truncate, int (*)(const char*, off_t));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, length);
}

int truncate64(const char* pathname, off64_t length) {
    int (*fn)(const char*, off64_t) =
        OPT_REAL(truncate64, int (*)(const char*, off64_t));
    if (fn == NULL) {
        errno = ENOSYS;
        return -1;
    }
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, length);
}

int statfs(const char* pathname, struct statfs* buffer) {
    int (*fn)(const char*, struct statfs*) =
        REAL(statfs, int (*)(const char*, struct statfs*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, buffer);
}

int statfs64(const char* pathname, struct statfs64* buffer) {
    int (*fn)(const char*, struct statfs64*) =
        OPT_REAL(statfs64, int (*)(const char*, struct statfs64*));
    if (fn == NULL) {
        errno = ENOSYS;
        return -1;
    }
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, buffer);
}

int statvfs(const char* pathname, struct statvfs* buffer) {
    int (*fn)(const char*, struct statvfs*) =
        REAL(statvfs, int (*)(const char*, struct statvfs*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, buffer);
}

int statvfs64(const char* pathname, struct statvfs64* buffer) {
    int (*fn)(const char*, struct statvfs64*) =
        OPT_REAL(statvfs64, int (*)(const char*, struct statvfs64*));
    if (fn == NULL) {
        errno = ENOSYS;
        return -1;
    }
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, buffer);
}

/* ------------------------------------------------------------------------- */
/* Extended-attribute path wrappers                                           */
/* ------------------------------------------------------------------------- */

ssize_t getxattr(const char* pathname, const char* name, void* value, size_t size) {
    ssize_t (*fn)(const char*, const char*, void*, size_t) =
        REAL(getxattr, ssize_t (*)(const char*, const char*, void*, size_t));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, name, value, size);
}

ssize_t lgetxattr(const char* pathname, const char* name, void* value, size_t size) {
    ssize_t (*fn)(const char*, const char*, void*, size_t) =
        REAL(lgetxattr, ssize_t (*)(const char*, const char*, void*, size_t));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, name, value, size);
}

int setxattr(
    const char* pathname,
    const char* name,
    const void* value,
    size_t size,
    int flags
) {
    int (*fn)(const char*, const char*, const void*, size_t, int) =
        REAL(setxattr, int (*)(const char*, const char*, const void*, size_t, int));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, name, value, size, flags);
}

int lsetxattr(
    const char* pathname,
    const char* name,
    const void* value,
    size_t size,
    int flags
) {
    int (*fn)(const char*, const char*, const void*, size_t, int) =
        REAL(lsetxattr, int (*)(const char*, const char*, const void*, size_t, int));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, name, value, size, flags);
}

ssize_t listxattr(const char* pathname, char* list, size_t size) {
    ssize_t (*fn)(const char*, char*, size_t) =
        REAL(listxattr, ssize_t (*)(const char*, char*, size_t));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, list, size);
}

ssize_t llistxattr(const char* pathname, char* list, size_t size) {
    ssize_t (*fn)(const char*, char*, size_t) =
        REAL(llistxattr, ssize_t (*)(const char*, char*, size_t));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, list, size);
}

int removexattr(const char* pathname, const char* name) {
    int (*fn)(const char*, const char*) =
        REAL(removexattr, int (*)(const char*, const char*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, name);
}

int lremovexattr(const char* pathname, const char* name) {
    int (*fn)(const char*, const char*) =
        REAL(lremovexattr, int (*)(const char*, const char*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;
    return fn(path, name);
}

/* ------------------------------------------------------------------------- */
/* Canonical path wrappers                                                    */
/* ------------------------------------------------------------------------- */

char* realpath(const char* pathname, char* resolved_path) {
    char* (*fn)(const char*, char*) = REAL(realpath, char* (*)(const char*, char*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return NULL;
    return fn(path, resolved_path);
}

/*
 * With _FORTIFY_SOURCE enabled, calls such as
 *
 *     char resolved[PATH_MAX];
 *     realpath(path, resolved);
 *
 * are compiled as __realpath_chk(path, resolved, sizeof(resolved)).  Pacman
 * and libalpm are built with fortification, so intercepting realpath() alone
 * is not sufficient.  Route the fortified entry point through the same
 * virtual-root mapping while still using libc's real implementation.
 */
char* __realpath_chk(
    const char* pathname,
    char* resolved_path,
    size_t resolved_path_size
) {
    if (resolved_path != NULL && resolved_path_size < PATH_MAX) {
        errno = ENAMETOOLONG;
        return NULL;
    }

    char* (*fn)(const char*, char*) = REAL(realpath, char* (*)(const char*, char*));
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return NULL;

    debug_path_operation("__realpath_chk", pathname, path);
    return fn(path, resolved_path);
}

char* canonicalize_file_name(const char* pathname) {
    char* (*fn)(const char*) =
        OPT_REAL(canonicalize_file_name, char* (*)(const char*));
    if (fn == NULL) {
        errno = ENOSYS;
        return NULL;
    }
    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return NULL;
    return fn(path);
}

/* ------------------------------------------------------------------------- */
/* inotify                                                                    */
/* ------------------------------------------------------------------------- */

int inotify_add_watch(int fd, const char* pathname, uint32_t mask) {
    int caller_errno = errno;
    int (*fn)(int, const char*, uint32_t) =
        REAL(inotify_add_watch, int (*)(int, const char*, uint32_t));

    char path_buffer[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, path_buffer, sizeof(path_buffer));
    if (path == NULL) return -1;

    int watch = fn(fd, path, mask);

    /* Android rejects a watch on the real filesystem root. */
    if (watch == -1 && errno == EACCES && pathname != NULL &&
        strcmp(pathname, "/") == 0) {
        int saved_errno = errno;
        watch = fn(fd, app_files_dir(), mask);
        if (watch != -1) {
            errno = caller_errno;
            return watch;
        }
        errno = saved_errno;
    }

    return watch;
}
