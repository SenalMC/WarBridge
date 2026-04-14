package top.cnuo.warbridge.game;

import org.bukkit.ChatColor;
import org.bukkit.Color;

public enum TeamColor {
    RED("red", ChatColor.RED, Color.RED, (short) 14),
    YELLOW("yellow", ChatColor.YELLOW, Color.YELLOW, (short) 4),
    GREEN("green", ChatColor.GREEN, Color.GREEN, (short) 5),
    BLUE("blue", ChatColor.BLUE, Color.BLUE, (short) 11);

    private final String id;
    private final ChatColor chatColor;
    private final Color leatherColor;
    private final short woolData;

    TeamColor(String id, ChatColor chatColor, Color leatherColor, short woolData) {
        this.id = id;
        this.chatColor = chatColor;
        this.leatherColor = leatherColor;
        this.woolData = woolData;
    }

    public String getId() { return id; }
    public ChatColor getChatColor() { return chatColor; }
    public Color getLeatherColor() { return leatherColor; }
    public short getWoolData() { return woolData; }

    public static TeamColor fromId(String id) {
        if (id == null) return null;
        for (TeamColor value : values()) {
            if (value.id.equalsIgnoreCase(id)) return value;
        }
        return null;
    }
}
