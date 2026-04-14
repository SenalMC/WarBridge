package top.cnuo.warbridge.api;

import org.bukkit.entity.Player;
import top.cnuo.warbridge.WarbridgePlugin;
import top.cnuo.warbridge.game.GameSession;
import top.cnuo.warbridge.game.GameState;
import top.cnuo.warbridge.game.GameTeam;
import top.cnuo.warbridge.game.PlayerStats;
import top.cnuo.warbridge.game.RoundState;
import top.cnuo.warbridge.game.TeamColor;

import java.util.Collections;
import java.util.Map;

public class SimpleWarbridgeApi implements WarbridgeApi {
    private final WarbridgePlugin plugin;

    public SimpleWarbridgeApi(WarbridgePlugin plugin) {
        this.plugin = plugin;
    }

    private GameSession session() {
        return plugin.getGameSession();
    }

    @Override
    public GameState getState() { return session().getState(); }

    @Override
    public RoundState getRoundState() { return session().getRoundState(); }

    @Override
    public boolean isInGame(Player player) {
        return player != null && session().getPlayerTeam(player.getUniqueId()) != null && !session().isSpectator(player.getUniqueId());
    }

    @Override
    public boolean isSpectator(Player player) { return player != null && session().isSpectator(player.getUniqueId()); }

    @Override
    public boolean isRespawning(Player player) { return player != null && session().isRespawning(player.getUniqueId()); }

    @Override
    public TeamColor getPlayerTeam(Player player) { return player == null ? null : session().getPlayerTeam(player.getUniqueId()); }

    @Override
    public GameTeam getPlayerTeamObject(Player player) { return player == null ? null : session().getPlayerTeamObject(player.getUniqueId()); }

    @Override
    public PlayerStats getPlayerStats(Player player) { return player == null ? null : session().getStats(player.getUniqueId()); }

    @Override
    public int getPlayersCount() { return session().getPlayersCount(); }

    @Override
    public int getActivePlayersCount() { return session().getActivePlayersCount(); }

    @Override
    public int getRoundNumber() { return session().getRoundNumber(); }

    @Override
    public int getCountdown() { return session().getCountdown(); }

    @Override
    public int getGameSeconds() { return session().getGameSeconds(); }

    @Override
    public String getWinnerName() { return session().getWinnerName(); }

    @Override
    public Map<TeamColor, GameTeam> getTeams() { return Collections.unmodifiableMap(session().getTeams()); }
}
