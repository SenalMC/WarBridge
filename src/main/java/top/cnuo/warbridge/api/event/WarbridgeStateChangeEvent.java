package top.cnuo.warbridge.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import top.cnuo.warbridge.game.GameState;

public class WarbridgeStateChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final GameState fromState;
    private final GameState toState;

    public WarbridgeStateChangeEvent(GameState fromState, GameState toState) {
        this.fromState = fromState;
        this.toState = toState;
    }

    public GameState getFromState() { return fromState; }
    public GameState getToState() { return toState; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
