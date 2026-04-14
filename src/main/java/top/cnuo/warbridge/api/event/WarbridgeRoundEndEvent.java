package top.cnuo.warbridge.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import top.cnuo.warbridge.game.GameTeam;

public class WarbridgeRoundEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final int roundNumber;
    private final Player scorer;
    private final GameTeam scoringTeam;

    public WarbridgeRoundEndEvent(int roundNumber, Player scorer, GameTeam scoringTeam) {
        this.roundNumber = roundNumber;
        this.scorer = scorer;
        this.scoringTeam = scoringTeam;
    }

    public int getRoundNumber() { return roundNumber; }
    public Player getScorer() { return scorer; }
    public GameTeam getScoringTeam() { return scoringTeam; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
