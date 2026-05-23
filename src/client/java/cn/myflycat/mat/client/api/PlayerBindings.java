package cn.myflycat.mat.client.api;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.graalvm.polyglot.HostAccess;

public final class PlayerBindings {
    @HostAccess.Export
    public Pos pos() {
        return ClientThread.runSync(() -> {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            if (p == null) return new Pos(Double.NaN, Double.NaN, Double.NaN);
            return new Pos(p.getX(), p.getY(), p.getZ());
        });
    }

    @HostAccess.Export
    public double health() {
        return ClientThread.runSync(() -> {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            return p == null ? Double.NaN : (double) p.getHealth();
        });
    }

    @HostAccess.Export
    public String dimension() {
        return ClientThread.runSync(() -> {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            if (p == null) return null;
            return p.getWorld().getRegistryKey().getValue().toString();
        });
    }

    @HostAccess.Export
    public float yaw() {
        return ClientThread.runSync(() -> {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            return p == null ? Float.NaN : p.getYaw();
        });
    }

    @HostAccess.Export
    public float pitch() {
        return ClientThread.runSync(() -> {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            return p == null ? Float.NaN : p.getPitch();
        });
    }

    @HostAccess.Export
    public boolean onGround() {
        return ClientThread.runSync(() -> {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            return p != null && p.isOnGround();
        });
    }

    @HostAccess.Export
    public int food() {
        return ClientThread.runSync(() -> {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            return p == null ? 0 : p.getHungerManager().getFoodLevel();
        });
    }

    @HostAccess.Export
    public float saturation() {
        return ClientThread.runSync(() -> {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            return p == null ? Float.NaN : p.getHungerManager().getSaturationLevel();
        });
    }
}
