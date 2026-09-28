package isp.multithread.programming.basic;

/**
 * Classic producer - consumer example using synchronized, wait() and notifyAll().
 *
 * The producer puts numbers into a buffer that can hold a single value.
 * The consumer takes numbers out of the buffer.
 *
 * - If the buffer is full, the producer must wait until the consumer takes the value.
 * - If the buffer is empty, the consumer must wait until the producer puts a value.
 */
public class ProducerConsumerExample {

    public static void main(String[] args) {
        Buffer buffer = new Buffer();

        Producer producer = new Producer(buffer);
        Consumer consumer = new Consumer(buffer);

        producer.start();
        consumer.start();
    }
}

/**
 * Shared object between the two threads. Holds at most one value.
 */
class Buffer {
    private int value;
    private boolean empty = true;

    /**
     * Called by the producer. Waits while the buffer is full.
     */
    public synchronized void put(int newValue) throws InterruptedException {
        // wait() is always called inside a while loop: when the thread wakes up
        // it checks the condition again before continuing
        while (!empty) {
            System.out.println("Producer: buffer is full, waiting...");
            wait(); // releases the lock on this object and sleeps until notified
        }

        value = newValue;
        empty = false;
        System.out.println("Producer: put " + value);

        notifyAll(); // wake up the consumer, there is a value to take
    }

    /**
     * Called by the consumer. Waits while the buffer is empty.
     */
    public synchronized int take() throws InterruptedException {
        while (empty) {
            System.out.println("Consumer: buffer is empty, waiting...");
            wait();
        }

        empty = true;
        System.out.println("Consumer: took " + value);

        notifyAll(); // wake up the producer, there is free space now
        return value;
    }
}

class Producer extends Thread {
    private Buffer buffer;

    public Producer(Buffer buffer) {
        this.buffer = buffer;
    }

    public void run() {
        try {
            for (int i = 1; i <= 10; i++) {
                buffer.put(i);
                Thread.sleep(500); // producer is fast
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class Consumer extends Thread {
    private Buffer buffer;

    public Consumer(Buffer buffer) {
        this.buffer = buffer;
    }

    public void run() {
        try {
            for (int i = 1; i <= 10; i++) {
                buffer.take();
                Thread.sleep(1000); // consumer is slow, so the producer will have to wait
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
