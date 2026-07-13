#define _GNU_SOURCE

#include "pacman_fake_root.h"

#include <dlfcn.h>
#include <elf.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include <sys/auxv.h>
#include <sys/types.h>
#include <unistd.h>

static int pacman_process = -1;

static bool is_pacman_name(const char *path) {
    if (path == NULL || path[0] == '\0') {
        return false;
    }

    const char *name = strrchr(path, '/');
    name = name != NULL ? name + 1 : path;

    return strcmp(name, "pacman") == 0 ||
           strcmp(name, "pacman-key") == 0;
}

static bool cmdline_contains_pacman(void) {
    int fd = open("/proc/self/cmdline", O_RDONLY | O_CLOEXEC);

    if (fd < 0) {
        return false;
    }

    char buffer[8192];
    ssize_t length = read(fd, buffer, sizeof(buffer) - 1);
    close(fd);

    if (length <= 0) {
        return false;
    }

    buffer[length] = '\0';

    size_t offset = 0;

    while (offset < (size_t)length) {
        const char *argument = buffer + offset;
        size_t remaining = (size_t)length - offset;
        size_t argument_length = strnlen(argument, remaining);

        if (argument_length == remaining) {
            break;
        }

        if (is_pacman_name(argument)) {
            return true;
        }

        offset += argument_length + 1;
    }

    return false;
}

bool pacman_fake_root_is_pacman(void) {
    if (pacman_process != -1) {
        return pacman_process == 1;
    }

    /*
     * For a script launched through a shebang, /proc/self/exe points to
     * the interpreter, while AT_EXECFN normally retains the path that
     * was originally executed.
     */
    errno = 0;

    const char *execfn =
        (const char *)(uintptr_t)getauxval(AT_EXECFN);

    if (execfn != NULL && is_pacman_name(execfn)) {
        pacman_process = 1;
        return true;
    }

    /*
     * This handles:
     *
     *   bash /usr/bin/pacman-key ...
     *   sh /usr/bin/pacman-key ...
     *
     * where AT_EXECFN identifies bash/sh but the script path remains in
     * the process command line.
     */
    if (cmdline_contains_pacman()) {
        pacman_process = 1;
        return true;
    }

    /*
     * Normal ELF executable fallback for pacman itself.
     */
    char path[PATH_MAX];
    ssize_t length =
        readlink("/proc/self/exe", path, sizeof(path) - 1);

    if (length >= 0) {
        path[length] = '\0';

        if (is_pacman_name(path)) {
            pacman_process = 1;
            return true;
        }
    }

    pacman_process = 0;
    return false;
}

uid_t getuid(void) {
    if (pacman_fake_root_is_pacman()) {
        return 0;
    }

    static uid_t (*real_getuid)(void);

    if (real_getuid == NULL) {
        real_getuid = dlsym(RTLD_NEXT, "getuid");
    }

    if (real_getuid == NULL) {
        errno = ENOSYS;
        return (uid_t)-1;
    }

    return real_getuid();
}

uid_t geteuid(void) {
    if (pacman_fake_root_is_pacman()) {
        return 0;
    }

    static uid_t (*real_geteuid)(void);

    if (real_geteuid == NULL) {
        real_geteuid = dlsym(RTLD_NEXT, "geteuid");
    }

    if (real_geteuid == NULL) {
        errno = ENOSYS;
        return (uid_t)-1;
    }

    return real_geteuid();
}

gid_t getgid(void) {
    if (pacman_fake_root_is_pacman()) {
        return 0;
    }

    static gid_t (*real_getgid)(void);

    if (real_getgid == NULL) {
        real_getgid = dlsym(RTLD_NEXT, "getgid");
    }

    if (real_getgid == NULL) {
        errno = ENOSYS;
        return (gid_t)-1;
    }

    return real_getgid();
}

gid_t getegid(void) {
    if (pacman_fake_root_is_pacman()) {
        return 0;
    }

    static gid_t (*real_getegid)(void);

    if (real_getegid == NULL) {
        real_getegid = dlsym(RTLD_NEXT, "getegid");
    }

    if (real_getegid == NULL) {
        errno = ENOSYS;
        return (gid_t)-1;
    }

    return real_getegid();
}

int getresuid(uid_t *ruid, uid_t *euid, uid_t *suid) {
    if (pacman_fake_root_is_pacman()) {
        if (ruid != NULL) {
            *ruid = 0;
        }

        if (euid != NULL) {
            *euid = 0;
        }

        if (suid != NULL) {
            *suid = 0;
        }

        return 0;
    }

    static int (*real_getresuid)(uid_t *, uid_t *, uid_t *);

    if (real_getresuid == NULL) {
        real_getresuid = dlsym(RTLD_NEXT, "getresuid");
    }

    if (real_getresuid == NULL) {
        errno = ENOSYS;
        return -1;
    }

    return real_getresuid(ruid, euid, suid);
}

int getresgid(gid_t *rgid, gid_t *egid, gid_t *sgid) {
    if (pacman_fake_root_is_pacman()) {
        if (rgid != NULL) {
            *rgid = 0;
        }

        if (egid != NULL) {
            *egid = 0;
        }

        if (sgid != NULL) {
            *sgid = 0;
        }

        return 0;
    }

    static int (*real_getresgid)(gid_t *, gid_t *, gid_t *);

    if (real_getresgid == NULL) {
        real_getresgid = dlsym(RTLD_NEXT, "getresgid");
    }

    if (real_getresgid == NULL) {
        errno = ENOSYS;
        return -1;
    }

    return real_getresgid(rgid, egid, sgid);
}