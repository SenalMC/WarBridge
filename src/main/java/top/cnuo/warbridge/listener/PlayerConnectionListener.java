package top.cnuo.warbridge.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import top.cnuo.warbridge.WarbridgePlugin;

public class PlayerConnectionListener implements Listener {
    private final WarbridgePlugin plugin;

    public PlayerConnectionListener(WarbridgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.getGameSession().joinPlayer(event.getPlayer());
            }
        }, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        plugin.getGameSession().removePlayer(event.getPlayer());
    }
}
