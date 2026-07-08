/*
 * PTY Native Library for Cosmic IDE
 * Provides pseudo-terminal allocation and control
 * 
 * DESIGN PRINCIPLES:
 * 1. All PTY (slave FD) management happens natively - never exposed to Java
 * 2. Only master_fd and child_pid returned to Java
 * 3. Slave FD closed in parent after fork - prevents EOF issues
 * 4. Master FD kept BLOCKING for proper InputStream behavior
 * 5. Process group signals (-pid) for terminal control
 */

#ifndef PTY_NATIVE_H
#define PTY_NATIVE_H

#include <jni.h>

/**
 * Spawns child process inside PTY (fork/exec)
 * 
 * This is the PRIMARY entry point for PTY creation.
 * 
 * Arguments:
 *  - working_dir: directory to chdir to in child
 *  - exe_path: absolute path to executable
 *  - argv: array of command-line arguments (null-terminated)
 *  - env_vars: array of environment variables (null-terminated), or NULL to inherit
 * 
 * Returns: [master_fd, child_pid]
 *  - master_fd: File descriptor for terminal I/O
 *  - child_pid: Process ID for signal control
 * 
 * NOTE: Slave FD is closed in parent immediately after fork.
 *       Java never sees or touches the slave FD.
 */
JNIEXPORT jintArray JNICALL
Java_org_cosmicide_exec_linux_PtyTerminalKt_nativeSpawnInPty(JNIEnv *env, jclass clazz,
                                                              jstring working_dir,
                                                              jstring exe_path,
                                                              jobjectArray argv,
                                                              jobjectArray env_vars);

/**
 * Closes PTY master file descriptor
 * 
 * NOTE: Slave FD is closed in parent after fork, never managed from Java.
 *       This only closes the master FD that Java owns.
 */
JNIEXPORT jboolean JNICALL
Java_org_cosmicide_exec_linux_PtyTerminalKt_nativeClosePty(JNIEnv *env, jclass clazz,
                                                            jint master_fd);

/**
 * Sets terminal window size
 * Uses TIOCSWINSZ ioctl on master_fd
 */
JNIEXPORT jboolean JNICALL
Java_org_cosmicide_exec_linux_PtyTerminalKt_nativeSetWindowSize(JNIEnv *env, jclass clazz,
                                                                jint master_fd, jint rows,
                                                                jint columns);

/**
 * Sends a signal to the PTY foreground process group.
 * Falls back to fallback_pid's process group and then fallback_pid itself.
 */
JNIEXPORT jboolean JNICALL
Java_org_cosmicide_exec_linux_PtyTerminalKt_nativeSendSignalToForegroundProcessGroup(
        JNIEnv *env, jclass clazz, jint master_fd, jint fallback_pid, jint signal);

/**
 * Reads from file descriptor into byte array
 */
JNIEXPORT jint JNICALL
Java_org_cosmicide_exec_linux_PtyTerminalKt_nativeRead(JNIEnv *env, jclass clazz,
                                                        jint fd, jbyteArray buffer, jint len);

/**
 * Writes to file descriptor from byte array
 */
JNIEXPORT jint JNICALL
Java_org_cosmicide_exec_linux_PtyTerminalKt_nativeWrite(JNIEnv *env, jclass clazz,
                                                         jint fd, jbyteArray buffer, jint off,
                                                         jint len);

/**
 * Sends signal to process GROUP (not just single process)
 * 
 * IMPORTANT: Uses -pid to target the process group.
 * This ensures Ctrl+C, Ctrl+Z work on child processes (java, gradle, etc.).
 * 
 * Signal codes: 1=SIGHUP, 2=SIGINT, 9=SIGKILL, 15=SIGTERM
 */
JNIEXPORT jboolean JNICALL
Java_org_cosmicide_exec_linux_PtyProcessKt_nativeSendSignal(JNIEnv *env, jclass clazz,
                                                             jint pid, jint signal);

/**
 * Waits for child process to complete (BLOCKING)
 * 
 * REAPS the child process - only call this ONCE.
 * After waitFor(), the child is consumed and cannot be waited on again.
 * 
 * Returns: exit code (0 for success, non-zero for failure)
 *          negative value = killed by signal (e.g., -SIGTERM = -15)
 */
JNIEXPORT jint JNICALL
Java_org_cosmicide_exec_linux_PtyProcessKt_nativeWaitPid(JNIEnv *env, jclass clazz,
                                                          jint pid);

#endif /* PTY_NATIVE_H */
