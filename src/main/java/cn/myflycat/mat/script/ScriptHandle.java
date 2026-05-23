package cn.myflycat.mat.script;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class ScriptHandle {
    public enum State { READY, RUNNING, CANCELLED, FAILED, DONE }

    private final String name;
    private final AtomicReference<State> state = new AtomicReference<>(State.READY);
    private final AtomicBoolean cancelFlag = new AtomicBoolean(false);
    private final CompletableFuture<Void> completion = new CompletableFuture<>();
    private volatile Throwable error;

    public ScriptHandle(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public State state() {
        return state.get();
    }

    public boolean isCancelRequested() {
        return cancelFlag.get();
    }

    public Throwable error() {
        return error;
    }

    public CompletableFuture<Void> completion() {
        return completion;
    }

    public void requestCancel() {
        cancelFlag.set(true);
    }

    boolean markRunning() {
        return state.compareAndSet(State.READY, State.RUNNING);
    }

    void markDone() {
        if (state.compareAndSet(State.RUNNING, State.DONE)) {
            completion.complete(null);
        }
    }

    void markFailed(Throwable t) {
        this.error = t;
        if (state.compareAndSet(State.RUNNING, State.FAILED)) {
            completion.complete(null);
        }
    }

    void markCancelled() {
        if (state.compareAndSet(State.RUNNING, State.CANCELLED)) {
            completion.complete(null);
        }
    }
}
