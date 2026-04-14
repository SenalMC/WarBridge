package top.cnuo.warbridge.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import top.cnuo.warbridge.game.GameTeam;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class WarbridgeGameEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final GameTeam winner;
    private final String winnerName;
    private final Map<String, Integer> finalScores;

    public WarbridgeGameEndEvent(GameTeam winner, String winnerName, Map<String, Integer> finalScores) {
        this.winner = winner;
        this.winnerName = winnerName;
        this.finalScores = Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(finalScores));
    }

    public GameTeam getWinner() { return winner; }
    public String getWinnerName() { return winnerName; }
    public Map<String, Integer> getFinalScores() { return finalScores; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
