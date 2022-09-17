package org.cosmic.ide.ui.console

/** Created by Duy on 10-Feb-17 */
class IntegerQueue(size: Int) {
    const val QUEUE_SIZE = 4 * 1024 // 4MB ram
    private val text: IntArray
    private var front = 0
    private var rear = 0

    init {
        text = IntArray(size)
    }

    fun getFront(): Int = front

    fun getRear(): Int = rear

    @Synchronized
    fun read(): Int {

        while (front == rear) {
            try {
                wait()
            } catch (ignored: InterruptedException) {
            }
        }
        val b = text.get(front)
        front++
        if (front >= text.length) {
            front = 0
        }
        b
    }

    @Synchronized
    fun write(b: Int) {
        text[rear] = b
        rear++
        if (rear >= text.length) rear = 0
        if (front == rear) {
            front++
            if (front >= text.length) front = 0
        }
        notify()
    }

    @Synchronized
    fun write(data: IntArray) {
        for (i in data) {
            write(i)
        }
    }

    @Synchronized
    fun flush() {
        rear = front
        notify()
    }

    @Synchronized
    fun clear() {
        rear = front
        notify()
    }
}
