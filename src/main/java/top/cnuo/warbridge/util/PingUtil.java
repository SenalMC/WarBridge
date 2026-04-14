package top.cnuo.warbridge.util;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class PingUtil {
    private PingUtil() {}

    public static int getPing(Player player) {
        try {
            Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
            return entityPlayer.getClass().getField("ping").getInt(entityPlayer);
        } catch (Exception ignored) {
            try {
                Method method = player.getClass().getMethod("getPing");
                return ((Number) method.invoke(player)).intValue();
            } catch (Exception e) {
                return -1;
            }
        }
    }
}
