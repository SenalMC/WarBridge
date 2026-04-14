package top.cnuo.warbridge.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public final class BungeeUtil {
    private BungeeUtil() {}

    public static void sendToServer(JavaPlugin plugin, Player player, String server) {
        if (plugin == null || player == null || server == null || server.trim().isEmpty()) return;
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
        } catch (Exception ignored) {
        }
    }
}
