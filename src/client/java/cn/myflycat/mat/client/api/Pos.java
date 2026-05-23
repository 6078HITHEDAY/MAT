package cn.myflycat.mat.client.api;

import org.graalvm.polyglot.HostAccess;

public final class Pos {
    private final double x;
    private final double y;
    private final double z;

    public Pos(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @HostAccess.Export
    public double getX() { return x; }

    @HostAccess.Export
    public double getY() { return y; }

    @HostAccess.Export
    public double getZ() { return z; }

    @HostAccess.Export
    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
