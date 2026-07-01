#define _GNU_SOURCE
#define _LARGEFILE64_SOURCE

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dlfcn.h>
#include <pthread.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>
#include <stdarg.h>
#include <unistd.h>
#include <errno.h>
#include <dirent.h>

/*
 * libpath_redirect.c
 *
 * LD_PRELOAD shim for running glibc/JDK tooling in an Android app-private
 * filesystem layout.
 *
 * Redirects:
 *   /tmp and /tmp/...                      -> $TMPDIR
 *   any path ending in /etc/resolv.conf    -> $RESOLV_CONF_PATH
 *   any path ending in /etc/hosts          -> $HOSTS_PATH
 *   any path ending in /etc/nsswitch.conf  -> $NSSWITCH_CONF_PATH
 *   any path ending in /etc/gai.conf       -> $GAI_CONF_PATH
 *
 * The suffix matching is intentional: some glibc/Termux-derived builds are
 * configured with sysconfdir paths such as
 * /data/data/com.termux/usr/etc/resolv.conf instead of literal /etc/resolv.conf.
 *
 * Also spoofs selected stat fields for hsperfdata_* paths so HotSpot's
 * perfdata security checks accept an app-private/custom TMPDIR.
 *
 * Build:
 *   gcc -shared -fPIC -O2 -Wall -Wextra \
 *       -o libpath_redirect.so libpath_redirect.c dns_fallback.c -ldl -pthread
 */

#define REDIR_BUF_SIZE 4096
#define ARRAY_LEN(a) (sizeof(a) / sizeof((a)[0]))

/* ------------------------------------------------------------------------- */
/* Thread-safe RTLD_NEXT symbol cache                                         */
/* ------------------------------------------------------------------------- */

static void* lookup_next_symbol(const char* name) {
    dlerror();
    return dlsym(RTLD_NEXT, name);
}

static void require_symbol_or_abort(const char* name, void* symbol) {
    if (!symbol) {
        fprintf(stderr, "tmpredir: failed to resolve symbol: %s\n", name);
        abort();
    }
}

#define DECLARE_REAL_SYMBOL(sym)                                      \
    static void* real_##sym##_ptr = NULL;                             \
    static pthread_once_t real_##sym##_once = PTHREAD_ONCE_INIT;       \
    static void init_##sym(void) {                                     \
        real_##sym##_ptr = lookup_next_symbol(#sym);                   \
    }                                                                 \
    static void* get_##sym(int required) {                             \
        pthread_once(&real_##sym##_once, init_##sym);                  \
        if (required) require_symbol_or_abort(#sym, real_##sym##_ptr); \
        return real_##sym##_ptr;                                       \
    }

#define REAL(sym, type) ((type) get_##sym(1))
#define OPT_REAL(sym, type) ((type) get_##sym(0))

DECLARE_REAL_SYMBOL(access)
DECLARE_REAL_SYMBOL(canonicalize_file_name)
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
DECLARE_REAL_SYMBOL(lchown)
DECLARE_REAL_SYMBOL(link)
DECLARE_REAL_SYMBOL(linkat)
DECLARE_REAL_SYMBOL(lstat)
DECLARE_REAL_SYMBOL(lstat64)
DECLARE_REAL_SYMBOL(mkdir)
DECLARE_REAL_SYMBOL(mkdirat)
DECLARE_REAL_SYMBOL(open)
DECLARE_REAL_SYMBOL(open64)
DECLARE_REAL_SYMBOL(openat)
DECLARE_REAL_SYMBOL(openat64)
DECLARE_REAL_SYMBOL(opendir)
DECLARE_REAL_SYMBOL(readlink)
DECLARE_REAL_SYMBOL(readlinkat)
DECLARE_REAL_SYMBOL(realpath)
DECLARE_REAL_SYMBOL(remove)
DECLARE_REAL_SYMBOL(rename)
DECLARE_REAL_SYMBOL(renameat)
DECLARE_REAL_SYMBOL(renameat2)
DECLARE_REAL_SYMBOL(rmdir)
DECLARE_REAL_SYMBOL(stat)
DECLARE_REAL_SYMBOL(stat64)
DECLARE_REAL_SYMBOL(symlink)
DECLARE_REAL_SYMBOL(symlinkat)
DECLARE_REAL_SYMBOL(unlink)
DECLARE_REAL_SYMBOL(unlinkat)
DECLARE_REAL_SYMBOL(__fxstat)
DECLARE_REAL_SYMBOL(__fxstat64)
DECLARE_REAL_SYMBOL(__fxstatat)
DECLARE_REAL_SYMBOL(__fxstatat64)
DECLARE_REAL_SYMBOL(__lxstat)
DECLARE_REAL_SYMBOL(__lxstat64)
DECLARE_REAL_SYMBOL(__xstat)
DECLARE_REAL_SYMBOL(__xstat64)

/* ------------------------------------------------------------------------- */
/* Path redirection                                                           */
/* ------------------------------------------------------------------------- */

typedef struct {
    const char* suffix;
    const char* env;
} suffix_redirect_t;

static const suffix_redirect_t suffix_redirects[] = {
    { "/etc/resolv.conf",   "RESOLV_CONF_PATH"   },
    { "/etc/hosts",         "HOSTS_PATH"         },
    { "/etc/nsswitch.conf", "NSSWITCH_CONF_PATH" },
    { "/etc/gai.conf",      "GAI_CONF_PATH"      },
};

static int path_ends_with(const char* path, const char* suffix) {
    if (!path || !suffix) return 0;

    size_t path_len = strlen(path);
    size_t suffix_len = strlen(suffix);

    if (path_len < suffix_len) return 0;
    return strcmp(path + path_len - suffix_len, suffix) == 0;
}


static int path_redirect_debug_enabled(void) {
    const char* v = getenv("PATH_REDIRECT_DEBUG");
    return v && *v && strcmp(v, "0") != 0;
}

static void debug_redirect(const char* from, const char* to, const char* env) {
    if (!path_redirect_debug_enabled()) return;
    fprintf(stderr, "path_redirect: %s -> %s via %s\n", from, to ? to : "(null)", env);
}


static const char* redirect_path(const char* path, char* buffer, size_t buffer_size) {
    if (!path) return path;

    for (size_t i = 0; i < ARRAY_LEN(suffix_redirects); i++) {
        if (path_ends_with(path, suffix_redirects[i].suffix)) {
            const char* replacement = getenv(suffix_redirects[i].env);
            if (replacement && *replacement) {
                debug_redirect(path, replacement, suffix_redirects[i].env);
                snprintf(buffer, buffer_size, "%s", replacement);
                return buffer;
            }
            return path;
        }
    }

    const char* tmpdir = getenv("TMPDIR");
    if (!tmpdir || !*tmpdir) return path;

    if (strcmp(path, "/tmp") == 0) {
        snprintf(buffer, buffer_size, "%s", tmpdir);
        return buffer;
    }

    if (strncmp(path, "/tmp/", 5) == 0) {
        size_t len = strlen(tmpdir);
        snprintf(
            buffer,
            buffer_size,
            len > 0 && tmpdir[len - 1] == '/' ? "%s%s" : "%s/%s",
            tmpdir,
            path + 5
        );
        return buffer;
    }

    return path;
}

static inline int should_redirect_at(int dirfd, const char* path) {
    return path && (path[0] == '/' || dirfd == AT_FDCWD);
}

static inline const char* redirect_at_path(
    int dirfd,
    const char* path,
    char* buffer,
    size_t buffer_size
) {
    return should_redirect_at(dirfd, path)
        ? redirect_path(path, buffer, buffer_size)
        : path;
}

/* ------------------------------------------------------------------------- */
/* HotSpot hsperfdata stat spoofing                                           */
/* ------------------------------------------------------------------------- */

static int is_perf_path(const char* path) {
    return path && strstr(path, "hsperfdata_") != NULL;
}

static int is_perf_path_either(const char* a, const char* b) {
    return is_perf_path(a) || is_perf_path(b);
}

static void spoof_stat_if_perf_data(struct stat* st) {
    if (!st) return;

    st->st_uid = geteuid();

    if (S_ISDIR(st->st_mode)) {
        st->st_mode &= ~(S_IWGRP | S_IWOTH);
    } else if (S_ISREG(st->st_mode)) {
        st->st_mode &= ~(S_IWGRP | S_IWOTH);
        st->st_nlink = 1;
    }
}

#if defined(__USE_LARGEFILE64) || defined(_LARGEFILE64_SOURCE)
static void spoof_stat64_if_perf_data(struct stat64* st) {
    if (!st) return;

    st->st_uid = geteuid();

    if (S_ISDIR(st->st_mode)) {
        st->st_mode &= ~(S_IWGRP | S_IWOTH);
    } else if (S_ISREG(st->st_mode)) {
        st->st_mode &= ~(S_IWGRP | S_IWOTH);
        st->st_nlink = 1;
    }
}
#endif

static int fd_points_to_perf_data(int fd) {
    char proc_path[64];
    char link_path[REDIR_BUF_SIZE];

    snprintf(proc_path, sizeof(proc_path), "/proc/self/fd/%d", fd);

    ssize_t n = readlink(proc_path, link_path, sizeof(link_path) - 1);
    if (n <= 0) return 0;

    link_path[n] = '\0';
    return is_perf_path(link_path);
}

/* ------------------------------------------------------------------------- */
/* Real stat helpers, used by normal and legacy xstat wrappers                */
/* ------------------------------------------------------------------------- */

static int real_stat_call(const char* path, struct stat* st) {
    int (*fn)(const char*, struct stat*) = REAL(stat, int (*)(const char*, struct stat*));
    return fn(path, st);
}

static int real_lstat_call(const char* path, struct stat* st) {
    int (*fn)(const char*, struct stat*) = REAL(lstat, int (*)(const char*, struct stat*));
    return fn(path, st);
}

static int real_fstat_call(int fd, struct stat* st) {
    int (*fn)(int, struct stat*) = REAL(fstat, int (*)(int, struct stat*));
    return fn(fd, st);
}

static int real_fstatat_call(int dirfd, const char* path, struct stat* st, int flags) {
    int (*fn)(int, const char*, struct stat*, int) =
        REAL(fstatat, int (*)(int, const char*, struct stat*, int));
    return fn(dirfd, path, st, flags);
}

#if defined(__USE_LARGEFILE64) || defined(_LARGEFILE64_SOURCE)
static int real_stat64_call(const char* path, struct stat64* st) {
    int (*fn)(const char*, struct stat64*) =
        OPT_REAL(stat64, int (*)(const char*, struct stat64*));
    if (fn) return fn(path, st);

#if defined(__LP64__) || defined(_LP64)
    return real_stat_call(path, (struct stat*) st);
#else
    errno = ENOSYS;
    return -1;
#endif
}

static int real_lstat64_call(const char* path, struct stat64* st) {
    int (*fn)(const char*, struct stat64*) =
        OPT_REAL(lstat64, int (*)(const char*, struct stat64*));
    if (fn) return fn(path, st);

#if defined(__LP64__) || defined(_LP64)
    return real_lstat_call(path, (struct stat*) st);
#else
    errno = ENOSYS;
    return -1;
#endif
}

static int real_fstat64_call(int fd, struct stat64* st) {
    int (*fn)(int, struct stat64*) =
        OPT_REAL(fstat64, int (*)(int, struct stat64*));
    if (fn) return fn(fd, st);

#if defined(__LP64__) || defined(_LP64)
    return real_fstat_call(fd, (struct stat*) st);
#else
    errno = ENOSYS;
    return -1;
#endif
}

static int real_fstatat64_call(int dirfd, const char* path, struct stat64* st, int flags) {
    int (*fn)(int, const char*, struct stat64*, int) =
        OPT_REAL(fstatat64, int (*)(int, const char*, struct stat64*, int));
    if (fn) return fn(dirfd, path, st, flags);

#if defined(__LP64__) || defined(_LP64)
    return real_fstatat_call(dirfd, path, (struct stat*) st, flags);
#else
    errno = ENOSYS;
    return -1;
#endif
}
#endif

/* ------------------------------------------------------------------------- */
/* Small helper macros for repetitive wrappers                                */
/* ------------------------------------------------------------------------- */

#define WRAP_PATH_1(ret, name, type, args, call_args)        \
    ret name args {                                          \
        type fn = REAL(name, type);                          \
        char path_buf[REDIR_BUF_SIZE];                       \
        pathname = redirect_path(pathname, path_buf, sizeof(path_buf)); \
        return fn call_args;                                 \
    }

#define WRAP_AT_PATH_1(ret, name, type, args, call_args)     \
    ret name args {                                          \
        type fn = REAL(name, type);                          \
        char path_buf[REDIR_BUF_SIZE];                       \
        pathname = redirect_at_path(dirfd, pathname, path_buf, sizeof(path_buf)); \
        return fn call_args;                                 \
    }

static inline int open_needs_mode(int flags) {
#if defined(O_TMPFILE)
    return (flags & O_CREAT) || ((flags & O_TMPFILE) == O_TMPFILE);
#else
    return (flags & O_CREAT);
#endif
}

/* ------------------------------------------------------------------------- */
/* stdio and directory wrappers                                               */
/* ------------------------------------------------------------------------- */

FILE* fopen(const char* pathname, const char* mode) {
    FILE* (*fn)(const char*, const char*) = REAL(fopen, FILE* (*)(const char*, const char*));
    char buf[REDIR_BUF_SIZE];
    return fn(redirect_path(pathname, buf, sizeof(buf)), mode);
}

FILE* freopen(const char* pathname, const char* mode, FILE* stream) {
    FILE* (*fn)(const char*, const char*, FILE*) =
        REAL(freopen, FILE* (*)(const char*, const char*, FILE*));
    char buf[REDIR_BUF_SIZE];
    return fn(redirect_path(pathname, buf, sizeof(buf)), mode, stream);
}

#if defined(__USE_LARGEFILE64) || defined(_LARGEFILE64_SOURCE)
FILE* fopen64(const char* pathname, const char* mode) {
    FILE* (*fn)(const char*, const char*) =
        OPT_REAL(fopen64, FILE* (*)(const char*, const char*));
    if (!fn) fn = REAL(fopen, FILE* (*)(const char*, const char*));
    char buf[REDIR_BUF_SIZE];
    return fn(redirect_path(pathname, buf, sizeof(buf)), mode);
}

FILE* freopen64(const char* pathname, const char* mode, FILE* stream) {
    FILE* (*fn)(const char*, const char*, FILE*) =
        OPT_REAL(freopen64, FILE* (*)(const char*, const char*, FILE*));
    if (!fn) fn = REAL(freopen, FILE* (*)(const char*, const char*, FILE*));
    char buf[REDIR_BUF_SIZE];
    return fn(redirect_path(pathname, buf, sizeof(buf)), mode, stream);
}
#endif

DIR* opendir(const char* name) {
    DIR* (*fn)(const char*) = REAL(opendir, DIR* (*)(const char*));
    char buf[REDIR_BUF_SIZE];
    return fn(redirect_path(name, buf, sizeof(buf)));
}

int mkdir(const char* pathname, mode_t mode) {
    int (*fn)(const char*, mode_t) = REAL(mkdir, int (*)(const char*, mode_t));
    char buf[REDIR_BUF_SIZE];
    return fn(redirect_path(pathname, buf, sizeof(buf)), mode);
}

int mkdirat(int dirfd, const char* pathname, mode_t mode) {
    int (*fn)(int, const char*, mode_t) = REAL(mkdirat, int (*)(int, const char*, mode_t));
    char buf[REDIR_BUF_SIZE];
    return fn(dirfd, redirect_at_path(dirfd, pathname, buf, sizeof(buf)), mode);
}

int rmdir(const char* pathname) {
    int (*fn)(const char*) = REAL(rmdir, int (*)(const char*));
    char buf[REDIR_BUF_SIZE];
    return fn(redirect_path(pathname, buf, sizeof(buf)));
}

/* ------------------------------------------------------------------------- */
/* open/create wrappers                                                       */
/* ------------------------------------------------------------------------- */

int open(const char* pathname, int flags, ...) {
    int (*fn)(const char*, int, ...) = REAL(open, int (*)(const char*, int, ...));
    char buf[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, buf, sizeof(buf));

    if (open_needs_mode(flags)) {
        va_list ap;
        va_start(ap, flags);
        mode_t mode = va_arg(ap, mode_t);
        va_end(ap);
        return fn(path, flags, mode);
    }

    return fn(path, flags);
}

#if defined(__USE_LARGEFILE64) || defined(_LARGEFILE64_SOURCE)
int open64(const char* pathname, int flags, ...) {
    int (*fn)(const char*, int, ...) =
        OPT_REAL(open64, int (*)(const char*, int, ...));
    if (!fn) fn = REAL(open, int (*)(const char*, int, ...));

    char buf[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, buf, sizeof(buf));

    if (open_needs_mode(flags)) {
        va_list ap;
        va_start(ap, flags);
        mode_t mode = va_arg(ap, mode_t);
        va_end(ap);
        return fn(path, flags, mode);
    }

    return fn(path, flags);
}
#endif

int openat(int dirfd, const char* pathname, int flags, ...) {
    int (*fn)(int, const char*, int, ...) =
        REAL(openat, int (*)(int, const char*, int, ...));
    char buf[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, buf, sizeof(buf));

    if (open_needs_mode(flags)) {
        va_list ap;
        va_start(ap, flags);
        mode_t mode = va_arg(ap, mode_t);
        va_end(ap);
        return fn(dirfd, path, flags, mode);
    }

    return fn(dirfd, path, flags);
}

#if defined(__USE_LARGEFILE64) || defined(_LARGEFILE64_SOURCE)
int openat64(int dirfd, const char* pathname, int flags, ...) {
    int (*fn)(int, const char*, int, ...) =
        OPT_REAL(openat64, int (*)(int, const char*, int, ...));
    if (!fn) fn = REAL(openat, int (*)(int, const char*, int, ...));

    char buf[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, buf, sizeof(buf));

    if (open_needs_mode(flags)) {
        va_list ap;
        va_start(ap, flags);
        mode_t mode = va_arg(ap, mode_t);
        va_end(ap);
        return fn(dirfd, path, flags, mode);
    }

    return fn(dirfd, path, flags);
}
#endif

int creat(const char* pathname, mode_t mode) {
    int (*fn)(const char*, mode_t) = REAL(creat, int (*)(const char*, mode_t));
    char buf[REDIR_BUF_SIZE];
    return fn(redirect_path(pathname, buf, sizeof(buf)), mode);
}

#if defined(__USE_LARGEFILE64) || defined(_LARGEFILE64_SOURCE)
int creat64(const char* pathname, mode_t mode) {
    int (*fn)(const char*, mode_t) = OPT_REAL(creat64, int (*)(const char*, mode_t));
    if (!fn) fn = REAL(creat, int (*)(const char*, mode_t));
    char buf[REDIR_BUF_SIZE];
    return fn(redirect_path(pathname, buf, sizeof(buf)), mode);
}
#endif

/* ------------------------------------------------------------------------- */
/* stat wrappers                                                              */
/* ------------------------------------------------------------------------- */

int stat(const char* pathname, struct stat* st) {
    char buf[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, buf, sizeof(buf));
    int rc = real_stat_call(path, st);
    if (rc == 0 && is_perf_path_either(pathname, path)) spoof_stat_if_perf_data(st);
    return rc;
}

int lstat(const char* pathname, struct stat* st) {
    char buf[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, buf, sizeof(buf));
    int rc = real_lstat_call(path, st);
    if (rc == 0 && is_perf_path_either(pathname, path)) spoof_stat_if_perf_data(st);
    return rc;
}

int fstat(int fd, struct stat* st) {
    int rc = real_fstat_call(fd, st);
    if (rc == 0 && fd_points_to_perf_data(fd)) spoof_stat_if_perf_data(st);
    return rc;
}

int fstatat(int dirfd, const char* pathname, struct stat* st, int flags) {
    char buf[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, buf, sizeof(buf));
    int rc = real_fstatat_call(dirfd, path, st, flags);
    if (rc == 0 && is_perf_path_either(pathname, path)) spoof_stat_if_perf_data(st);
    return rc;
}

#if defined(__USE_LARGEFILE64) || defined(_LARGEFILE64_SOURCE)
int stat64(const char* pathname, struct stat64* st) {
    char buf[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, buf, sizeof(buf));
    int rc = real_stat64_call(path, st);
    if (rc == 0 && is_perf_path_either(pathname, path)) spoof_stat64_if_perf_data(st);
    return rc;
}

int lstat64(const char* pathname, struct stat64* st) {
    char buf[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, buf, sizeof(buf));
    int rc = real_lstat64_call(path, st);
    if (rc == 0 && is_perf_path_either(pathname, path)) spoof_stat64_if_perf_data(st);
    return rc;
}

int fstat64(int fd, struct stat64* st) {
    int rc = real_fstat64_call(fd, st);
    if (rc == 0 && fd_points_to_perf_data(fd)) spoof_stat64_if_perf_data(st);
    return rc;
}

int fstatat64(int dirfd, const char* pathname, struct stat64* st, int flags) {
    char buf[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, buf, sizeof(buf));
    int rc = real_fstatat64_call(dirfd, path, st, flags);
    if (rc == 0 && is_perf_path_either(pathname, path)) spoof_stat64_if_perf_data(st);
    return rc;
}
#endif

/* ------------------------------------------------------------------------- */
/* Legacy glibc xstat wrappers                                                */
/* ------------------------------------------------------------------------- */

int __xstat(int ver, const char* pathname, struct stat* st) {
    int (*fn)(int, const char*, struct stat*) =
        OPT_REAL(__xstat, int (*)(int, const char*, struct stat*));
    char buf[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, buf, sizeof(buf));
    int rc = fn ? fn(ver, path, st) : real_stat_call(path, st);
    if (rc == 0 && is_perf_path_either(pathname, path)) spoof_stat_if_perf_data(st);
    return rc;
}

int __lxstat(int ver, const char* pathname, struct stat* st) {
    int (*fn)(int, const char*, struct stat*) =
        OPT_REAL(__lxstat, int (*)(int, const char*, struct stat*));
    char buf[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, buf, sizeof(buf));
    int rc = fn ? fn(ver, path, st) : real_lstat_call(path, st);
    if (rc == 0 && is_perf_path_either(pathname, path)) spoof_stat_if_perf_data(st);
    return rc;
}

int __fxstat(int ver, int fd, struct stat* st) {
    int (*fn)(int, int, struct stat*) = OPT_REAL(__fxstat, int (*)(int, int, struct stat*));
    int rc = fn ? fn(ver, fd, st) : real_fstat_call(fd, st);
    if (rc == 0 && fd_points_to_perf_data(fd)) spoof_stat_if_perf_data(st);
    return rc;
}

int __fxstatat(int ver, int dirfd, const char* pathname, struct stat* st, int flags) {
    int (*fn)(int, int, const char*, struct stat*, int) =
        OPT_REAL(__fxstatat, int (*)(int, int, const char*, struct stat*, int));
    char buf[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, buf, sizeof(buf));
    int rc = fn ? fn(ver, dirfd, path, st, flags) : real_fstatat_call(dirfd, path, st, flags);
    if (rc == 0 && is_perf_path_either(pathname, path)) spoof_stat_if_perf_data(st);
    return rc;
}

#if defined(__USE_LARGEFILE64) || defined(_LARGEFILE64_SOURCE)
int __xstat64(int ver, const char* pathname, struct stat64* st) {
    int (*fn)(int, const char*, struct stat64*) =
        OPT_REAL(__xstat64, int (*)(int, const char*, struct stat64*));
    char buf[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, buf, sizeof(buf));
    int rc = fn ? fn(ver, path, st) : real_stat64_call(path, st);
    if (rc == 0 && is_perf_path_either(pathname, path)) spoof_stat64_if_perf_data(st);
    return rc;
}

int __lxstat64(int ver, const char* pathname, struct stat64* st) {
    int (*fn)(int, const char*, struct stat64*) =
        OPT_REAL(__lxstat64, int (*)(int, const char*, struct stat64*));
    char buf[REDIR_BUF_SIZE];
    const char* path = redirect_path(pathname, buf, sizeof(buf));
    int rc = fn ? fn(ver, path, st) : real_lstat64_call(path, st);
    if (rc == 0 && is_perf_path_either(pathname, path)) spoof_stat64_if_perf_data(st);
    return rc;
}

int __fxstat64(int ver, int fd, struct stat64* st) {
    int (*fn)(int, int, struct stat64*) =
        OPT_REAL(__fxstat64, int (*)(int, int, struct stat64*));
    int rc = fn ? fn(ver, fd, st) : real_fstat64_call(fd, st);
    if (rc == 0 && fd_points_to_perf_data(fd)) spoof_stat64_if_perf_data(st);
    return rc;
}

int __fxstatat64(int ver, int dirfd, const char* pathname, struct stat64* st, int flags) {
    int (*fn)(int, int, const char*, struct stat64*, int) =
        OPT_REAL(__fxstatat64, int (*)(int, int, const char*, struct stat64*, int));
    char buf[REDIR_BUF_SIZE];
    const char* path = redirect_at_path(dirfd, pathname, buf, sizeof(buf));
    int rc = fn ? fn(ver, dirfd, path, st, flags) : real_fstatat64_call(dirfd, path, st, flags);
    if (rc == 0 && is_perf_path_either(pathname, path)) spoof_stat64_if_perf_data(st);
    return rc;
}
#endif

/* ------------------------------------------------------------------------- */
/* access/readlink/metadata wrappers                                          */
/* ------------------------------------------------------------------------- */

int access(const char* pathname, int mode) {
    int (*fn)(const char*, int) = REAL(access, int (*)(const char*, int));
    char buf[REDIR_BUF_SIZE];
    return fn(redirect_path(pathname, buf, sizeof(buf)), mode);
}

int faccessat(int dirfd, const char* pathname, int mode, int flags) {
    int (*fn)(int, const char*, int, int) =
        REAL(faccessat, int (*)(int, const char*, int, int));
    char buf[REDIR_BUF_SIZE];
    return fn(dirfd, redirect_at_path(dirfd, pathname, buf, sizeof(buf)), mode, flags);
}

#ifdef __linux__
int faccessat2(int dirfd, const char* pathname, int mode, int flags) {
    int (*fn)(int, const char*, int, int) =
        OPT_REAL(faccessat2, int (*)(int, const char*, int, int));
    char buf[REDIR_BUF_SIZE];
    pathname = redirect_at_path(dirfd, pathname, buf, sizeof(buf));
    if (fn) return fn(dirfd, pathname, mode, flags);
    return faccessat(dirfd, pathname, mode, flags);
}
#endif

ssize_t readlink(const char* pathname, char* out, size_t out_size) {
    ssize_t (*fn)(const char*, char*, size_t) =
        REAL(readlink, ssize_t (*)(const char*, char*, size_t));
    char buf[REDIR_BUF_SIZE];
    return fn(redirect_path(pathname, buf, sizeof(buf)), out, out_size);
}

ssize_t readlinkat(int dirfd, const char* pathname, char* out, size_t out_size) {
    ssize_t (*fn)(int, const char*, char*, size_t) =
        REAL(readlinkat, ssize_t (*)(int, const char*, char*, size_t));
    char buf[REDIR_BUF_SIZE];
    return fn(dirfd, redirect_at_path(dirfd, pathname, buf, sizeof(buf)), out, out_size);
}

int chmod(const char* pathname, mode_t mode) {
    int (*fn)(const char*, mode_t) = REAL(chmod, int (*)(const char*, mode_t));
    char buf[REDIR_BUF_SIZE];
    return fn(redirect_path(pathname, buf, sizeof(buf)), mode);
}

int fchmodat(int dirfd, const char* pathname, mode_t mode, int flags) {
    int (*fn)(int, const char*, mode_t, int) =
        REAL(fchmodat, int (*)(int, const char*, mode_t, int));
    char buf[REDIR_BUF_SIZE];
    return fn(dirfd, redirect_at_path(dirfd, pathname, buf, sizeof(buf)), mode, flags);
}

int chown(const char* pathname, uid_t owner, gid_t group) {
    int (*fn)(const char*, uid_t, gid_t) = REAL(chown, int (*)(const char*, uid_t, gid_t));
    char buf[REDIR_BUF_SIZE];
    return fn(redirect_path(pathname, buf, sizeof(buf)), owner, group);
}

int lchown(const char* pathname, uid_t owner, gid_t group) {
    int (*fn)(const char*, uid_t, gid_t) = REAL(lchown, int (*)(const char*, uid_t, gid_t));
    char buf[REDIR_BUF_SIZE];
    return fn(redirect_path(pathname, buf, sizeof(buf)), owner, group);
}

int fchownat(int dirfd, const char* pathname, uid_t owner, gid_t group, int flags) {
    int (*fn)(int, const char*, uid_t, gid_t, int) =
        REAL(fchownat, int (*)(int, const char*, uid_t, gid_t, int));
    char buf[REDIR_BUF_SIZE];
    return fn(dirfd, redirect_at_path(dirfd, pathname, buf, sizeof(buf)), owner, group, flags);
}

/* ------------------------------------------------------------------------- */
/* link/remove/rename wrappers                                                */
/* ------------------------------------------------------------------------- */

int symlink(const char* target, const char* linkpath) {
    int (*fn)(const char*, const char*) = REAL(symlink, int (*)(const char*, const char*));
    char target_buf[REDIR_BUF_SIZE];
    char link_buf[REDIR_BUF_SIZE];
    return fn(
        redirect_path(target, target_buf, sizeof(target_buf)),
        redirect_path(linkpath, link_buf, sizeof(link_buf))
    );
}

int symlinkat(const char* target, int dirfd, const char* linkpath) {
    int (*fn)(const char*, int, const char*) =
        REAL(symlinkat, int (*)(const char*, int, const char*));
    char target_buf[REDIR_BUF_SIZE];
    char link_buf[REDIR_BUF_SIZE];
    return fn(
        redirect_path(target, target_buf, sizeof(target_buf)),
        dirfd,
        redirect_at_path(dirfd, linkpath, link_buf, sizeof(link_buf))
    );
}

int link(const char* oldpath, const char* newpath) {
    int (*fn)(const char*, const char*) = REAL(link, int (*)(const char*, const char*));
    char old_buf[REDIR_BUF_SIZE];
    char new_buf[REDIR_BUF_SIZE];
    return fn(
        redirect_path(oldpath, old_buf, sizeof(old_buf)),
        redirect_path(newpath, new_buf, sizeof(new_buf))
    );
}

int linkat(int olddirfd, const char* oldpath, int newdirfd, const char* newpath, int flags) {
    int (*fn)(int, const char*, int, const char*, int) =
        REAL(linkat, int (*)(int, const char*, int, const char*, int));
    char old_buf[REDIR_BUF_SIZE];
    char new_buf[REDIR_BUF_SIZE];
    return fn(
        olddirfd,
        redirect_at_path(olddirfd, oldpath, old_buf, sizeof(old_buf)),
        newdirfd,
        redirect_at_path(newdirfd, newpath, new_buf, sizeof(new_buf)),
        flags
    );
}

int unlink(const char* pathname) {
    int (*fn)(const char*) = REAL(unlink, int (*)(const char*));
    char buf[REDIR_BUF_SIZE];
    return fn(redirect_path(pathname, buf, sizeof(buf)));
}

int unlinkat(int dirfd, const char* pathname, int flags) {
    int (*fn)(int, const char*, int) = REAL(unlinkat, int (*)(int, const char*, int));
    char buf[REDIR_BUF_SIZE];
    return fn(dirfd, redirect_at_path(dirfd, pathname, buf, sizeof(buf)), flags);
}

int remove(const char* pathname) {
    int (*fn)(const char*) = REAL(remove, int (*)(const char*));
    char buf[REDIR_BUF_SIZE];
    return fn(redirect_path(pathname, buf, sizeof(buf)));
}

int rename(const char* oldpath, const char* newpath) {
    int (*fn)(const char*, const char*) = REAL(rename, int (*)(const char*, const char*));
    char old_buf[REDIR_BUF_SIZE];
    char new_buf[REDIR_BUF_SIZE];
    return fn(
        redirect_path(oldpath, old_buf, sizeof(old_buf)),
        redirect_path(newpath, new_buf, sizeof(new_buf))
    );
}

int renameat(int olddirfd, const char* oldpath, int newdirfd, const char* newpath) {
    int (*fn)(int, const char*, int, const char*) =
        REAL(renameat, int (*)(int, const char*, int, const char*));
    char old_buf[REDIR_BUF_SIZE];
    char new_buf[REDIR_BUF_SIZE];
    return fn(
        olddirfd,
        redirect_at_path(olddirfd, oldpath, old_buf, sizeof(old_buf)),
        newdirfd,
        redirect_at_path(newdirfd, newpath, new_buf, sizeof(new_buf))
    );
}

#ifdef __linux__
int renameat2(int olddirfd, const char* oldpath, int newdirfd, const char* newpath, unsigned int flags) {
    int (*fn)(int, const char*, int, const char*, unsigned int) =
        OPT_REAL(renameat2, int (*)(int, const char*, int, const char*, unsigned int));
    char old_buf[REDIR_BUF_SIZE];
    char new_buf[REDIR_BUF_SIZE];

    oldpath = redirect_at_path(olddirfd, oldpath, old_buf, sizeof(old_buf));
    newpath = redirect_at_path(newdirfd, newpath, new_buf, sizeof(new_buf));

    if (fn) return fn(olddirfd, oldpath, newdirfd, newpath, flags);
    if (flags != 0) {
        errno = ENOSYS;
        return -1;
    }
    return renameat(olddirfd, oldpath, newdirfd, newpath);
}
#endif

/* ------------------------------------------------------------------------- */
/* canonical path wrappers                                                    */
/* ------------------------------------------------------------------------- */

char* realpath(const char* pathname, char* resolved_path) {
    char* (*fn)(const char*, char*) = REAL(realpath, char* (*)(const char*, char*));
    char buf[REDIR_BUF_SIZE];
    return fn(redirect_path(pathname, buf, sizeof(buf)), resolved_path);
}

#ifdef __GLIBC__
char* canonicalize_file_name(const char* pathname) {
    char* (*fn)(const char*) =
        REAL(canonicalize_file_name, char* (*)(const char*));
    char buf[REDIR_BUF_SIZE];
    return fn(redirect_path(pathname, buf, sizeof(buf)));
}
#endif

