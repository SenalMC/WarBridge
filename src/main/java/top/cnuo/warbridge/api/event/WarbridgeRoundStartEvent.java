package top.cnuo.warbridge.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WarbridgeRoundStartEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final int roundNumber;

    public WarbridgeRoundStartEvent(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public int getRoundNumber() { return roundNumber; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
