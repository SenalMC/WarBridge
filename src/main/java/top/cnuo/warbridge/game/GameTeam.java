package top.cnuo.warbridge.game;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import top.cnuo.warbridge.util.Cuboid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GameTeam {
    private final TeamColor color;
    private String displayName;
    private Location spawn;
    private Cuboid cage;
    private Cuboid portal;
    private final Set<UUID> players = new HashSet<UUID>();
    private int score;
    private final List<BlockStateSnapshot> spawnedCage = new ArrayList<BlockStateSnapshot>();

    public GameTeam(TeamColor color, String displayName) {
        this.color = color;
        this.displayName = displayName;
    }

    public TeamColor getColor() { return color; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Location getSpawn() { return spawn == null ? null : spawn.clone(); }
    public void setSpawn(Location spawn) { this.spawn = spawn == null ? null : spawn.clone(); }
    public Cuboid getCage() { return cage; }
    public void setCage(Cuboid cage) { this.cage = cage; }
    public Cuboid getPortal() { return portal; }
    public void setPortal(Cuboid portal) { this.portal = portal; }
    public Set<UUID> getPlayers() { return players; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public void addScore() { this.score++; }
    public int size() { return players.size(); }

    public void spawnCage(Material material) {
        clearCage();
        if (cage == null || material == null) return;
        for (Block block : cage.getBlocks()) {
            spawnedCage.add(new BlockStateSnapshot(block.getLocation(), block.getType(), block.getData()));
            block.setType(material);
            block.setData((byte) 0);
        }
    }

    public void clearCage() {
        for (BlockStateSnapshot snapshot : spawnedCage) {
            Block block = snapshot.location.getBlock();
            block.setType(snapshot.type);
            block.setData(snapshot.data);
        }
        spawnedCage.clear();
    }

    private static class BlockStateSnapshot {
        private final Location location;
        private final Material type;
        private final byte data;

        private BlockStateSnapshot(Location location, Material type, byte data) {
            this.location = location;
            this.type = type;
            this.data = data;
        }
    }
}
