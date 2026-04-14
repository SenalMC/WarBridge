package top.cnuo.warbridge.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WarbridgeGameStartEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final int activePlayers;
    private final int targetPoints;

    public WarbridgeGameStartEvent(int activePlayers, int targetPoints) {
        this.activePlayers = activePlayers;
        this.targetPoints = targetPoints;
    }

    public int getActivePlayers() { return activePlayers; }
    public int getTargetPoints() { return targetPoints; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
