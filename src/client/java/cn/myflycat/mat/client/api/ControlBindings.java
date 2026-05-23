package cn.myflycat.mat.client.api;

import cn.myflycat.mat.script.ScriptHandle;
import org.graalvm.polyglot.HostAccess;

public final class ControlBindings {
    private static final long POLL_INTERVAL_MS = 50L;

    private final ScriptHandle handle;

    public ControlBindings(ScriptHandle handle) {
        this.handle = handle;
    }

    @HostAccess.Export
    public void sleep(long millis) throws InterruptedException {
        if (millis <= 0) {
            checkCancelled();
            return;
        }
        long deadline = System.currentTimeMillis() + millis;
        while (true) {
            checkCancelled();
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) return;
            Thread.sleep(Math.min(POLL_INTERVAL_MS, remaining));
        }
    }

    @HostAccess.Export
    public boolean cancelled() {
        return handle.isCancelRequested();
    }

    private void checkCancelled() throws InterruptedException {
        if (handle.isCancelRequested()) {
            throw new InterruptedException("Script cancelled");
        }
    }
}
