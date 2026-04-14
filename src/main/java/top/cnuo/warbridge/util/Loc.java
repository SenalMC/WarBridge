package top.cnuo.warbridge.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class Loc {
    private Loc() {}

    public static String serialize(Location location) {
        if (location == null || location.getWorld() == null) return "";
        return location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getYaw() + "," + location.getPitch();
    }

    public static Location deserialize(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String[] parts = value.split(",");
        if (parts.length < 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0F;
        float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0F;
        return new Location(world, x, y, z, yaw, pitch);
    }
}
