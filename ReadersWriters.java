/*
 * Readers-Writers Synchronization Project

 * Student Name: Molka Derouich
 * Student ID: 230504599
 *
  */

// Simple semaphore implementation using wait/notify
// This is the base tool used to control access between threads
class Semaphore {
    private int value;

    // Initialize the semaphore with a starting value
    public Semaphore(int value) {
        this.value = value;
    }

    // acquire() blocks the thread if the semaphore value is 0
    // The thread waits until another thread releases the semaphore
    public synchronized void acquire() throws InterruptedException {
        while (value == 0) {
            wait();
        }
        value--;
    }

    // release() increases the semaphore value
    // and wakes up one waiting thread
    public synchronized void release() {
        value++;
        notify();
    }
}
// This class manages synchronization between readers and writers
class ReadWriteLock {
    private int readerCount = 0;          // Number of active readers
    private final Semaphore mutex = new Semaphore(1);     // Protects readerCount
    private final Semaphore writeLock = new Semaphore(1); // Ensures exclusive writing

    // Called when a reader wants to start reading
    public void readLock() throws InterruptedException {
        mutex.acquire();          // Protect readerCount
        readerCount++;

        // If this is the first reader, block writers
        if (readerCount == 1) {
            writeLock.acquire();
        }

        mutex.release();
    }

    // Called when a reader finishes reading
    public void readUnLock() throws InterruptedException {
        mutex.acquire();
        readerCount--;

        // If this is the last reader, allow writers to continue
        if (readerCount == 0) {
            writeLock.release();
        }

        mutex.release();
    }

    // Writers must acquire exclusive access
    public void writeLock() throws InterruptedException {
        writeLock.acquire();
    }

    // Release exclusive write access
    public void writeUnLock() {
        writeLock.release();
    }
}
// This class represents the shared data accessed by readers and writers
class SharedData {
    private int value = 0;     // Actual data
    private int version = 0;   // Version number for read-once guarantee

    // Writer updates the data and increases the version
    public void write() {
        value++;
        version++;
        System.out.println("Writer wrote value " + value + " (version " + version + ")");
    }

    // Reader reads the data only if it is new
    public void read(String readerName, int lastVersionRead) {
        if (version > lastVersionRead) {
            System.out.println(readerName + " read value " + value + " (version " + version + ")");
        }
    }

    // Used by readers to update the last version they read
    public int getVersion() {
        return version;
    }
}
// Reader thread
class Reader extends Thread {
    private final ReadWriteLock lock;
    private final SharedData data;
    private int lastReadVersion = 0; // Tracks last version read

    public Reader(String name, ReadWriteLock lock, SharedData data) {
        super(name);
        this.lock = lock;
        this.data = data;
    }

    // Reader logic
    public void run() {
        try {
            lock.readLock();                          // Request read access
            data.read(getName(), lastReadVersion);    // Read data
            lastReadVersion = data.getVersion();      // Update version
            lock.readUnLock();                        // Release read access
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
// Writer thread
class Writer extends Thread {
    private final ReadWriteLock lock;
    private final SharedData data;

    public Writer(String name, ReadWriteLock lock, SharedData data) {
        super(name);
        this.lock = lock;
        this.data = data;
    }

    // Writer logic
    public void run() {
        try {
            lock.writeLock();   // Request exclusive access
            data.write();       // Modify shared data
            lock.writeUnLock(); // Release write access
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
// Test class to demonstrate correct synchronization
public class ReadersWritersTest {
    public static void main(String[] args) {

        ReadWriteLock lock = new ReadWriteLock();
        SharedData data = new SharedData();

        // Start readers and writers in mixed order
        new Reader("Reader-1", lock, data).start();
        new Reader("Reader-2", lock, data).start();
        new Writer("Writer-1", lock, data).start();
        new Reader("Reader-3", lock, data).start();
        new Writer("Writer-2", lock, data).start();
    }
}
