/*
 * PTY Native Implementation for Cosmic IDE
 * Linux-specific pseudo-terminal handling with Termux-style process spawning
 * 
 * CRITICAL DESIGN:
 * - Native layer handles all PTY management, including slave FD lifecycle
 * - Parent process closes slave FD immediately after fork/exec
 * - Only return master_fd and child_pid to Java
 * - Use blocking I/O on master (no O_NONBLOCK)
 * - Process group signal delivery for SIGINT, SIGTERM
 */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <signal.h>
#include <errno.h>
#include <string.h>
#include <termios.h>
#include <android/log.h>

#ifdef __ANDROID__
#include <pty.h>
#else
#include <pty.h>
#endif

#include "pty_native.h"

#define PTY_BUFFER_SIZE 4096
#define PTY_PATH_MAX 256
#define LOG_TAG "CosmicPtyNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static void throw_out_of_memory(JNIEnv *env, const char *message) {
    jclass clazz = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
    if (clazz != NULL) {
        (*env)->ThrowNew(env, clazz, message);
    }
}

static void throw_io_exception(JNIEnv *env, const char *message) {
    jclass clazz = (*env)->FindClass(env, "java/io/IOException");
    if (clazz != NULL) {
        (*env)->ThrowNew(env, clazz, message);
    }
}

static char *duplicate_jstring(JNIEnv *env, jstring value, const char *error_message) {
    if (value == NULL) {
        return NULL;
    }

    const char *utf = (*env)->GetStringUTFChars(env, value, NULL);
    if (utf == NULL) {
        throw_out_of_memory(env, error_message);
        return NULL;
    }

    char *copy = strdup(utf);
    (*env)->ReleaseStringUTFChars(env, value, utf);

    if (copy == NULL) {
        throw_out_of_memory(env, error_message);
    }

    return copy;
}

static void free_string_array(char **values, jsize count) {
    if (values == NULL) {
        return;
    }

    for (jsize i = 0; i < count; i++) {
        free(values[i]);
    }

    free(values);
}

static void configure_terminal_defaults(int fd) {
    struct termios term;
    if (tcgetattr(fd, &term) == -1) {
        LOGE("tcgetattr failed fd=%d errno=%d", fd, errno);
        return;
    }

    term.c_lflag |= (ISIG | ICANON | ECHO | ECHOE | ECHOK | IEXTEN);
    term.c_iflag |= (BRKINT | ICRNL | IXON);
    term.c_iflag &= ~(IGNBRK | INLCR | IGNCR);
    term.c_oflag |= (OPOST | ONLCR);
    term.c_cflag = (term.c_cflag & ~CSIZE) | CREAD | CS8;
    term.c_cc[VINTR] = 3;   // Ctrl+C
    term.c_cc[VQUIT] = 28;  // Ctrl+backslash
    term.c_cc[VERASE] = 127;
    term.c_cc[VKILL] = 21;  // Ctrl+U
    term.c_cc[VEOF] = 4;    // Ctrl+D
#ifdef VSUSP
    term.c_cc[VSUSP] = 26;  // Ctrl+Z
#endif

    if (tcsetattr(fd, TCSANOW, &term) == -1) {
        LOGE("tcsetattr failed fd=%d errno=%d", fd, errno);
    } else {
        LOGD("configured terminal defaults fd=%d", fd);
    }
}

static void reset_child_signal_handlers(void) {
    signal(SIGINT, SIG_DFL);
    signal(SIGQUIT, SIG_DFL);
    signal(SIGTERM, SIG_DFL);
    signal(SIGHUP, SIG_DFL);
    signal(SIGPIPE, SIG_DFL);
#ifdef SIGTSTP
    signal(SIGTSTP, SIG_DFL);
#endif
#ifdef SIGTTIN
    signal(SIGTTIN, SIG_DFL);
#endif
#ifdef SIGTTOU
    signal(SIGTTOU, SIG_DFL);
#endif
}

static jboolean signal_process(pid_t pid, int signal) {
    if (pid <= 0) {
        return JNI_FALSE;
    }

    if (kill(-pid, signal) != -1) {
        LOGD("signal process group pid=%d signal=%d", pid, signal);
        return JNI_TRUE;
    }

    if (kill(pid, signal) != -1) {
        LOGD("signal process pid=%d signal=%d", pid, signal);
        return JNI_TRUE;
    }

    LOGE("signal process failed pid=%d signal=%d errno=%d", pid, signal, errno);
    return JNI_FALSE;
}

static jboolean signal_descendant_processes(pid_t pid, int signal, int depth) {
    if (pid <= 0 || depth > 32) {
        return JNI_FALSE;
    }

    char children_path[128];
    snprintf(children_path, sizeof(children_path), "/proc/%d/task/%d/children", pid, pid);

    FILE *children = fopen(children_path, "r");
    if (children == NULL) {
        LOGD("children open failed pid=%d path=%s errno=%d", pid, children_path, errno);
        return JNI_FALSE;
    }

    jboolean delivered = JNI_FALSE;
    pid_t child_pid;
    while (fscanf(children, "%d", &child_pid) == 1) {
        LOGD("descendant child parent=%d child=%d depth=%d", pid, child_pid, depth);
        if (signal_descendant_processes(child_pid, signal, depth + 1)) {
            delivered = JNI_TRUE;
        }
        if (signal_process(child_pid, signal)) {
            delivered = JNI_TRUE;
        }
    }

    fclose(children);
    return delivered;
}

static char **duplicate_jstring_array(JNIEnv *env, jobjectArray array, jsize *out_count,
                                      const char *error_message) {
    *out_count = 0;

    if (array == NULL) {
        return NULL;
    }

    jsize count = (*env)->GetArrayLength(env, array);
    char **values = calloc((size_t) count + 1, sizeof(char *));
    if (values == NULL) {
        throw_out_of_memory(env, error_message);
        return NULL;
    }

    for (jsize i = 0; i < count; i++) {
        jstring item = (jstring) (*env)->GetObjectArrayElement(env, array, i);
        if ((*env)->ExceptionCheck(env)) {
            free_string_array(values, count);
            return NULL;
        }

        if (item != NULL) {
            values[i] = duplicate_jstring(env, item, error_message);
            (*env)->DeleteLocalRef(env, item);

            if (values[i] == NULL) {
                free_string_array(values, count);
                return NULL;
            }
        }
    }

    values[count] = NULL;
    *out_count = count;
    return values;
}

/**
 * Spawns a child process inside a new PTY (Termux-style fork/exec)
 * 
 * CRITICAL: All JNI conversions happen BEFORE fork() to avoid unsafe JNI calls in child.
 * Child process does ONLY raw POSIX syscalls after fork().
 * 
 * Returns: [master_fd, child_pid] on success, or throws exception
 */
JNIEXPORT jintArray JNICALL
Java_org_cosmicide_exec_linux_PtyTerminalKt_nativeSpawnInPty(JNIEnv *env, jclass clazz,
                                                              jstring working_dir,
                                                              jstring exe_path,
                                                              jobjectArray argv,
                                                              jobjectArray env_vars) {
    // Convert all Java strings and arrays into owned C strings BEFORE fork().
    LOGD("nativeSpawnInPty begin");
    char *work_dir = duplicate_jstring(env, working_dir, "Failed to convert working_dir");
    if (work_dir == NULL) {
        return NULL;
    }

    char *exe = duplicate_jstring(env, exe_path, "Failed to convert exe_path");
    if (exe == NULL) {
        free(work_dir);
        return NULL;
    }

    jsize argv_count = 0;
    char **c_argv = duplicate_jstring_array(env, argv, &argv_count, "Failed to convert argv");
    if (c_argv == NULL) {
        free(exe);
        free(work_dir);
        return NULL;
    }

    jsize env_count = 0;
    char **c_env = duplicate_jstring_array(env, env_vars, &env_count, "Failed to convert env");
    if (env_vars != NULL && c_env == NULL) {
        free_string_array(c_argv, argv_count);
        free(exe);
        free(work_dir);
        return NULL;
    }
    
    // NOW: allocate PTY pair
    int master_fd, slave_fd;
    char slave_name[PTY_PATH_MAX];
    
    if (openpty(&master_fd, &slave_fd, slave_name, NULL, NULL) == -1) {
        int error = errno;
        LOGE("openpty failed errno=%d", error);
        free_string_array(c_env, env_count);
        free_string_array(c_argv, argv_count);
        free(exe);
        free(work_dir);

        throw_io_exception(env, strerror(error));
        return NULL;
    }
    LOGD("openpty success master_fd=%d slave_fd=%d slave=%s", master_fd, slave_fd, slave_name);
    
    // Master FD: Keep BLOCKING for proper terminal I/O
    int flags = fcntl(master_fd, F_GETFL);
    fcntl(master_fd, F_SETFL, flags & ~O_NONBLOCK);

    configure_terminal_defaults(slave_fd);
    
    // NOW: fork (child will have copies of all converted C strings)
    pid_t pid = fork();
    
    if (pid == -1) {
        int error = errno;
        LOGE("fork failed errno=%d", error);
        close(master_fd);
        close(slave_fd);

        free_string_array(c_env, env_count);
        free_string_array(c_argv, argv_count);
        free(exe);
        free(work_dir);

        throw_io_exception(env, strerror(error));
        return NULL;
    }
    
    if (pid > 0) {
        // Parent process: cleanup owned C strings and return
        close(slave_fd);
        LOGD("fork parent child_pid=%d master_fd=%d", pid, master_fd);

        free_string_array(c_env, env_count);
        free_string_array(c_argv, argv_count);
        free(exe);
        free(work_dir);

        jintArray result = (*env)->NewIntArray(env, 2);
        if (result == NULL) {
            close(master_fd);
            return NULL;
        }
        
        jint values[2];
        values[0] = master_fd;
        values[1] = (jint)pid;
        
        (*env)->SetIntArrayRegion(env, result, 0, 2, values);
        return result;
    }
    
    // Child process (pid == 0): ONLY raw POSIX syscalls from here on
    // No JNI, no malloc (except what's already allocated)
    // No stdio (it will be redirected)
    
    // Close master in child (we only need slave)
    close(master_fd);
    
    // Change to working directory
    if (work_dir && work_dir[0] != '\0') {
        if (chdir(work_dir) == -1) {
            LOGE("child chdir failed errno=%d", errno);
            _exit(1);
        }
    }
    
    // Create new session (process group leader)
    if (setsid() == -1) {
        LOGE("child setsid failed errno=%d", errno);
        _exit(1);
    }
    
    // Make slave the controlling terminal
    if (ioctl(slave_fd, TIOCSCTTY, NULL) == -1) {
        LOGE("child TIOCSCTTY failed errno=%d", errno);
        _exit(1);
    }

    // setsid() makes the child a process group leader. Mark that group as the
    // foreground process group for this PTY so VINTR (^C), VSUSP (^Z), etc. are
    // delivered by the terminal driver to the currently foreground job.
    if (tcsetpgrp(slave_fd, getpid()) == -1) {
        LOGE("child tcsetpgrp failed errno=%d", errno);
        _exit(1);
    }
    LOGD("child pty ready pid=%d slave_fd=%d", getpid(), slave_fd);

    reset_child_signal_handlers();
    
    // Redirect stdio to slave PTY
    if (dup2(slave_fd, STDIN_FILENO) == -1 ||
        dup2(slave_fd, STDOUT_FILENO) == -1 ||
        dup2(slave_fd, STDERR_FILENO) == -1) {
        LOGE("child dup2 failed errno=%d", errno);
        _exit(1);
    }
    
    // Close original slave fd (now duplicated as 0,1,2)
    if (slave_fd > 2) {
        close(slave_fd);
    }
    
    // Exec the program (replaces process image, no cleanup needed)
    if (c_env) {
        LOGD("child execve exe=%s", exe);
        execve(exe, c_argv, c_env);
    } else {
        LOGD("child execv exe=%s", exe);
        execv(exe, c_argv);
    }
    
    // If we get here, exec failed - don't exit(), use _exit() to avoid cleanup handlers
    LOGE("child exec failed exe=%s errno=%d", exe, errno);
    _exit(127);
}

/**
 * Closes PTY master file descriptor
 * 
 * NOTE: Slave FD is closed in parent after fork - never managed by Java layer.
 * This only closes the master FD that Java owns.
 */
JNIEXPORT jboolean JNICALL
Java_org_cosmicide_exec_linux_PtyTerminalKt_nativeClosePty(JNIEnv *env, jclass clazz,
                                                            jint master_fd) {
    if (master_fd < 0) {
        return JNI_FALSE;
    }

    if (close(master_fd) == -1) {
        LOGE("close pty failed fd=%d errno=%d", master_fd, errno);
        return JNI_FALSE;
    }

    LOGD("close pty fd=%d", master_fd);
    return JNI_TRUE;
}

/**
 * Sets terminal window size using TIOCSWINSZ
 */
JNIEXPORT jboolean JNICALL
Java_org_cosmicide_exec_linux_PtyTerminalKt_nativeSetWindowSize(JNIEnv *env, jclass clazz,
                                                                jint master_fd, jint rows,
                                                                jint columns) {
    if (master_fd < 0 || rows <= 0 || columns <= 0) {
        return JNI_FALSE;
    }

    struct winsize ws;
    ws.ws_row = rows;
    ws.ws_col = columns;
    ws.ws_xpixel = 0;
    ws.ws_ypixel = 0;

    if (ioctl(master_fd, TIOCSWINSZ, &ws) == -1) {
        LOGE("setWindowSize failed fd=%d rows=%d columns=%d errno=%d", master_fd, rows, columns, errno);
        return JNI_FALSE;
    }
    LOGD("setWindowSize fd=%d rows=%d columns=%d", master_fd, rows, columns);
    return JNI_TRUE;
}

/**
 * Sends a signal to the current foreground process group for the PTY.
 *
 * This is different from always signaling the original shell PID. Interactive
 * shells move foreground jobs into their own process groups; Ctrl+C must target
 * that current foreground group.
 */
JNIEXPORT jboolean JNICALL
Java_org_cosmicide_exec_linux_PtyTerminalKt_nativeSendSignalToForegroundProcessGroup(
        JNIEnv *env, jclass clazz, jint master_fd, jint fallback_pid, jint signal) {
    if (master_fd < 0 || signal <= 0) {
        return JNI_FALSE;
    }

    pid_t foreground_pgrp = -1;
    if (ioctl(master_fd, TIOCGPGRP, &foreground_pgrp) != -1 && foreground_pgrp > 0) {
        LOGD("foreground pgrp fd=%d pgrp=%d signal=%d", master_fd, foreground_pgrp, signal);
        if (kill(-foreground_pgrp, signal) != -1) {
            LOGD("foreground pgrp signal delivered pgrp=%d signal=%d", foreground_pgrp, signal);
            return JNI_TRUE;
        }
        LOGE("foreground pgrp signal failed pgrp=%d signal=%d errno=%d", foreground_pgrp, signal, errno);
    } else {
        LOGE("TIOCGPGRP failed fd=%d fallback_pid=%d signal=%d errno=%d pgrp=%d",
             master_fd, fallback_pid, signal, errno, foreground_pgrp);
    }

    if (signal_descendant_processes((pid_t) fallback_pid, signal, 0)) {
        LOGD("descendant signal delivered fallback_pid=%d signal=%d", fallback_pid, signal);
        return JNI_TRUE;
    }

    if (signal_process((pid_t) fallback_pid, signal)) {
        LOGD("fallback signal delivered pid=%d signal=%d", fallback_pid, signal);
        return JNI_TRUE;
    }

    LOGE("foreground/fallback signal failed fd=%d fallback_pid=%d signal=%d", master_fd, fallback_pid, signal);
    return JNI_FALSE;
}

/**
 * Reads from file descriptor into byte array
 * 
 * Handles EINTR (signal interrupt) and EIO (PTY EOF) correctly.
 */
JNIEXPORT jint JNICALL
Java_org_cosmicide_exec_linux_PtyTerminalKt_nativeRead(JNIEnv *env, jclass clazz,
                                                        jint fd, jbyteArray buffer, jint len) {
    if (fd < 0 || buffer == NULL || len <= 0 || len > PTY_BUFFER_SIZE ||
        len > (*env)->GetArrayLength(env, buffer)) {
        return -1;
    }

    jbyte *buf = (*env)->GetByteArrayElements(env, buffer, NULL);
    if (buf == NULL) {
        return -1;
    }

    // Loop on EINTR - signals can interrupt read()
    ssize_t read_bytes;
    do {
        read_bytes = read(fd, buf, len);
    } while (read_bytes == -1 && errno == EINTR);

    // Handle errors
    if (read_bytes == -1) {
        int error = errno;
        (*env)->ReleaseByteArrayElements(env, buffer, buf, JNI_ABORT);
        // EIO means PTY slave closed - treat as EOF
        if (error == EIO) {
            LOGD("read EOF fd=%d errno=EIO", fd);
            return -1;  // EOF
        }
        LOGE("read failed fd=%d errno=%d", fd, error);
        return -1;  // Error
    }

    (*env)->ReleaseByteArrayElements(env, buffer, buf, 0);
    return (jint)read_bytes;
}

/**
 * Writes from byte array to file descriptor
 * 
 * IMPORTANT: Validates bounds and loops until all bytes are written.
 * PTY writes can return fewer bytes than requested.
 * No artificial cap on write size - removed PTY_BUFFER_SIZE limit.
 * 
 * Returns: total bytes written on success (should equal len), -1 on error
 */
JNIEXPORT jint JNICALL
Java_org_cosmicide_exec_linux_PtyTerminalKt_nativeWrite(JNIEnv *env, jclass clazz,
                                                         jint fd, jbyteArray buffer, jint off,
                                                         jint len) {
    if (fd < 0 || buffer == NULL) {
        return -1;
    }

    if (len <= 0) {
        return 0;
    }
    
    jsize array_len = (*env)->GetArrayLength(env, buffer);
    if (off < 0 || len < 0 || off > array_len || len > array_len - off) {
        return -1;  // Invalid parameters (out of bounds)
    }

    jbyte *buf = (*env)->GetByteArrayElements(env, buffer, NULL);
    if (buf == NULL) {
        return -1;
    }

    // Loop until all bytes are written or error
    ssize_t total_written = 0;
    ssize_t remaining = len;
    
    while (remaining > 0) {
        ssize_t n;
        do {
            n = write(fd, buf + off + total_written, remaining);
        } while (n == -1 && errno == EINTR);
        
        if (n < 0) {
            int error = errno;
            (*env)->ReleaseByteArrayElements(env, buffer, buf, JNI_ABORT);
            LOGE("write failed fd=%d errno=%d total_written=%zd remaining=%zd", fd, error, total_written, remaining);
            return -1;
        }
        
        if (n == 0) {
            // write() returned 0 - should not happen on blocking fd, exit loop
            break;
        }
        
        total_written += n;
        remaining -= n;
    }

    (*env)->ReleaseByteArrayElements(env, buffer, buf, JNI_ABORT);
    return (jint)total_written;
}

/**
 * Sends signal to process group
 * 
 * IMPORTANT: Uses negative PID to send to process group.
 * This ensures all children of the shell receive the signal.
 * Falls back to single PID if process group fails.
 * 
 * Signal codes: 1=SIGHUP, 2=SIGINT, 9=SIGKILL, 15=SIGTERM
 */
JNIEXPORT jboolean JNICALL
Java_org_cosmicide_exec_linux_PtyProcessKt_nativeSendSignal(JNIEnv *env, jclass clazz,
                                                             jint pid, jint signal) {
    if (pid <= 0 || signal <= 0) {
        return JNI_FALSE;
    }

    jboolean delivered = signal_process((pid_t) pid, signal);
    LOGD("nativeSendSignal pid=%d signal=%d delivered=%d", pid, signal, delivered);
    return delivered;
}

/**
 * Waits for a child process to exit and returns its exit status
 * 
 * IMPORTANT: This is a BLOCKING call that REAPS the child.
 * After calling waitFor(), the child is reaped and cannot be waited on again.
 * Call this ONCE to get the final exit code.
 * 
 * Returns: exit code (0 for success, non-zero for failure or signal)
 *          negative value = killed by signal (e.g., -SIGTERM = -15)
 */
JNIEXPORT jint JNICALL
Java_org_cosmicide_exec_linux_PtyProcessKt_nativeWaitPid(JNIEnv *env, jclass clazz,
                                                          jint pid) {
    if (pid <= 0) {
        return -1;
    }

    int status = 0;
    pid_t result = waitpid((pid_t)pid, &status, 0);

    if (result == -1) {
        LOGE("waitpid failed pid=%d errno=%d", pid, errno);
        return -1;
    }

    // Extract and return exit code
    if (WIFEXITED(status)) {
        LOGD("waitpid exited pid=%d code=%d", pid, WEXITSTATUS(status));
        return WEXITSTATUS(status);
    } else if (WIFSIGNALED(status)) {
        // Return negative signal number if killed by signal
        LOGD("waitpid signaled pid=%d signal=%d", pid, WTERMSIG(status));
        return -(WTERMSIG(status));
    }

    return -1;
}
