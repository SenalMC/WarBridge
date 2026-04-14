package top.cnuo.warbridge.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import top.cnuo.warbridge.game.GameState;

public class WarbridgePlayerJoinGameEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final GameState stateAtJoin;

    public WarbridgePlayerJoinGameEvent(Player player, GameState stateAtJoin) {
        this.player = player;
        this.stateAtJoin = stateAtJoin;
    }

    public Player getPlayer() { return player; }
    public GameState getStateAtJoin() { return stateAtJoin; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
