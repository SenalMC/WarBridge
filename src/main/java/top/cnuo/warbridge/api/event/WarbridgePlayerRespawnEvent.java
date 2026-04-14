package top.cnuo.warbridge.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import top.cnuo.warbridge.game.TeamColor;

public class WarbridgePlayerRespawnEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final TeamColor teamColor;

    public WarbridgePlayerRespawnEvent(Player player, TeamColor teamColor) {
        this.player = player;
        this.teamColor = teamColor;
    }

    public Player getPlayer() { return player; }
    public TeamColor getTeamColor() { return teamColor; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
