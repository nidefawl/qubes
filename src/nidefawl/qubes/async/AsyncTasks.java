package nidefawl.qubes.async;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.*;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import nidefawl.qubes.util.GameError;

public class AsyncTasks {
    static ArrayList<Future<Runnable>> tasks = new ArrayList<>();
    static ExecutorService service;
    public static void init() {
        if (service == null)
        service = Executors.newFixedThreadPool(2, 
                new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("PooledThread_%d")
                .setPriority(Thread.NORM_PRIORITY-3)
                .setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        e.printStackTrace();
                    }
                }).setThreadFactory(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new AsyncTaskThread(r);
                    }
                })
                .build());
    }
    public static Future<Runnable> submit(IAsyncTask<Runnable> iAsyncTask) {
        Future<Runnable> future = service.submit(iAsyncTask);
        if (iAsyncTask.requiresComplete()) {
            tasks.add(future);    
        }
        return future;
    }
    public static void completeTasks() {
        
        if (!tasks.isEmpty()) {
            long start = System.currentTimeMillis();
            for (int i = 0; i < tasks.size(); i++) {
                Future<Runnable> a = tasks.get(i);
                if (a.isDone()) {
                    tasks.remove(i--);
                    try {
                        Runnable run = a.get();
                        if (run != null) {
                            run.run();
                        }
                    } catch (Exception e) {
                        throw new GameError("Error while handling async tasks", e);
                    }
                    long end = System.currentTimeMillis()-start;
                    if (end > 120) {
                        System.out.println("takes long");
                        break;
                    }
                }
            }
        }
    }
    public static void shutdown() {
        if (service != null)
            service.shutdownNow();
    }
}
