package cn.myflycat.mat.client.api;

import net.minecraft.client.MinecraftClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public final class ClientThread {
    private ClientThread() {}

    public static <T> T runSync(Supplier<T> fn) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.isOnThread()) {
            return fn.get();
        }
        CompletableFuture<T> future = mc.submit(fn::get);
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for main thread", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error err) throw err;
            throw new RuntimeException(cause);
        }
    }

    public static void runSyncVoid(Runnable r) {
        runSync(() -> { r.run(); return null; });
    }
}
