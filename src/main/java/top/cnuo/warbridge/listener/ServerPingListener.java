package top.cnuo.warbridge.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import top.cnuo.warbridge.WarbridgePlugin;

import java.lang.reflect.Method;
import java.util.Locale;

public class ServerPingListener implements Listener {
    private final WarbridgePlugin plugin;

    public ServerPingListener(WarbridgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        String key = plugin.getGameSession().getState().name().toLowerCase(Locale.ENGLISH);
        String line1 = plugin.getPlaceholderService().apply(null, plugin.getFileManager().get("motd.yml").getString("motds." + key + ".line1", "&eWarbridge"));
        String line2 = plugin.getPlaceholderService().apply(null, plugin.getFileManager().get("motd.yml").getString("motds." + key + ".line2", "&f%warbridge_state%"));
        event.setMotd(line1 + "\n" + line2);
        handlePlayerCount(event, key);
    }

    private void handlePlayerCount(ServerListPingEvent event, String key) {
        boolean useReal = plugin.getFileManager().get("motd.yml").getBoolean("player-count." + key + ".use-real", true);
        if (useReal) return;
        int shownOnline = plugin.getFileManager().get("motd.yml").getInt("player-count." + key + ".shown-online", plugin.getGameSession().getPlayersCount());
        int shownMax = plugin.getFileManager().get("motd.yml").getInt("player-count." + key + ".shown-max", plugin.getFileManager().get("game.yml").getInt("max-players", 8));
        try {
            Method setNumPlayers = event.getClass().getMethod("setNumPlayers", int.class);
            setNumPlayers.invoke(event, shownOnline);
            Method setMaxPlayers = event.getClass().getMethod("setMaxPlayers", int.class);
            setMaxPlayers.invoke(event, shownMax);
        } catch (Exception ignored) {
        }
    }
}
