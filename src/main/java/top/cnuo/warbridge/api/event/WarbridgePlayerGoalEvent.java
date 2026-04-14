package top.cnuo.warbridge.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import top.cnuo.warbridge.game.GameTeam;

public class WarbridgePlayerGoalEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final GameTeam scoringTeam;
    private final GameTeam enteredPortalTeam;
    private final int newScore;

    public WarbridgePlayerGoalEvent(Player player, GameTeam scoringTeam, GameTeam enteredPortalTeam, int newScore) {
        this.player = player;
        this.scoringTeam = scoringTeam;
        this.enteredPortalTeam = enteredPortalTeam;
        this.newScore = newScore;
    }

    public Player getPlayer() { return player; }
    public GameTeam getScoringTeam() { return scoringTeam; }
    public GameTeam getEnteredPortalTeam() { return enteredPortalTeam; }
    public int getNewScore() { return newScore; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
