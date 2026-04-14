package top.cnuo.warbridge.listener;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import top.cnuo.warbridge.WarbridgePlugin;
import top.cnuo.warbridge.game.GameState;
import top.cnuo.warbridge.game.RoundState;

import java.util.ArrayList;
import java.util.List;

public class BlockListener implements Listener {
    private final WarbridgePlugin plugin;

    public BlockListener(WarbridgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE && plugin.getGameSession().getState() == GameState.EDIT) return;
        if (plugin.getGameSession().isSpectator(player.getUniqueId()) || plugin.getGameSession().isRespawning(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getGameSession().getState() != GameState.GAMING || plugin.getGameSession().getRoundState() != RoundState.RUNNING) {
            event.setCancelled(true);
            return;
        }
        Location location = event.getBlockPlaced().getLocation();
        List<Material> allowed = new ArrayList<Material>();
        for (String name : plugin.getFileManager().get("game.yml").getStringList("allowed-place-materials")) {
            Material material = Material.matchMaterial(name);
            if (material != null) allowed.add(material);
        }
        if (!allowed.contains(event.getBlockPlaced().getType())
                || plugin.getGameSession().isPortal(location)
                || plugin.getGameSession().isCage(location)
                || plugin.getGameSession().isProtectedSpawn(location)
                || !plugin.getGameSession().isInGameRegion(location)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessages().get(player, "cannot-place"));
            return;
        }
        plugin.getGameSession().registerPlacedBlock(location, event.getBlockReplacedState().getType(), event.getBlockReplacedState().getRawData());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE && plugin.getGameSession().getState() == GameState.EDIT) return;
        if (plugin.getGameSession().isSpectator(player.getUniqueId()) || plugin.getGameSession().isRespawning(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getGameSession().getState() != GameState.GAMING || plugin.getGameSession().getRoundState() != RoundState.RUNNING) {
            event.setCancelled(true);
            return;
        }
        Location location = event.getBlock().getLocation();
        if (plugin.getGameSession().isCage(location) || plugin.getGameSession().isPortal(location) || !plugin.getGameSession().isPlacedBlock(location)) {
            event.setCancelled(true);
            return;
        }
        plugin.getGameSession().removePlacedBlock(location);
    }
}
