package nidefawl.qubes.async;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.concurrent.*;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import nidefawl.qubes.util.GameError;

public class AsyncTasks {
    static ArrayList<AsyncTask> tasks = new ArrayList<>();
    static ExecutorService service;
    public static void init() {
        if (service == null)
        service = Executors.newFixedThreadPool(4, 
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
    public static void submit(AsyncTask iAsyncTask) {
        try {
            if (service == null) {
                return;
            }
            iAsyncTask.pre();
            iAsyncTask.setFuture(service.submit(iAsyncTask));
            tasks.add(iAsyncTask);  
        } catch (java.util.concurrent.RejectedExecutionException e) {
            if (!service.isTerminated()) {
                throw new GameError("while submitting task "+iAsyncTask, e);
            }
        } catch (Exception e2) {
            throw new GameError("while submitting task "+iAsyncTask, e2);
            
        }  
    }
    public static boolean completeTasks() {
        if (!tasks.isEmpty()) {
            long start = System.currentTimeMillis();
            for (int i = 0; i < tasks.size(); i++) {
                AsyncTask a = tasks.get(i);
                if (a.isDone()||a.isCancelled()) {
                    tasks.remove(i--);
                    try {
                        a.post();
                    } catch (Exception e) {
                        throw new GameError("Error while handling async tasks", e);
                    }
                    long end = System.currentTimeMillis()-start;
                    if (end > 120) {
                        System.out.println("takes long");
                        break;
                    }
                } else {
                }
            }
        }
        return tasks.isEmpty();
    }
    public static void shutdown() {
        if (service != null) {
            service.shutdownNow();
            service = null;
        }
    }
}
