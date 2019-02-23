package nidefawl.qubes.async;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public abstract class AsyncTask implements Callable<Void> {
    private Future<Void> future;

    public enum TaskType {
        CHUNK_DECOMPRESS, LOAD_TEXTURES
    }

    public abstract TaskType getType();
    public void pre() {
        
    }
    public void post() {
        
    }
    public void setFuture(Future<Void> future) {
        this.future = future;
    }
    public boolean isDone() {
        return this.future.isDone();
    }
    public boolean isCancelled() {
        return this.future.isCancelled();
    }
    public void checkException() throws Exception {
        this.future.get();
    }
}
