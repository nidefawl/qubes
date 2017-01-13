package nidefawl.qubes.util;

import nidefawl.qubes.Game;

public class ThreadedWorker {
    private final Object                   sync = new Object();
    public final int                       numThreads;
    Thread[]                               threads;
    private final ResettableCountDownLatch latch;
    private final ResettableCountDownLatch latch2;

    private IThreadedWork target;
    private volatile boolean                            isRunning;

    public ThreadedWorker(int numThreads) {
        this.numThreads = numThreads;
        this.threads      = new Thread[this.numThreads];
        this.latch       = new ResettableCountDownLatch(this.numThreads);
        this.latch2      = new ResettableCountDownLatch(this.numThreads);
        
    }


    public void init() {
        this.isRunning = true;
        for (int i = 0; i < this.threads.length; i++) {
            this.threads[i] = new Thread(new Threaded(this, i));
            this.threads[i].setName("Culler " + i);
            this.threads[i].setDaemon(true);
//            this.threads[i].setPriority(Thread.MAX_PRIORITY);
        }
        for (int i = 0; i < this.threads.length; i++) {
            this.threads[i].start();
        }
    }
    public void stopThread() {
        if (this.isRunning) {
            this.isRunning = false;
            for (int i = 0; i < this.threads.length; i++) {
                if (this.threads[i] != null) {
                    this.threads[i].interrupt();
                }
            }
            for (int i = 0; i < this.threads.length; i++) {
                if (this.threads[i] != null) {
                    try {
                        this.threads[i].join(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    public void work(IThreadedWork target) {
        this.target = target;
        try {
            latch2.await(); // make sure threads are at sync.wait() or hold sync lock
            latch2.reset();
            latch.reset();
            synchronized (sync) {
                sync.notifyAll();
            }
            latch.await(); // make sure threads are finished or further
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static final class Threaded implements Runnable {

        private final int     threadId;
        private ThreadedWorker worker;

        Threaded(ThreadedWorker threadedWorker, int threadId) {
            this.threadId = threadId;
            this.worker = threadedWorker;
        }

        @Override
        public void run() {
            try {
                while (worker.isRunning) {
                    synchronized (worker.sync) {
                        worker.latch2.countDown();
                        worker.sync.wait();
                    }
                    worker.target.fromThread(this.threadId, worker.numThreads);
                    worker.latch.countDown();
                }
            } catch (InterruptedException e) {
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
