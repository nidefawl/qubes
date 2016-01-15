package nidefawl.qubes.test;

import java.util.concurrent.LinkedBlockingQueue;

public class ThreadInterrupt {

    public static void main(String[] args) {
        MyThread t = new MyThread();
//      for (int i = 0; i < 10; i++)
        t.addQueue(1);
        t.start();
        try {
            Thread.sleep(44);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        for (int i = 0; i < 10; i++)t.addQueue(1);
        t.stopThread();
        System.out.println(t.total);
    }

    static class MyThread extends Thread {
        int                                total      = 0;
        private volatile boolean           isRunning  = true;
        private volatile boolean           isFinished = false;
        final LinkedBlockingQueue<Integer> q          = new LinkedBlockingQueue<>();

        public MyThread() {
            setName("MyThread");
            setDaemon(false);
        }

        public void addQueue(int a) {
            q.offer(a);
        }

        public void run() {
            System.out.println(getName() + " start");
            while (isRunning) {
                try {
                    Integer i = q.take();
                    if (i != null) {
                        this.total += i;
                        Thread.sleep(2); //heavy work
                    }
                    this.interrupt();
                } catch (InterruptedException e) {
                    System.err.println("break");
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            isFinished = true;
            System.out.println(getName() + " finished");
        }

        public void stopThread() {
            if (isRunning) {
                isRunning = false;
                while (!isFinished) {
                    try {
                        System.out.println("waiting");
                        Thread.sleep(20);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                System.out.println("end");
            }
        }
    }
}
