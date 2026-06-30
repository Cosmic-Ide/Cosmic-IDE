#define _GNU_SOURCE
#define _LARGEFILE64_SOURCE

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dlfcn.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>
#include <stdarg.h>
#include <unistd.h>
#include <errno.h>
#include <dirent.h>

/*
 * tmpredir.c
 *
 * Redirects /tmp and /tmp/... to $TMPDIR.
 *
 * Also spoofs hsperfdata_* stat results enough for HotSpot perfMemory_posix.cpp
 * security checks when running in an app-private/custom TMPDIR environment.
 *
 * Build:
 *   gcc -shared -fPIC -O2 -Wall -Wextra -o libtmpredir.so tmpredir.c -ldl
 *
 * Run:
 *   export TMPDIR=/data/data/your.package/cache/tmp
 *   mkdir -p "$TMPDIR"
 *   chmod 700 "$TMPDIR"
 *   export LD_PRELOAD=/path/to/libtmpredir.so
 */

static const char* redirect_path(const char* path, char* buffer, size_t buf_len) {
    if (!path) return path;

    const char* tmpdir = getenv("TMPDIR");
    if (!tmpdir || !*tmpdir) return path;

    if (strcmp(path, "/tmp") == 0) {
        snprintf(buffer, buf_len, "%s", tmpdir);
        return buffer;
    }

    if (strncmp(path, "/tmp/", 5) == 0) {
        size_t len = strlen(tmpdir);

        if (len > 0 && tmpdir[len - 1] == '/') {
            snprintf(buffer, buf_len, "%s%s", tmpdir, path + 5);
        } else {
            snprintf(buffer, buf_len, "%s/%s", tmpdir, path + 5);
        }

        return buffer;
    }

    return path;
}

static void* must_dlsym(const char* name) {
    void* sym = dlsym(RTLD_NEXT, name);
    if (!sym) {
        fprintf(stderr, "tmpredir: failed to resolve symbol: %s\n", name);
        abort();
    }
    return sym;
}

static inline int should_redirect(int dirfd, const char* pathname) {
    return pathname && (pathname[0] == '/' || dirfd == AT_FDCWD);
}

static int is_perf_path2(const char* a, const char* b) {
    return (a && strstr(a, "hsperfdata_")) ||
           (b && strstr(b, "hsperfdata_"));
}

static int is_perf_path1(const char* a) {
    return a && strstr(a, "hsperfdata_");
}

static int open_needs_mode(int flags) {
#if defined(O_TMPFILE)
    return (flags & O_CREAT) || ((flags & O_TMPFILE) == O_TMPFILE);
#else
    return (flags & O_CREAT);
#endif
}

/*
 * Spoof only the fields HotSpot's perfMemory_posix.cpp security checks care about:
 *
 * - directory owner must match geteuid()
 * - directory must not be group/world writable
 * - perfdata file must not appear hard-linked
 *
 * Do not touch st_ino/st_dev/st_size/st_mtime etc.
 */
static void spoof_stat_if_perf_data(struct stat* statbuf) {
    if (!statbuf) return;

    statbuf->st_uid = geteuid();

    if (S_ISDIR(statbuf->st_mode)) {
        statbuf->st_mode &= ~(S_IWGRP | S_IWOTH);
    } else if (S_ISREG(statbuf->st_mode)) {
        statbuf->st_mode &= ~(S_IWGRP | S_IWOTH);
        statbuf->st_nlink = 1;
    }
}

#if defined(__USE_LARGEFILE64) || defined(_LARGEFILE64_SOURCE)
static void spoof_stat64_if_perf_data(struct stat64* statbuf) {
    if (!statbuf) return;

    statbuf->st_uid = geteuid();

    if (S_ISDIR(statbuf->st_mode)) {
        statbuf->st_mode &= ~(S_IWGRP | S_IWOTH);
    } else if (S_ISREG(statbuf->st_mode)) {
        statbuf->st_mode &= ~(S_IWGRP | S_IWOTH);
        statbuf->st_nlink = 1;
    }
}
#endif

static int fd_points_to_perf_data(int fd) {
    char proc_path[64];
    char link_path[4096];

    snprintf(proc_path, sizeof(proc_path), "/proc/self/fd/%d", fd);

    ssize_t n = readlink(proc_path, link_path, sizeof(link_path) - 1);
    if (n <= 0) {
        return 0;
    }

    link_path[n] = '\0';
    return is_perf_path1(link_path);
}

/* ---------------- directory functions ---------------- */

DIR* opendir(const char* name) {
    static DIR* (*real_opendir)(const char*) = NULL;
    if (!real_opendir) real_opendir = must_dlsym("opendir");

    char buf[4096];
    return real_opendir(redirect_path(name, buf, sizeof(buf)));
}

int mkdir(const char* pathname, mode_t mode) {
    static int (*real_mkdir)(const char*, mode_t) = NULL;
    if (!real_mkdir) real_mkdir = must_dlsym("mkdir");

    char buf[4096];
    return real_mkdir(redirect_path(pathname, buf, sizeof(buf)), mode);
}

int mkdirat(int dirfd, const char* pathname, mode_t mode) {
    static int (*real_mkdirat)(int, const char*, mode_t) = NULL;
    if (!real_mkdirat) real_mkdirat = must_dlsym("mkdirat");

    char buf[4096];

    if (should_redirect(dirfd, pathname)) {
        pathname = redirect_path(pathname, buf, sizeof(buf));
    }

    return real_mkdirat(dirfd, pathname, mode);
}

int rmdir(const char* pathname) {
    static int (*real_rmdir)(const char*) = NULL;
    if (!real_rmdir) real_rmdir = must_dlsym("rmdir");

    char buf[4096];
    return real_rmdir(redirect_path(pathname, buf, sizeof(buf)));
}

/* ---------------- open/create functions ---------------- */

int open(const char* pathname, int flags, ...) {
    static int (*real_open)(const char*, int, ...) = NULL;
    if (!real_open) real_open = must_dlsym("open");

    char buf[4096];
    const char* final_path = redirect_path(pathname, buf, sizeof(buf));

    if (open_needs_mode(flags)) {
        va_list args;
        va_start(args, flags);
        mode_t mode = va_arg(args, mode_t);
        va_end(args);

        return real_open(final_path, flags, mode);
    }

    return real_open(final_path, flags);
}

int open64(const char* pathname, int flags, ...) {
    static int (*real_open64)(const char*, int, ...) = NULL;
    if (!real_open64) real_open64 = must_dlsym("open64");

    char buf[4096];
    const char* final_path = redirect_path(pathname, buf, sizeof(buf));

    if (open_needs_mode(flags)) {
        va_list args;
        va_start(args, flags);
        mode_t mode = va_arg(args, mode_t);
        va_end(args);

        return real_open64(final_path, flags, mode);
    }

    return real_open64(final_path, flags);
}

int openat(int dirfd, const char* pathname, int flags, ...) {
    static int (*real_openat)(int, const char*, int, ...) = NULL;
    if (!real_openat) real_openat = must_dlsym("openat");

    char buf[4096];

    if (should_redirect(dirfd, pathname)) {
        pathname = redirect_path(pathname, buf, sizeof(buf));
    }

    if (open_needs_mode(flags)) {
        va_list args;
        va_start(args, flags);
        mode_t mode = va_arg(args, mode_t);
        va_end(args);

        return real_openat(dirfd, pathname, flags, mode);
    }

    return real_openat(dirfd, pathname, flags);
}

int openat64(int dirfd, const char* pathname, int flags, ...) {
    static int (*real_openat64)(int, const char*, int, ...) = NULL;
    if (!real_openat64) real_openat64 = must_dlsym("openat64");

    char buf[4096];

    if (should_redirect(dirfd, pathname)) {
        pathname = redirect_path(pathname, buf, sizeof(buf));
    }

    if (open_needs_mode(flags)) {
        va_list args;
        va_start(args, flags);
        mode_t mode = va_arg(args, mode_t);
        va_end(args);

        return real_openat64(dirfd, pathname, flags, mode);
    }

    return real_openat64(dirfd, pathname, flags);
}

int creat(const char* pathname, mode_t mode) {
    static int (*real_creat)(const char*, mode_t) = NULL;
    if (!real_creat) real_creat = must_dlsym("creat");

    char buf[4096];
    return real_creat(redirect_path(pathname, buf, sizeof(buf)), mode);
}

int creat64(const char* pathname, mode_t mode) {
    static int (*real_creat64)(const char*, mode_t) = NULL;
    if (!real_creat64) real_creat64 = must_dlsym("creat64");

    char buf[4096];
    return real_creat64(redirect_path(pathname, buf, sizeof(buf)), mode);
}

/* ---------------- stat functions ---------------- */

int stat(const char* pathname, struct stat* statbuf) {
    static int (*real_stat)(const char*, struct stat*) = NULL;
    if (!real_stat) real_stat = must_dlsym("stat");

    char buf[4096];
    const char* target_path = redirect_path(pathname, buf, sizeof(buf));

    int res = real_stat(target_path, statbuf);

    if (res == 0 && is_perf_path2(pathname, target_path)) {
        spoof_stat_if_perf_data(statbuf);
    }

    return res;
}

int lstat(const char* pathname, struct stat* statbuf) {
    static int (*real_lstat)(const char*, struct stat*) = NULL;
    if (!real_lstat) real_lstat = must_dlsym("lstat");

    char buf[4096];
    const char* target_path = redirect_path(pathname, buf, sizeof(buf));

    int res = real_lstat(target_path, statbuf);

    if (res == 0 && is_perf_path2(pathname, target_path)) {
        spoof_stat_if_perf_data(statbuf);
    }

    return res;
}

int fstat(int fd, struct stat* statbuf) {
    static int (*real_fstat)(int, struct stat*) = NULL;
    if (!real_fstat) real_fstat = must_dlsym("fstat");

    int res = real_fstat(fd, statbuf);

    if (res == 0 && fd_points_to_perf_data(fd)) {
        spoof_stat_if_perf_data(statbuf);
    }

    return res;
}

int fstatat(int dirfd, const char* pathname, struct stat* statbuf, int flags) {
    static int (*real_fstatat)(int, const char*, struct stat*, int) = NULL;
    if (!real_fstatat) real_fstatat = must_dlsym("fstatat");

    char buf[4096];
    const char* target_path = pathname;

    if (should_redirect(dirfd, pathname)) {
        target_path = redirect_path(pathname, buf, sizeof(buf));
    }

    int res = real_fstatat(dirfd, target_path, statbuf, flags);

    if (res == 0 && is_perf_path2(pathname, target_path)) {
        spoof_stat_if_perf_data(statbuf);
    }

    return res;
}

#if defined(__USE_LARGEFILE64) || defined(_LARGEFILE64_SOURCE)

int stat64(const char* pathname, struct stat64* statbuf) {
    static int (*real_stat64)(const char*, struct stat64*) = NULL;
    if (!real_stat64) real_stat64 = must_dlsym("stat64");

    char buf[4096];
    const char* target_path = redirect_path(pathname, buf, sizeof(buf));

    int res = real_stat64(target_path, statbuf);

    if (res == 0 && is_perf_path2(pathname, target_path)) {
        spoof_stat64_if_perf_data(statbuf);
    }

    return res;
}

int lstat64(const char* pathname, struct stat64* statbuf) {
    static int (*real_lstat64)(const char*, struct stat64*) = NULL;
    if (!real_lstat64) real_lstat64 = must_dlsym("lstat64");

    char buf[4096];
    const char* target_path = redirect_path(pathname, buf, sizeof(buf));

    int res = real_lstat64(target_path, statbuf);

    if (res == 0 && is_perf_path2(pathname, target_path)) {
        spoof_stat64_if_perf_data(statbuf);
    }

    return res;
}

int fstat64(int fd, struct stat64* statbuf) {
    static int (*real_fstat64)(int, struct stat64*) = NULL;
    if (!real_fstat64) real_fstat64 = must_dlsym("fstat64");

    int res = real_fstat64(fd, statbuf);

    if (res == 0 && fd_points_to_perf_data(fd)) {
        spoof_stat64_if_perf_data(statbuf);
    }

    return res;
}

int fstatat64(int dirfd, const char* pathname, struct stat64* statbuf, int flags) {
    static int (*real_fstatat64)(int, const char*, struct stat64*, int) = NULL;
    if (!real_fstatat64) real_fstatat64 = must_dlsym("fstatat64");

    char buf[4096];
    const char* target_path = pathname;

    if (should_redirect(dirfd, pathname)) {
        target_path = redirect_path(pathname, buf, sizeof(buf));
    }

    int res = real_fstatat64(dirfd, target_path, statbuf, flags);

    if (res == 0 && is_perf_path2(pathname, target_path)) {
        spoof_stat64_if_perf_data(statbuf);
    }

    return res;
}

#endif

/*
 * Legacy glibc stat entry points.
 * Some old binaries or compatibility builds may call these directly.
 */

int __xstat(int ver, const char* pathname, struct stat* statbuf) {
    static int (*real___xstat)(int, const char*, struct stat*) = NULL;
    if (!real___xstat) real___xstat = must_dlsym("__xstat");

    char buf[4096];
    const char* target_path = redirect_path(pathname, buf, sizeof(buf));

    int res = real___xstat(ver, target_path, statbuf);

    if (res == 0 && is_perf_path2(pathname, target_path)) {
        spoof_stat_if_perf_data(statbuf);
    }

    return res;
}

int __lxstat(int ver, const char* pathname, struct stat* statbuf) {
    static int (*real___lxstat)(int, const char*, struct stat*) = NULL;
    if (!real___lxstat) real___lxstat = must_dlsym("__lxstat");

    char buf[4096];
    const char* target_path = redirect_path(pathname, buf, sizeof(buf));

    int res = real___lxstat(ver, target_path, statbuf);

    if (res == 0 && is_perf_path2(pathname, target_path)) {
        spoof_stat_if_perf_data(statbuf);
    }

    return res;
}

int __fxstat(int ver, int fd, struct stat* statbuf) {
    static int (*real___fxstat)(int, int, struct stat*) = NULL;
    if (!real___fxstat) real___fxstat = must_dlsym("__fxstat");

    int res = real___fxstat(ver, fd, statbuf);

    if (res == 0 && fd_points_to_perf_data(fd)) {
        spoof_stat_if_perf_data(statbuf);
    }

    return res;
}

int __fxstatat(int ver, int dirfd, const char* pathname, struct stat* statbuf, int flags) {
    static int (*real___fxstatat)(int, int, const char*, struct stat*, int) = NULL;
    if (!real___fxstatat) real___fxstatat = must_dlsym("__fxstatat");

    char buf[4096];
    const char* target_path = pathname;

    if (should_redirect(dirfd, pathname)) {
        target_path = redirect_path(pathname, buf, sizeof(buf));
    }

    int res = real___fxstatat(ver, dirfd, target_path, statbuf, flags);

    if (res == 0 && is_perf_path2(pathname, target_path)) {
        spoof_stat_if_perf_data(statbuf);
    }

    return res;
}

#if defined(__USE_LARGEFILE64) || defined(_LARGEFILE64_SOURCE)

int __xstat64(int ver, const char* pathname, struct stat64* statbuf) {
    static int (*real___xstat64)(int, const char*, struct stat64*) = NULL;
    if (!real___xstat64) real___xstat64 = must_dlsym("__xstat64");

    char buf[4096];
    const char* target_path = redirect_path(pathname, buf, sizeof(buf));

    int res = real___xstat64(ver, target_path, statbuf);

    if (res == 0 && is_perf_path2(pathname, target_path)) {
        spoof_stat64_if_perf_data(statbuf);
    }

    return res;
}

int __lxstat64(int ver, const char* pathname, struct stat64* statbuf) {
    static int (*real___lxstat64)(int, const char*, struct stat64*) = NULL;
    if (!real___lxstat64) real___lxstat64 = must_dlsym("__lxstat64");

    char buf[4096];
    const char* target_path = redirect_path(pathname, buf, sizeof(buf));

    int res = real___lxstat64(ver, target_path, statbuf);

    if (res == 0 && is_perf_path2(pathname, target_path)) {
        spoof_stat64_if_perf_data(statbuf);
    }

    return res;
}

int __fxstat64(int ver, int fd, struct stat64* statbuf) {
    static int (*real___fxstat64)(int, int, struct stat64*) = NULL;
    if (!real___fxstat64) real___fxstat64 = must_dlsym("__fxstat64");

    int res = real___fxstat64(ver, fd, statbuf);

    if (res == 0 && fd_points_to_perf_data(fd)) {
        spoof_stat64_if_perf_data(statbuf);
    }

    return res;
}

int __fxstatat64(int ver, int dirfd, const char* pathname, struct stat64* statbuf, int flags) {
    static int (*real___fxstatat64)(int, int, const char*, struct stat64*, int) = NULL;
    if (!real___fxstatat64) real___fxstatat64 = must_dlsym("__fxstatat64");

    char buf[4096];
    const char* target_path = pathname;

    if (should_redirect(dirfd, pathname)) {
        target_path = redirect_path(pathname, buf, sizeof(buf));
    }

    int res = real___fxstatat64(ver, dirfd, target_path, statbuf, flags);

    if (res == 0 && is_perf_path2(pathname, target_path)) {
        spoof_stat64_if_perf_data(statbuf);
    }

    return res;
}

#endif

/* ---------------- access functions ---------------- */

int access(const char* pathname, int mode) {
    static int (*real_access)(const char*, int) = NULL;
    if (!real_access) real_access = must_dlsym("access");

    char buf[4096];
    return real_access(redirect_path(pathname, buf, sizeof(buf)), mode);
}

int faccessat(int dirfd, const char* pathname, int mode, int flags) {
    static int (*real_faccessat)(int, const char*, int, int) = NULL;
    if (!real_faccessat) real_faccessat = must_dlsym("faccessat");

    char buf[4096];

    if (should_redirect(dirfd, pathname)) {
        pathname = redirect_path(pathname, buf, sizeof(buf));
    }

    return real_faccessat(dirfd, pathname, mode, flags);
}

/* ---------------- remove/rename functions ---------------- */

int unlink(const char* pathname) {
    static int (*real_unlink)(const char*) = NULL;
    if (!real_unlink) real_unlink = must_dlsym("unlink");

    char buf[4096];
    return real_unlink(redirect_path(pathname, buf, sizeof(buf)));
}

int unlinkat(int dirfd, const char* pathname, int flags) {
    static int (*real_unlinkat)(int, const char*, int) = NULL;
    if (!real_unlinkat) real_unlinkat = must_dlsym("unlinkat");

    char buf[4096];

    if (should_redirect(dirfd, pathname)) {
        pathname = redirect_path(pathname, buf, sizeof(buf));
    }

    return real_unlinkat(dirfd, pathname, flags);
}

int remove(const char* pathname) {
    static int (*real_remove)(const char*) = NULL;
    if (!real_remove) real_remove = must_dlsym("remove");

    char buf[4096];
    return real_remove(redirect_path(pathname, buf, sizeof(buf)));
}

int rename(const char* oldpath, const char* newpath) {
    static int (*real_rename)(const char*, const char*) = NULL;
    if (!real_rename) real_rename = must_dlsym("rename");

    char oldbuf[4096];
    char newbuf[4096];

    return real_rename(
        redirect_path(oldpath, oldbuf, sizeof(oldbuf)),
        redirect_path(newpath, newbuf, sizeof(newbuf))
    );
}

int renameat(int olddirfd, const char* oldpath, int newdirfd, const char* newpath) {
    static int (*real_renameat)(int, const char*, int, const char*) = NULL;
    if (!real_renameat) real_renameat = must_dlsym("renameat");

    char oldbuf[4096];
    char newbuf[4096];

    if (should_redirect(olddirfd, oldpath)) {
        oldpath = redirect_path(oldpath, oldbuf, sizeof(oldbuf));
    }

    if (should_redirect(newdirfd, newpath)) {
        newpath = redirect_path(newpath, newbuf, sizeof(newbuf));
    }

    return real_renameat(olddirfd, oldpath, newdirfd, newpath);
}

#ifdef __linux__
int renameat2(int olddirfd, const char* oldpath, int newdirfd, const char* newpath, unsigned int flags) {
    static int (*real_renameat2)(int, const char*, int, const char*, unsigned int) = NULL;
    if (!real_renameat2) real_renameat2 = must_dlsym("renameat2");

    char oldbuf[4096];
    char newbuf[4096];

    if (should_redirect(olddirfd, oldpath)) {
        oldpath = redirect_path(oldpath, oldbuf, sizeof(oldbuf));
    }

    if (should_redirect(newdirfd, newpath)) {
        newpath = redirect_path(newpath, newbuf, sizeof(newbuf));
    }

    return real_renameat2(olddirfd, oldpath, newdirfd, newpath, flags);
}
#endif