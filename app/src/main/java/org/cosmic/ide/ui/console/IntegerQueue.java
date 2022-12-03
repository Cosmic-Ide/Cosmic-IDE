package org.cosmic.ide.ui.console;

import android.util.Log;

/** Created by Duy on 10-Feb-17. */
public class IntegerQueue {

    public static final int QUEUE_SIZE = 4 * 1024; // 4MB ram
    private static final String TAG = "ByteQueue";
    public int[] text;
    public int front;
    public int rear;

    public IntegerQueue(int size) {
        text = new int[size];
        front = 0;
        rear = 0;
    }

    public int getFront() {
        return front;
    }

    public int getRear() {
        return rear;
    }

    public synchronized int read() {
        Log.d(TAG, "read() called");

        while (front == rear) {
            try {
                wait();
            } catch (InterruptedException ignored) {
            }
        }
        int b = text[front];
        front++;
        if (front >= text.length) front = 0;
        return b;
    }

    public synchronized void write(int b) {
        text[rear] = b;
        rear++;
        if (rear >= text.length) rear = 0;
        if (front == rear) {
            front++;
            if (front >= text.length) front = 0;
        }
        notify();
    }

    public synchronized void write(int[] data) {
        for (int i : data) {
            write(i);
        }
    }

    public synchronized void flush() {
        rear = front;
        notify();
    }

    public synchronized void clear() {
        rear = front;
        notify();
    }
}
