package top.cnuo.warbridge.api;

import org.bukkit.entity.Player;
import top.cnuo.warbridge.game.GameState;
import top.cnuo.warbridge.game.GameTeam;
import top.cnuo.warbridge.game.PlayerStats;
import top.cnuo.warbridge.game.RoundState;
import top.cnuo.warbridge.game.TeamColor;

import java.util.Map;

public interface WarbridgeApi {
    GameState getState();
    RoundState getRoundState();
    boolean isInGame(Player player);
    boolean isSpectator(Player player);
    boolean isRespawning(Player player);
    TeamColor getPlayerTeam(Player player);
    GameTeam getPlayerTeamObject(Player player);
    PlayerStats getPlayerStats(Player player);
    int getPlayersCount();
    int getActivePlayersCount();
    int getRoundNumber();
    int getCountdown();
    int getGameSeconds();
    String getWinnerName();
    Map<TeamColor, GameTeam> getTeams();
}
