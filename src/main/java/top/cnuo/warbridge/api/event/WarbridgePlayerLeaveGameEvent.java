package top.cnuo.warbridge.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import top.cnuo.warbridge.game.TeamColor;

public class WarbridgePlayerLeaveGameEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final TeamColor previousTeam;
    private final boolean spectator;

    public WarbridgePlayerLeaveGameEvent(Player player, TeamColor previousTeam, boolean spectator) {
        this.player = player;
        this.previousTeam = previousTeam;
        this.spectator = spectator;
    }

    public Player getPlayer() { return player; }
    public TeamColor getPreviousTeam() { return previousTeam; }
    public boolean wasSpectator() { return spectator; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
