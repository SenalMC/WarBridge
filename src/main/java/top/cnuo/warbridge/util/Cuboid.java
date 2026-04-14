package top.cnuo.warbridge.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class Cuboid {
    private final Location min;
    private final Location max;

    public Cuboid(Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null || !a.getWorld().equals(b.getWorld())) {
            throw new IllegalArgumentException("Cuboid points must be in same world.");
        }
        this.min = new Location(a.getWorld(), Math.min(a.getBlockX(), b.getBlockX()), Math.min(a.getBlockY(), b.getBlockY()), Math.min(a.getBlockZ(), b.getBlockZ()));
        this.max = new Location(a.getWorld(), Math.max(a.getBlockX(), b.getBlockX()), Math.max(a.getBlockY(), b.getBlockY()), Math.max(a.getBlockZ(), b.getBlockZ()));
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null || !location.getWorld().equals(min.getWorld())) return false;
        return location.getBlockX() >= min.getBlockX() && location.getBlockX() <= max.getBlockX()
                && location.getBlockY() >= min.getBlockY() && location.getBlockY() <= max.getBlockY()
                && location.getBlockZ() >= min.getBlockZ() && location.getBlockZ() <= max.getBlockZ();
    }

    public Location getMin() { return min.clone(); }
    public Location getMax() { return max.clone(); }
    public World getWorld() { return min.getWorld(); }

    public List<Block> getBlocks() {
        List<Block> blocks = new ArrayList<Block>();
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    blocks.add(min.getWorld().getBlockAt(x, y, z));
                }
            }
        }
        return blocks;
    }
}
