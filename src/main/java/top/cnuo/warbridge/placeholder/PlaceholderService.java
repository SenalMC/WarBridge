package top.cnuo.warbridge.placeholder;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import top.cnuo.warbridge.WarbridgePlugin;
import top.cnuo.warbridge.game.GameSession;
import top.cnuo.warbridge.game.GameTeam;
import top.cnuo.warbridge.game.PlayerStats;
import top.cnuo.warbridge.game.TeamColor;
import top.cnuo.warbridge.util.PingUtil;
import top.cnuo.warbridge.util.Text;

public class PlaceholderService {
    private final WarbridgePlugin plugin;

    public PlaceholderService(WarbridgePlugin plugin) {
        this.plugin = plugin;
    }

    public String apply(Player player, String text) {
        if (text == null) return "";
        GameSession session = plugin.getGameSession();
        String result = text;
        result = result.replace("%warbridge_state%", session.getState().name());
        result = result.replace("%warbridge_server%", plugin.getConfig().getString("server-id", Bukkit.getServerName()));
        result = result.replace("%warbridge_players%", String.valueOf(session.getPlayersCount()));
        result = result.replace("%warbridge_max_players%", String.valueOf(plugin.getFileManager().get("game.yml").getInt("max-players", 8)));
        result = result.replace("%warbridge_min_players%", String.valueOf(plugin.getFileManager().get("game.yml").getInt("min-players", 2)));
        result = result.replace("%warbridge_countdown%", String.valueOf(session.getCountdown()));
        result = result.replace("%warbridge_round_state%", session.getRoundState().name());
        result = result.replace("%warbridge_round_number%", String.valueOf(session.getRoundNumber()));
        result = result.replace("%warbridge_points_to_win%", String.valueOf(plugin.getFileManager().get("game.yml").getInt("points", 5)));
        result = result.replace("%warbridge_winner%", session.getWinnerName());
        result = result.replace("%warbridge_team_count%", String.valueOf(session.getTeams().size()));
        result = result.replace("%warbridge_game_time_left%", String.valueOf(Math.max(0, plugin.getFileManager().get("game.yml").getInt("time-limit-seconds", 600) - session.getGameSeconds())));
        result = result.replace("%warbridge_gameover_left%", String.valueOf(session.getGameoverCountdown()));
        if (player != null) {
            PlayerStats stats = session.getStats(player.getUniqueId());
            GameTeam team = session.getPlayerTeamObject(player.getUniqueId());
            TeamColor color = session.getPlayerTeam(player.getUniqueId());
            result = result.replace("%warbridge_player_name%", player.getName());
            result = result.replace("%warbridge_player_team%", team == null ? "None" : team.getColor().getChatColor() + team.getDisplayName());
            result = result.replace("%warbridge_player_team_name%", team == null ? "None" : team.getDisplayName());
            result = result.replace("%warbridge_player_team_color%", color == null ? "None" : color.getId());
            result = result.replace("%warbridge_player_kills%", String.valueOf(stats.getKills()));
            result = result.replace("%warbridge_player_deaths%", String.valueOf(stats.getDeaths()));
            result = result.replace("%warbridge_player_goals%", String.valueOf(stats.getGoals()));
            result = result.replace("%warbridge_player_coins%", String.valueOf(stats.getCoins()));
            result = result.replace("%warbridge_player_exp%", String.valueOf(stats.getExp()));
            result = result.replace("%warbridge_player_ping%", String.valueOf(PingUtil.getPing(player)));
            result = result.replace("%warbridge_player_is_alive%", String.valueOf(!session.isSpectator(player.getUniqueId())));
            result = result.replace("%warbridge_player_is_spectator%", String.valueOf(session.isSpectator(player.getUniqueId())));
            result = result.replace("%warbridge_player_arrows%", String.valueOf(countMaterial(player, Material.ARROW)));
            double cd = Math.max(0D, (stats.getBowCooldownUntil() - System.currentTimeMillis()) / 1000D);
            result = result.replace("%warbridge_bow_cooldown%", String.format("%.1f", cd));
            result = result.replace("%warbridge_bow_cooldown_formatted%", cd <= 0 ? "Ready" : String.format("%.1fs", cd));
        }
        for (TeamColor teamColor : TeamColor.values()) {
            GameTeam team = session.getTeams().get(teamColor);
            String prefix = "%warbridge_team_" + teamColor.getId() + "_";
            result = result.replace(prefix + "name%", team == null ? "None" : team.getDisplayName());
            result = result.replace(prefix + "score%", team == null ? "0" : String.valueOf(team.getScore()));
            result = result.replace(prefix + "players%", team == null ? "0" : String.valueOf(team.size()));
        }
        return Text.color(result);
    }

    private int countMaterial(Player player, Material material) {
        int total = 0;
        for (ItemStackWrapper wrapper : ItemStackWrapper.wrap(player.getInventory().getContents())) {
            if (wrapper.material == material) total += wrapper.amount;
        }
        return total;
    }

    private static class ItemStackWrapper {
        private final Material material;
        private final int amount;

        private ItemStackWrapper(Material material, int amount) {
            this.material = material;
            this.amount = amount;
        }

        private static java.util.List<ItemStackWrapper> wrap(org.bukkit.inventory.ItemStack[] items) {
            java.util.List<ItemStackWrapper> list = new java.util.ArrayList<ItemStackWrapper>();
            if (items == null) return list;
            for (org.bukkit.inventory.ItemStack item : items) {
                if (item != null) list.add(new ItemStackWrapper(item.getType(), item.getAmount()));
            }
            return list;
        }
    }
}
