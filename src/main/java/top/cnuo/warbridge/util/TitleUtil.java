package top.cnuo.warbridge.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class TitleUtil {
    private TitleUtil() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) return;
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> enumTitleAction = Class.forName("net.minecraft.server." + version + ".PacketPlayOutTitle$EnumTitleAction");
            Class<?> packetTitle = Class.forName("net.minecraft.server." + version + ".PacketPlayOutTitle");
            Class<?> iChatBaseComponent = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Class<?> chatSerializer = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");

            Object titleComp = chatSerializer.getMethod("a", String.class).invoke(null, "{\"text\":\"" + json(title) + "\"}");
            Object subtitleComp = chatSerializer.getMethod("a", String.class).invoke(null, "{\"text\":\"" + json(subtitle) + "\"}");
            Object titleAction = Enum.valueOf((Class<Enum>) enumTitleAction, "TITLE");
            Object subtitleAction = Enum.valueOf((Class<Enum>) enumTitleAction, "SUBTITLE");

            Constructor<?> timingCtor = packetTitle.getConstructor(int.class, int.class, int.class);
            Constructor<?> titleCtor = packetTitle.getConstructor(enumTitleAction, iChatBaseComponent);
            Object timing = timingCtor.newInstance(fadeIn, stay, fadeOut);
            Object titlePacket = titleCtor.newInstance(titleAction, titleComp);
            Object subtitlePacket = titleCtor.newInstance(subtitleAction, subtitleComp);

            sendPacket(player, timing);
            sendPacket(player, titlePacket);
            sendPacket(player, subtitlePacket);
        } catch (Throwable ignored) {
        }
    }

    private static void sendPacket(Player player, Object packet) throws Exception {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object connection = handle.getClass().getField("playerConnection").get(handle);
        for (Method method : connection.getClass().getMethods()) {
            if (method.getName().equals("sendPacket") && method.getParameterTypes().length == 1) {
                method.invoke(connection, packet);
                return;
            }
        }
    }

    private static String json(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
