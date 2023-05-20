/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package com.intellij.util;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.impl.CoreProgressManager;
import com.intellij.util.containers.ConcurrentList;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public final class ReadMostlyRWLock {

    private static final int SPIN_TO_WAIT_FOR_LOCK = 100;
    @NotNull
    final Thread writeThread;
    private final AtomicBoolean writeIntent = new AtomicBoolean(false);
    private final ConcurrentList<Reader> readers = ContainerUtil.createConcurrentList();
    private final ThreadLocal<Reader> R =
            ThreadLocal.withInitial(
                    () -> {
                        Reader status = new Reader(Thread.currentThread());
                        boolean added = readers.addIfAbsent(status);
                        assert added : readers + "; " + Thread.currentThread();
                        return status;
                    });
    volatile boolean writeRequested;
    private volatile boolean writeAcquired;
    private volatile boolean writeSuspended;
    private volatile long deadReadersGCStamp;

    public ReadMostlyRWLock(@NotNull Thread writeThread) {
        this.writeThread = writeThread;
    }

    public boolean isWriteThread() {
        return Thread.currentThread() == writeThread;
    }

    public boolean isReadLockedByThisThread() {
        checkReadThreadAccess();
        Reader status = R.get();
        return status.readRequested;
    }

    public Reader startRead() {
        if (Thread.currentThread() == writeThread) return null;
        Reader status = R.get();
        throwIfImpatient(status);
        if (status.readRequested) return null;

        if (!tryReadLock(status)) {
            ProgressIndicator progress = ProgressIndicatorProvider.getGlobalProgressIndicator();
            for (int iter = 0; ; iter++) {
                if (tryReadLock(status)) {
                    break;
                }

                if (progress != null
                        && progress.isCanceled()
                        && !ProgressManager.getInstance().isInNonCancelableSection()) {
                    throw new ProcessCanceledException();
                }
                waitABit(status, iter);
            }
        }
        return status;
    }

    public Reader startTryRead() {
        if (Thread.currentThread() == writeThread) return null;
        Reader status = R.get();
        throwIfImpatient(status);
        if (status.readRequested) return null;

        tryReadLock(status);
        return status;
    }

    public void endRead(Reader status) {
        checkReadThreadAccess();
        status.readRequested = false;
        if (writeRequested) {
            LockSupport.unpark(writeThread);
        }
    }

    private void waitABit(Reader status, int iteration) {
        if (iteration > SPIN_TO_WAIT_FOR_LOCK) {
            status.blocked = true;
            try {
                throwIfImpatient(status);
                LockSupport.parkNanos(this, 1_000_000);
            } finally {
                status.blocked = false;
            }
        } else {
            Thread.yield();
        }
    }

    private void throwIfImpatient(Reader status) throws CannotRunReadActionException {
        if (status.impatientReads
                && writeRequested
                && !ProgressManager.getInstance().isInNonCancelableSection()
                && CoreProgressManager.ENABLED) {
            throw new CannotRunReadActionException();
        }
    }

    public boolean isInImpatientReader() {
        return R.get().impatientReads;
    }

    public void executeByImpatientReader(@NotNull Runnable runnable)
            throws CannotRunReadActionException {
        checkReadThreadAccess();
        Reader status = R.get();
        boolean old = status.impatientReads;
        try {
            status.impatientReads = true;
            runnable.run();
        } finally {
            status.impatientReads = old;
        }
    }

    private boolean tryReadLock(Reader status) {
        throwIfImpatient(status);
        if (!writeRequested) {
            status.readRequested = true;
            if (!writeRequested) {
                return true;
            }
            status.readRequested = false;
        }
        return false;
    }

    public void writeIntentLock() {
        checkWriteThreadAccess();
        for (int iter = 0; ; iter++) {
            if (writeIntent.compareAndSet(false, true)) {
                assert !writeRequested;
                assert !writeAcquired;

                break;
            }

            if (iter > SPIN_TO_WAIT_FOR_LOCK) {
                LockSupport.parkNanos(this, 1_000_000);
            } else {
                Thread.yield();
            }
        }
    }

    public void writeIntentUnlock() {
        checkWriteThreadAccess();

        assert !writeAcquired;
        assert !writeRequested;

        writeIntent.set(false);
        LockSupport.unpark(writeThread);
    }

    public void writeLock() {
        checkWriteThreadAccess();
        assert !writeRequested;
        assert !writeAcquired;

        writeRequested = true;
        for (int iter = 0; ; iter++) {
            if (areAllReadersIdle()) {
                writeAcquired = true;
                break;
            }

            if (iter > SPIN_TO_WAIT_FOR_LOCK) {
                LockSupport.parkNanos(this, 1_000_000);
            } else {
                Thread.yield();
            }
        }
    }

    public void writeSuspendWhilePumpingIdeEventQueueHopingForTheBest(@NotNull Runnable runnable) {
        boolean prev = writeSuspended;
        writeSuspended = true;
        writeUnlock();
        try {
            runnable.run();
        } finally {
            // cancelActionToBeCancelledBeforeWrite();
            writeLock();
            writeSuspended = prev;
        }
    }

    public void writeUnlock() {
        checkWriteThreadAccess();
        writeAcquired = false;
        writeRequested = false;
        List<Reader> dead;
        long current = System.nanoTime();
        if (current - deadReadersGCStamp > 1_000_000) {
            dead = new ArrayList<>(readers.size());
            deadReadersGCStamp = current;
        } else {
            dead = null;
        }
        for (Reader reader : readers) {
            if (reader.blocked) {
                LockSupport.unpark(reader.thread);
            } else if (dead != null && !reader.thread.isAlive()) {
                dead.add(reader);
            }
        }

        if (dead != null) {
            readers.removeAll(dead);
        }
    }

    private void checkWriteThreadAccess() {
        if (Thread.currentThread() != writeThread) {
            throw new IllegalStateException(
                    "Current thread: "
                            + Thread.currentThread()
                            + "; "
                            + "expected: "
                            + writeThread);
        }
    }

    private void checkReadThreadAccess() {
        if (Thread.currentThread() == writeThread) {
            throw new IllegalStateException(
                    "Must not start read from the write thread: " + Thread.currentThread());
        }
    }

    private boolean areAllReadersIdle() {
        for (Reader reader : readers) {
            if (reader.readRequested) {
                return false;
            }
        }
        return true;
    }

    public boolean isWriteLocked() {
        return writeAcquired;
    }

    @Override
    public String toString() {
        return "ReadMostlyRWLock{"
                + "writeThread="
                + writeThread
                + ", writeRequested="
                + writeRequested
                + ", writeIntent="
                + writeIntent
                + ", writeAcquired="
                + writeAcquired
                + ", readers="
                + readers
                + ", writeSuspended="
                + writeSuspended
                + ", deadReadersGCStamp="
                + deadReadersGCStamp
                + ", R="
                + R
                + '}';
    }

    public static class Reader {
        @NotNull
        private final Thread thread;
        volatile boolean readRequested;
        private volatile boolean blocked;
        private volatile boolean impatientReads;

        Reader(@NotNull Thread readerThread) {
            thread = readerThread;
        }

        @Override
        public String toString() {
            return "Reader{"
                    + "thread="
                    + thread
                    + ", readRequested="
                    + readRequested
                    + ", "
                    + "blocked="
                    + blocked
                    + ", impatientReads="
                    + impatientReads
                    + '}';
        }
    }

    public static class CannotRunReadActionException extends RuntimeException {
    }
}
