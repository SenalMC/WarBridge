package top.cnuo.warbridge.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import top.cnuo.warbridge.WarbridgePlugin;
import top.cnuo.warbridge.game.GameState;
import top.cnuo.warbridge.game.GameTeam;
import top.cnuo.warbridge.game.RoundState;

public class PlayerMoveListener implements Listener {
    private final WarbridgePlugin plugin;

    public PlayerMoveListener(WarbridgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null) return;

        if (plugin.getGameSession().getState() == GameState.GAMING && plugin.getGameSession().getRoundState() == RoundState.RUNNING && !plugin.getGameSession().isSpectator(player.getUniqueId()) && !plugin.getGameSession().isRespawning(player.getUniqueId())) {
            GameTeam portal = plugin.getGameSession().getPortalAt(to);
            if (portal != null) {
                plugin.getGameSession().score(player, portal);
                return;
            }
            int voidY = plugin.getFileManager().get("game.yml").getInt("void-y", 40);
            if (to.getY() < voidY) {
                plugin.getGameSession().killPlayer(player, plugin.getGameSession().findRecentDamager(player), true);
                return;
            }
        } else {
            if (plugin.getGameSession().getLobbySpawn() != null && to.getY() < plugin.getFileManager().get("game.yml").getInt("void-y", 40)) {
                player.teleport(plugin.getGameSession().getLobbySpawn());
            }
        }
    }
}
