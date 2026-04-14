package top.cnuo.warbridge.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import top.cnuo.warbridge.WarbridgePlugin;
import top.cnuo.warbridge.game.GameState;
import top.cnuo.warbridge.game.GameTeam;
import top.cnuo.warbridge.game.TeamColor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BoardManager {
    private final WarbridgePlugin plugin;
    private BukkitTask task;

    public BoardManager(WarbridgePlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int ticks = plugin.getFileManager().get("board.yml").getInt("update-ticks", 20);
        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                updateAll();
            }
        }, ticks, ticks);
    }

    public void shutdown() {
        if (task != null) task.cancel();
    }

    public void updateAll() {
        if (!plugin.getConfig().getBoolean("scoreboard", true)) return;
        FileConfiguration cfg = plugin.getFileManager().get("board.yml");
        if (!cfg.getBoolean("enabled", true)) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            apply(player);
        }
    }

    private void apply(Player player) {
        FileConfiguration cfg = plugin.getFileManager().get("board.yml");
        String stateKey = plugin.getGameSession().getState().name().toLowerCase(Locale.ENGLISH);
        if (plugin.getGameSession().getState() == GameState.GAMING && plugin.getGameSession().isSpectator(player.getUniqueId())) {
            stateKey = "gaming-spectator";
        }
        List<String> lines = expandDynamicLines(cfg.getStringList("boards." + stateKey), stateKey);
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("warbridge", "dummy");
        objective.setDisplayName(cut(plugin.getPlaceholderService().apply(player, cfg.getString("title", "&e&lWarbridge")), 32));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> unique = uniquify(lines, player);
        int score = unique.size();
        for (String line : unique) {
            objective.getScore(cut(line, 40)).setScore(score--);
        }
        player.setScoreboard(board);
    }

    private List<String> uniquify(List<String> lines, Player player) {
        List<String> output = new ArrayList<String>();
        Set<String> used = new HashSet<String>();
        int idx = 0;
        for (String raw : lines) {
            String line = plugin.getPlaceholderService().apply(player, raw);
            String candidate = line;
            while (used.contains(candidate)) {
                candidate = line + org.bukkit.ChatColor.values()[idx++ % org.bukkit.ChatColor.values().length];
            }
            used.add(candidate);
            output.add(candidate);
        }
        return output;
    }


    private List<String> expandDynamicLines(List<String> lines, String stateKey) {
        List<String> expanded = new ArrayList<String>();
        for (String line : lines) {
            if ("%warbridge_dynamic_teams%".equalsIgnoreCase(line)) {
                expanded.addAll(buildDynamicTeamLines(stateKey));
            } else {
                expanded.add(line);
            }
        }
        return expanded;
    }

    private List<String> buildDynamicTeamLines(String stateKey) {
        List<String> built = new ArrayList<String>();
        for (TeamColor color : TeamColor.values()) {
            GameTeam team = plugin.getGameSession().getTeams().get(color);
            if (team == null) continue;
            if ("holding".equalsIgnoreCase(stateKey) || "pending".equalsIgnoreCase(stateKey)) {
                built.add(color.getChatColor() + team.getDisplayName() + " &f" + team.size());
            } else {
                built.add(color.getChatColor() + team.getDisplayName() + " &f" + team.getScore());
            }
        }
        if (built.isEmpty()) built.add("&7No Teams");
        return built;
    }
    private String cut(String text, int max) {
        return text.length() > max ? text.substring(0, max) : text;
    }
}
