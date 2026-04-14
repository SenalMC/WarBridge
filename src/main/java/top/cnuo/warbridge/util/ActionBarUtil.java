package top.cnuo.warbridge.util;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class ActionBarUtil {
    private ActionBarUtil() {}

    public static void send(Player player, String text) {
        if (player == null) return;
        try {
            String version = player.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            Object craftPlayer = craftPlayerClass.cast(player);
            Method getHandle = craftPlayerClass.getMethod("getHandle");
            Object entityPlayer = getHandle.invoke(craftPlayer);
            Class<?> iChatBaseComponentClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Class<?> chatSerializerClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            Method a = chatSerializerClass.getMethod("a", String.class);
            Object baseComponent = a.invoke(null, "{\"text\":\"" + escape(Text.color(text)) + "\"}");
            Class<?> packetPlayOutChatClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
            Constructor<?> constructor = packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, byte.class);
            Object packet = constructor.newInstance(baseComponent, (byte) 2);
            Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);
            Method sendPacket = playerConnection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet"));
            sendPacket.invoke(playerConnection, packet);
        } catch (Throwable ex) {
            player.sendMessage(Text.color(text));
        }
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
