#define _GNU_SOURCE

#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <signal.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <sys/sysmacros.h>
#include <time.h>
#include <ucontext.h>

/*
 * Android's app seccomp policy traps a number of newer Linux system calls.
 * Returning ENOSYS lets well-behaved callers select their documented legacy
 * fallback instead of terminating on SIGSYS. In particular, Go's Faccessat
 * implementation retries with faccessat(2) after faccessat2(2) returns
 * ENOSYS.
 */

#if defined(__aarch64__)
#define REG_ARG0 uc_mcontext.regs[0]
#define REG_ARG1 uc_mcontext.regs[1]
#define REG_ARG2 uc_mcontext.regs[2]
#define REG_ARG3 uc_mcontext.regs[3]
#define REG_ARG4 uc_mcontext.regs[4]
#define REG_RET uc_mcontext.regs[0]
#elif defined(__x86_64__)
#define REG_ARG0 uc_mcontext.gregs[REG_RDI]
#define REG_ARG1 uc_mcontext.gregs[REG_RSI]
#define REG_ARG2 uc_mcontext.gregs[REG_RDX]
#define REG_ARG3 uc_mcontext.gregs[REG_R10]
#define REG_ARG4 uc_mcontext.gregs[REG_R8]
#define REG_RET uc_mcontext.gregs[REG_RAX]
#else
#error "Unsupported architecture for SIGSYS syscall trapping"
#endif

/* Go installs its own SIGSYS handler after preload constructors run. */
#define SIGSYS_HANDOFF_ATTEMPTS 5000U
#define SIGSYS_HANDOFF_INTERVAL_NS 1000000L

static long emulate_statx(
    int dirfd,
    const char *pathname,
    int flags,
    unsigned int mask,
    struct statx *result
) {
    if (result == NULL) return -EFAULT;
    (void)mask;

    struct stat stat_result;
    int fstatat_flags = flags & (AT_SYMLINK_NOFOLLOW | AT_NO_AUTOMOUNT | AT_EMPTY_PATH);
    if (fstatat(dirfd, pathname, &stat_result, fstatat_flags) != 0) {
        return -errno;
    }

    memset(result, 0, sizeof(*result));
    result->stx_mask = STATX_BASIC_STATS;
    result->stx_blksize = stat_result.st_blksize;
    result->stx_nlink = stat_result.st_nlink;
    result->stx_uid = stat_result.st_uid;
    result->stx_gid = stat_result.st_gid;
    result->stx_mode = stat_result.st_mode;
    result->stx_ino = stat_result.st_ino;
    result->stx_size = stat_result.st_size;
    result->stx_blocks = stat_result.st_blocks;
    result->stx_atime.tv_sec = stat_result.st_atim.tv_sec;
    result->stx_atime.tv_nsec = stat_result.st_atim.tv_nsec;
    result->stx_ctime.tv_sec = stat_result.st_ctim.tv_sec;
    result->stx_ctime.tv_nsec = stat_result.st_ctim.tv_nsec;
    result->stx_mtime.tv_sec = stat_result.st_mtim.tv_sec;
    result->stx_mtime.tv_nsec = stat_result.st_mtim.tv_nsec;
    result->stx_rdev_major = major(stat_result.st_rdev);
    result->stx_rdev_minor = minor(stat_result.st_rdev);
    result->stx_dev_major = major(stat_result.st_dev);
    result->stx_dev_minor = minor(stat_result.st_dev);

    return 0;
}

static void sigsys_handler(int signal_number, siginfo_t *info, void *uctx_raw) {
    (void)signal_number;

    if (info == NULL || info->si_code != SYS_SECCOMP || uctx_raw == NULL) {
        return;
    }

    /*
     * SECCOMP_RET_TRAP has already advanced the saved PC past the syscall.
     * Only replace its result; advancing the PC again would skip an instruction.
     */
    ucontext_t *ctx = (ucontext_t *)uctx_raw;

#ifdef SYS_statx
    if (info->si_syscall == SYS_statx) {
        ctx->REG_RET = emulate_statx(
            (int)ctx->REG_ARG0,
            (const char *)ctx->REG_ARG1,
            (int)ctx->REG_ARG2,
            (unsigned int)ctx->REG_ARG3,
            (struct statx *)ctx->REG_ARG4
        );
        return;
    }
#endif

    ctx->REG_RET = -ENOSYS;
}

static void install_sigsys_handler(void) {
    struct sigaction current;
    if (sigaction(SIGSYS, NULL, &current) != 0) return;

    if ((current.sa_flags & SA_SIGINFO) != 0 &&
        current.sa_sigaction == sigsys_handler) {
        return;
    }

    struct sigaction action;
    memset(&action, 0, sizeof(action));
    action.sa_sigaction = sigsys_handler;
    action.sa_flags = SA_SIGINFO;
    sigemptyset(&action.sa_mask);
    (void)sigaction(SIGSYS, &action, NULL);
}

static void *maintain_sigsys_handler(void *unused) {
    (void)unused;

    const struct timespec interval = {
        .tv_sec = 0,
        .tv_nsec = SIGSYS_HANDOFF_INTERVAL_NS,
    };

    for (unsigned int attempt = 0; attempt < SIGSYS_HANDOFF_ATTEMPTS; ++attempt) {
        install_sigsys_handler();
        (void)nanosleep(&interval, NULL);
    }

    return NULL;
}

__attribute__((constructor))
static void init_syscall_interposer(void) {
    install_sigsys_handler();

    pthread_t thread;
    if (pthread_create(&thread, NULL, maintain_sigsys_handler, NULL) == 0) {
        (void)pthread_detach(thread);
    }
}
