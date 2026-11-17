package com.mmorpg.config;

import java.util.ArrayList;
import java.util.List;

/** Per-server layout of an authored Overworld, in general.json (openWorld). */
public final class OpenWorldSettings {
    public double centerX = 0;
    public double centerZ = 0;
    public double mapRadius = 2500;
    public double safeRadius = 200;
    public List<SafeArea> safeAreas = new ArrayList<>();

    public static final class SafeArea {
        public String name = "";
        public double x;
        public double z;
        public double radius;
    }

    public boolean isSafe(double x, double z) {
        double cx = finiteOr(centerX, 0), cz = finiteOr(centerZ, 0);
        double r = Double.isFinite(safeRadius) && safeRadius >= 0 ? safeRadius : 200;
        if (within(x, z, cx, cz, r)) return true;
        if (safeAreas != null) for (SafeArea area : safeAreas) {
            if (area != null && Double.isFinite(area.x) && Double.isFinite(area.z)
                    && Double.isFinite(area.radius) && area.radius > 0
                    && within(x, z, area.x, area.z, area.radius)) return true;
        }
        return false;
    }

    public double distanceRatio(double x, double z) {
        double radius = Double.isFinite(mapRadius) && mapRadius > 0 ? mapRadius : 2500;
        return Math.min(1, Math.hypot(x - finiteOr(centerX, 0), z - finiteOr(centerZ, 0)) / radius);
    }

    private static double finiteOr(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private static boolean within(double x, double z, double cx, double cz, double radius) {
        return Math.hypot(x - cx, z - cz) <= radius;
    }
}
