package cn.myflycat.mat.client.api;

import cn.myflycat.mat.Mat;
import org.graalvm.polyglot.HostAccess;

public final class BaritoneBindings {
    private static final boolean AVAILABLE = detect();

    private static boolean detect() {
        try {
            Class.forName("baritone.api.BaritoneAPI", false, BaritoneBindings.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            Mat.LOGGER.warn("Baritone not detected on classpath; mat.baritone.* will be no-ops.");
            return false;
        }
    }

    @HostAccess.Export
    public boolean available() {
        return AVAILABLE;
    }

    @HostAccess.Export
    public boolean gotoBlock(int x, int y, int z) {
        if (!AVAILABLE) return false;
        return ClientThread.runSync(() -> {
            baritone.api.IBaritone b = primary();
            if (b == null) return false;
            b.getCustomGoalProcess().setGoalAndPath(new baritone.api.pathing.goals.GoalBlock(x, y, z));
            return true;
        });
    }

    @HostAccess.Export
    public boolean gotoXZ(int x, int z) {
        if (!AVAILABLE) return false;
        return ClientThread.runSync(() -> {
            baritone.api.IBaritone b = primary();
            if (b == null) return false;
            b.getCustomGoalProcess().setGoalAndPath(new baritone.api.pathing.goals.GoalXZ(x, z));
            return true;
        });
    }

    @HostAccess.Export
    public boolean gotoNear(int x, int y, int z, int radius) {
        if (!AVAILABLE) return false;
        return ClientThread.runSync(() -> {
            baritone.api.IBaritone b = primary();
            if (b == null) return false;
            b.getCustomGoalProcess().setGoalAndPath(new baritone.api.pathing.goals.GoalNear(
                new net.minecraft.util.math.BlockPos(x, y, z), radius));
            return true;
        });
    }

    @HostAccess.Export
    public boolean mine(int quantity, String... blockNames) {
        if (!AVAILABLE) return false;
        if (blockNames == null || blockNames.length == 0) return false;
        return ClientThread.runSync(() -> {
            baritone.api.IBaritone b = primary();
            if (b == null) return false;
            b.getMineProcess().mineByName(quantity, blockNames);
            return true;
        });
    }

    @HostAccess.Export
    public boolean cancel() {
        if (!AVAILABLE) return false;
        return ClientThread.runSync(() -> {
            baritone.api.IBaritone b = primary();
            if (b == null) return false;
            b.getPathingBehavior().cancelEverything();
            b.getMineProcess().cancel();
            b.getFollowProcess().cancel();
            return true;
        });
    }

    @HostAccess.Export
    public boolean isPathing() {
        if (!AVAILABLE) return false;
        return ClientThread.runSync(() -> {
            baritone.api.IBaritone b = primary();
            return b != null && b.getPathingBehavior().isPathing();
        });
    }

    private static baritone.api.IBaritone primary() {
        baritone.api.IBaritoneProvider p = baritone.api.BaritoneAPI.getProvider();
        return p == null ? null : p.getPrimaryBaritone();
    }
}
