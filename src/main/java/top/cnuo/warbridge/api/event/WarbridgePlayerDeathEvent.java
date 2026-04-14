package top.cnuo.warbridge.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WarbridgePlayerDeathEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Player killer;
    private final int respawnDelaySeconds;

    public WarbridgePlayerDeathEvent(Player player, Player killer, int respawnDelaySeconds) {
        this.player = player;
        this.killer = killer;
        this.respawnDelaySeconds = respawnDelaySeconds;
    }

    public Player getPlayer() { return player; }
    public Player getKiller() { return killer; }
    public int getRespawnDelaySeconds() { return respawnDelaySeconds; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
