package org.cosmic.ide.ui.console;

/** A circular byte buffer allowing one producer and one consumer thread. */
public class ByteQueue {

    private final byte[] mBuffer;
    private int mHead;
    private int mStoredBytes;

    public ByteQueue(int size) {
        mBuffer = new byte[size];
    }

    public int getBytesAvailable() {
        synchronized (this) {
            return mStoredBytes;
        }
    }

    public int read(byte[] buffer, int offset, int length) throws InterruptedException {
        if (length + offset > buffer.length) {
            throw new IllegalArgumentException("length + offset > buffer.length");
        }
        if (length < 0) {
            throw new IllegalArgumentException("length < 0");
        }
        if (length == 0) {
            return 0;
        }
        synchronized (this) {
            while (mStoredBytes == 0) {
                wait();
            }
            int totalRead = 0;
            int bufferLength = mBuffer.length;
            boolean wasFull = bufferLength == mStoredBytes;
            while (length > 0 && mStoredBytes > 0) {
                int oneRun = Math.min(bufferLength - mHead, mStoredBytes);
                int bytesToCopy = Math.min(length, oneRun);
                System.arraycopy(mBuffer, mHead, buffer, offset, bytesToCopy);
                mHead += bytesToCopy;
                if (mHead >= bufferLength) {
                    mHead = 0;
                }
                mStoredBytes -= bytesToCopy;
                length -= bytesToCopy;
                offset += bytesToCopy;
                totalRead += bytesToCopy;
            }
            if (wasFull) {
                notify();
            }
            return totalRead;
        }
    }

    public void write(byte[] buffer, int offset, int length) throws InterruptedException {
        if (length + offset > buffer.length) {
            throw new IllegalArgumentException("length + offset > buffer.length");
        }
        if (length < 0) {
            throw new IllegalArgumentException("length < 0");
        }
        if (length == 0) {
            return;
        }
        synchronized (this) {
            int bufferLength = mBuffer.length;
            boolean wasEmpty = mStoredBytes == 0;
            while (length > 0) {
                while (bufferLength == mStoredBytes) {
                    wait();
                }
                int tail = mHead + mStoredBytes;
                int oneRun;
                if (tail >= bufferLength) {
                    tail = tail - bufferLength;
                    oneRun = mHead - tail;
                } else {
                    oneRun = bufferLength - tail;
                }
                int bytesToCopy = Math.min(oneRun, length);
                System.arraycopy(buffer, offset, mBuffer, tail, bytesToCopy);
                offset += bytesToCopy;
                mStoredBytes += bytesToCopy;
                length -= bytesToCopy;
            }
            if (wasEmpty) {
                notify();
            }
        }
    }
}