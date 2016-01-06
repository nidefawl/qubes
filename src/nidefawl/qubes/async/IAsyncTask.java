package nidefawl.qubes.async;

import java.util.concurrent.Callable;

public abstract class IAsyncTask<T> implements Callable<T> {

    public enum TaskType {
        CHUNK_DECOMPRESS
    }

    public abstract TaskType getType();
    public abstract boolean requiresComplete();
}
