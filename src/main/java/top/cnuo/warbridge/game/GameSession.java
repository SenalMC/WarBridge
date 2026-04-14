package top.cnuo.warbridge.game;

import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import top.cnuo.warbridge.WarbridgePlugin;
import top.cnuo.warbridge.api.event.WarbridgeGameEndEvent;
import top.cnuo.warbridge.api.event.WarbridgeGameStartEvent;
import top.cnuo.warbridge.api.event.WarbridgePlayerDeathEvent;
import top.cnuo.warbridge.api.event.WarbridgePlayerGoalEvent;
import top.cnuo.warbridge.api.event.WarbridgePlayerJoinGameEvent;
import top.cnuo.warbridge.api.event.WarbridgePlayerKillEvent;
import top.cnuo.warbridge.api.event.WarbridgePlayerLeaveGameEvent;
import top.cnuo.warbridge.api.event.WarbridgePlayerRespawnEvent;
import top.cnuo.warbridge.api.event.WarbridgeRoundEndEvent;
import top.cnuo.warbridge.api.event.WarbridgeRoundStartEvent;
import top.cnuo.warbridge.api.event.WarbridgeStateChangeEvent;
import top.cnuo.warbridge.util.BungeeUtil;
import top.cnuo.warbridge.util.Cuboid;
import top.cnuo.warbridge.util.Loc;
import top.cnuo.warbridge.util.TitleUtil;
import top.cnuo.warbridge.util.ActionBarUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class GameSession {
    private final WarbridgePlugin plugin;
    private final Map<TeamColor, GameTeam> teams = new LinkedHashMap<TeamColor, GameTeam>();
    private final Map<UUID, PlayerStats> stats = new HashMap<UUID, PlayerStats>();
    private final Map<UUID, TeamColor> playerTeams = new HashMap<UUID, TeamColor>();
    private final Map<LocationKey, BlockStateSnapshot> placedBlocks = new HashMap<LocationKey, BlockStateSnapshot>();
    private final Map<UUID, UUID> lastDamager = new HashMap<UUID, UUID>();
    private final Map<UUID, Long> lastDamageTime = new HashMap<UUID, Long>();
    private final Set<UUID> spectators = new HashSet<UUID>();
    private final Set<UUID> respawning = new HashSet<UUID>();
    private final Random random = new Random();

    private GameState state;
    private RoundState roundState = RoundState.NONE;
    private BukkitTask heartbeat;
    private Location lobbySpawn;
    private Location spectatorSpawn;
    private Cuboid gameRegion;
    private int countdown;
    private int roundCountdown;
    private int gameoverCountdown;
    private int gameSeconds;
    private int roundNumber;
    private String winnerName = "None";
    private Location cachedGamePos1;
    private BukkitTask emptyTeamTask;
    private String emptyTeamPendingWinner;

    public GameSession(WarbridgePlugin plugin) {
        this.plugin = plugin;
        loadState();
    }

    private void resetAllMatchStats() {
        for (PlayerStats stat : stats.values()) {
            stat.resetMatch();
        }
    }

    private void clearRuntimeEntities() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getWorld().getEntities();
        }
        java.util.Set<org.bukkit.World> worlds = new java.util.HashSet<org.bukkit.World>();
        for (Player player : Bukkit.getOnlinePlayers()) worlds.add(player.getWorld());
        if (lobbySpawn != null && lobbySpawn.getWorld() != null) worlds.add(lobbySpawn.getWorld());
        for (org.bukkit.World world : worlds) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Arrow || entity instanceof Item) entity.remove();
            }
        }
    }

    private void cancelEmptyTeamTask() {
        if (emptyTeamTask != null) { emptyTeamTask.cancel(); emptyTeamTask = null; }
        emptyTeamPendingWinner = null;
    }
    public void setTeam(TeamColor color, String displayName) {
        if (color == null) {
            return;
        }

        GameTeam team = teams.get(color);
        if (team == null) {
            team = new GameTeam(color, displayName);
            teams.put(color, team);
        } else {
            team.setDisplayName(displayName);
        }
    }


    public void loadState() {
        teams.clear();
        spectators.clear();
        respawning.clear();
        playerTeams.clear();
        placedBlocks.clear();
        FileConfiguration map = plugin.getFileManager().get("map.yml");
        this.state = GameState.valueOf(plugin.getConfig().getString("state-on-startup", "PENDING").toUpperCase(Locale.ENGLISH));
        this.lobbySpawn = Loc.deserialize(map.getString("lobby.spawn"));
        this.spectatorSpawn = Loc.deserialize(map.getString("spectator.spawn"));
        this.cachedGamePos1 = Loc.deserialize(map.getString("regions.game-pos1"));
        Location p2 = Loc.deserialize(map.getString("regions.game-pos2"));
        if (cachedGamePos1 != null && p2 != null) this.gameRegion = new Cuboid(cachedGamePos1, p2);

        ConfigurationSection teamsSec = map.getConfigurationSection("teams");
        if (teamsSec != null) {
            for (String key : teamsSec.getKeys(false)) {
                TeamColor color = TeamColor.fromId(key);
                if (color == null) continue;
                ConfigurationSection sec = teamsSec.getConfigurationSection(key);
                GameTeam team = new GameTeam(color, sec.getString("name", key));
                team.setSpawn(Loc.deserialize(sec.getString("spawn")));
                Location cage1 = Loc.deserialize(sec.getString("cage-pos1"));
                Location cage2 = Loc.deserialize(sec.getString("cage-pos2"));
                if (cage1 != null && cage2 != null) team.setCage(new Cuboid(cage1, cage2));
                Location portal1 = Loc.deserialize(sec.getString("portal-pos1"));
                Location portal2 = Loc.deserialize(sec.getString("portal-pos2"));
                if (portal1 != null && portal2 != null) team.setPortal(new Cuboid(portal1, portal2));
                teams.put(color, team);
            }
        }
        resetScores();
        resetAllMatchStats();
        roundState = RoundState.NONE;
        winnerName = "None";
    }

    public void startHeartbeat() {
        if (heartbeat != null) heartbeat.cancel();
        heartbeat = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, 20L, 20L);
    }
    public TeamColor getSmallestTeamColor() {
        if (teams == null || teams.isEmpty()) {
            return null;
        }

        TeamColor bestColor = null;
        int bestSize = Integer.MAX_VALUE;

        for (TeamColor color : TeamColor.values()) {
            GameTeam team = teams.get(color);
            if (team == null) {
                continue;
            }

            int size = team.getPlayers() == null ? 0 : team.getPlayers().size();

            if (bestColor == null || size < bestSize) {
                bestColor = color;
                bestSize = size;
            }
        }

        return bestColor;
    }
    public void shutdown() {
        if (heartbeat != null) heartbeat.cancel();
        clearSpectators();
        respawning.clear();
        cleanupPlacedBlocks();
        clearRuntimeEntities();
        for (GameTeam team : teams.values()) team.clearCage();
        cancelEmptyTeamTask();
    }

    private void tick() {
        awardPlaytime();
        switch (state) {
            case PENDING:
                tickPending();
                break;
            case HOLDING:
                tickHolding();
                break;
            case GAMING:
                tickGaming();
                break;
            case GAMEOVER:
                tickGameOver();
                break;
            case EDIT:
            default:
                break;
        }
    }

    private void awardPlaytime() {
        int exp = plugin.getFileManager().get("game.yml").getInt("rewards.playtime-exp-per-tick", 0);
        int coins = plugin.getFileManager().get("game.yml").getInt("rewards.playtime-coins-per-tick", 0);
        if (exp == 0 && coins == 0) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isSpectator(player.getUniqueId())) continue;
            PlayerStats stat = getStats(player.getUniqueId());
            stat.addExp(exp);
            stat.addCoins(coins);
        }
    }

    private void tickPending() {
        ensureLobbySelectorItems();
        if (canStart() && getActivePlayersCount() >= plugin.getFileManager().get("game.yml").getInt("min-players", 2)) {
            setState(GameState.HOLDING);
            countdown = plugin.getFileManager().get("game.yml").getInt("countdown-seconds", 30);
            plugin.broadcast("countdown-start", Collections.singletonMap("%seconds%", String.valueOf(countdown)));
        }
    }

    private void tickHolding() {
        ensureLobbySelectorItems();
        int minPlayers = plugin.getFileManager().get("game.yml").getInt("min-players", 2);
        int maxPlayers = plugin.getFileManager().get("game.yml").getInt("max-players", 8);
        if (getActivePlayersCount() < minPlayers) {
            setState(GameState.PENDING);
            plugin.broadcast("not-enough-players", null);
            return;
        }
        if (getActivePlayersCount() >= maxPlayers && countdown > plugin.getFileManager().get("game.yml").getInt("full-countdown-seconds", 10)) {
            countdown = plugin.getFileManager().get("game.yml").getInt("full-countdown-seconds", 10);
            plugin.broadcast("countdown-full", null);
        }
        if (countdown <= 5 && countdown > 0) {
            Sound sound = getCountdownSound();
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), sound, 1F, 1F);
            }
        }
        if (countdown <= 0) {
            startGame();
            return;
        }
        countdown--;
    }

    private void tickGaming() {
        gameSeconds++;
        int limit = plugin.getFileManager().get("game.yml").getInt("time-limit-seconds", 600);
        if (limit > 0 && gameSeconds >= limit) {
            finishGame(findLeader());
            return;
        }
        checkEmptyTeams();
        if (roundState == RoundState.PREPARE || roundState == RoundState.ENDING) {
            if (roundCountdown <= 0) {
                if (roundState == RoundState.PREPARE) {
                    for (GameTeam team : teams.values()) team.clearCage();
                    roundState = RoundState.RUNNING;
                    Bukkit.getPluginManager().callEvent(new WarbridgeRoundStartEvent(roundNumber));
                    plugin.broadcast("round-start", Collections.singletonMap("%round%", String.valueOf(roundNumber)));
                } else {
                    prepareNextRound();
                }
                return;
            }
            roundCountdown--;
        }
    }

    private void tickGameOver() {
        if (gameoverCountdown <= 0) {
            if (plugin.getConfig().getBoolean("send-fallback-on-gameover", true)) {
                String fallback = plugin.getConfig().getString("fallbackserver", "lobby");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(plugin.getMessages().get(player, "send-fallback"));
                    BungeeUtil.sendToServer(plugin, player, fallback);
                }
            }
            resetArenaForNextGame();
            setState(GameState.PENDING);
            return;
        }
        gameoverCountdown--;
    }


    public List<String> getStartCheckProblems() {
        List<String> problems = new ArrayList<String>();
        if (state == GameState.EDIT) {
            problems.add("服务器当前仍处于 EDIT 编辑模式");
        }
        if (teams.size() < 2) {
            problems.add("队伍数量不足，至少需要 2 个队伍");
        }
        if (lobbySpawn == null) {
            problems.add("未设置 lobby 候场出生点");
        }
        if (spectatorSpawn == null) {
            problems.add("未设置 spectator 观战出生点（不影响开局，但建议设置）");
        }
        if (gameRegion == null) {
            problems.add("未设置游戏区域 game-pos1 / game-pos2");
        }
        for (GameTeam team : teams.values()) {
            String teamName = team.getDisplayName() == null ? team.getColor().name() : team.getDisplayName();
            if (team.getSpawn() == null) problems.add("队伍 " + teamName + " 未设置出生点");
            if (team.getCage() == null) problems.add("队伍 " + teamName + " 未设置笼子区域");
            if (team.getPortal() == null) problems.add("队伍 " + teamName + " 未设置传送门区域");
        }

        int minPlayers = plugin.getFileManager().get("game.yml").getInt("min-players", 2);
        int maxPlayers = plugin.getFileManager().get("game.yml").getInt("max-players", 8);
        int active = getActivePlayersCount();
        if (active < minPlayers) {
            problems.add("有效玩家不足：当前 " + active + " / 最小需要 " + minPlayers);
        }
        if (active > maxPlayers) {
            problems.add("有效玩家超过最大人数：当前 " + active + " / 最大允许 " + maxPlayers);
        }
        return problems;
    }

    public List<String> getStartCheckSummary() {
        List<String> lines = new ArrayList<String>();
        lines.add("状态: " + state.name());
        lines.add("有效玩家: " + getActivePlayersCount() + " / 在线玩家: " + Bukkit.getOnlinePlayers().size());
        lines.add("观战人数: " + spectators.size());
        lines.add("队伍数量: " + teams.size());
        lines.add("最小人数: " + plugin.getFileManager().get("game.yml").getInt("min-players", 2));
        lines.add("最大人数: " + plugin.getFileManager().get("game.yml").getInt("max-players", 8));
        lines.add("Lobby: " + (lobbySpawn != null ? "已设置" : "未设置"));
        lines.add("Spectator: " + (spectatorSpawn != null ? "已设置" : "未设置"));
        lines.add("GameRegion: " + (gameRegion != null ? "已设置" : "未设置"));
        for (GameTeam team : teams.values()) {
            String teamName = team.getDisplayName() == null ? team.getColor().name() : team.getDisplayName();
            lines.add("队伍 " + teamName + ": spawn=" + (team.getSpawn() != null ? "√" : "×")
                    + ", cage=" + (team.getCage() != null ? "√" : "×")
                    + ", portal=" + (team.getPortal() != null ? "√" : "×")
                    + ", effective=" + getEffectiveTeamPlayers(team.getColor()));
        }
        return lines;
    }
    private void setState(GameState newState) {
        if (newState == null) return;
        if (this.state != newState) {
            GameState old = this.state;
            this.state = newState;
            if (old != null) {
                Bukkit.getPluginManager().callEvent(new WarbridgeStateChangeEvent(old, newState));
            }
        } else {
            this.state = newState;
        }
    }

    public boolean canStart() {
        if (teams.size() < 2 || lobbySpawn == null || gameRegion == null) return false;
        for (GameTeam team : teams.values()) {
            if (team.getSpawn() == null || team.getCage() == null || team.getPortal() == null) return false;
        }
        return true;
    }

    private void startGame() {
        setState(GameState.GAMING);
        gameSeconds = 0;
        roundNumber = 0;
        winnerName = "None";
        clearSpectators();
        resetScores();
        resetAllMatchStats();
        cancelEmptyTeamTask();
        assignTeams();
        Bukkit.getPluginManager().callEvent(new WarbridgeGameStartEvent(getActivePlayersCount(), plugin.getFileManager().get("game.yml").getInt("points", 5)));
        prepareNextRound();
    }

    public void prepareNextRound() {
        roundNumber++;
        cleanupPlacedBlocks();
        clearRuntimeEntities();
        roundState = RoundState.PREPARE;
        roundCountdown = plugin.getFileManager().get("game.yml").getInt("prepare-seconds", 5);
        Material cageMaterial = Material.matchMaterial(plugin.getFileManager().get("game.yml").getString("cage-material", "GLASS"));
        if (cageMaterial == null) cageMaterial = Material.GLASS;
        for (GameTeam team : teams.values()) {
            team.spawnCage(cageMaterial);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isSpectator(player.getUniqueId())) continue;
            TeamColor color = playerTeams.get(player.getUniqueId());
            if (color == null) continue;
            GameTeam team = teams.get(color);
            if (team == null) continue;
            resetPlayer(player);
            if (team.getSpawn() != null) player.teleport(team.getSpawn());
            giveKit(player, color);
        }
        plugin.broadcast("round-prepare", mapOf("%round%", String.valueOf(roundNumber), "%seconds%", String.valueOf(roundCountdown)));
    }

    public void score(Player scorer, GameTeam enteredPortal) {
        if (scorer == null || enteredPortal == null || state != GameState.GAMING || roundState != RoundState.RUNNING) return;
        TeamColor playerColor = playerTeams.get(scorer.getUniqueId());
        if (playerColor == null) return;
        GameTeam scoringTeam = teams.get(playerColor);
        if (scoringTeam == null) return;
        if (enteredPortal.getColor() == playerColor) {
            scorer.sendMessage(plugin.getMessages().get(scorer, "own-goal"));
            killPlayer(scorer, null, false);
            return;
        }
        scoringTeam.addScore();
        getStats(scorer.getUniqueId()).addGoal();
        Bukkit.getPluginManager().callEvent(new WarbridgePlayerGoalEvent(scorer, scoringTeam, enteredPortal, scoringTeam.getScore()));
        spawnFirework(scoringTeam.getSpawn(), scoringTeam.getColor());
        playConfiguredSoundToAll("goal");
        String goalAnnouncement = plugin.getMessages().getRandom(scorer, "announcements.goal", mapOf(
                "%player%", scorer.getName(),
                "%portal_team%", enteredPortal.getColor().getChatColor() + enteredPortal.getDisplayName(),
                "%scoring_team%", scoringTeam.getColor().getChatColor() + scoringTeam.getDisplayName(),
                "%points%", String.valueOf(scoringTeam.getScore())
        ));
        for (Player online : Bukkit.getOnlinePlayers()) online.sendMessage(goalAnnouncement);
        int target = plugin.getFileManager().get("game.yml").getInt("points", 5);
        if (scoringTeam.getScore() >= target) {
            finishGame(scoringTeam);
            return;
        }
        roundState = RoundState.ENDING;
        roundCountdown = plugin.getFileManager().get("game.yml").getInt("round-end-seconds", 5);
        Bukkit.getPluginManager().callEvent(new WarbridgeRoundEndEvent(roundNumber, scorer, scoringTeam));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isSpectator(player.getUniqueId())) continue;
            resetPlayer(player);
            TeamColor color = playerTeams.get(player.getUniqueId());
            if (color != null && teams.get(color) != null && teams.get(color).getSpawn() != null) {
                player.teleport(teams.get(color).getSpawn());
            }
        }
    }

    public void finishGame(GameTeam winner) {
        setState(GameState.GAMEOVER);
        roundState = RoundState.NONE;
        winnerName = winner == null ? "平局" : winner.getColor().getChatColor() + winner.getDisplayName();
        gameoverCountdown = plugin.getConfig().getInt("gameover-send-delay-seconds", 8);
        cleanupPlacedBlocks();
        clearRuntimeEntities();
        for (GameTeam team : teams.values()) team.clearCage();
        cancelEmptyTeamTask();
        for (Player player : Bukkit.getOnlinePlayers()) {
            resetPlayer(player);
        }
        Map<String, Integer> finalScores = new LinkedHashMap<String, Integer>();
        for (GameTeam team : teams.values()) {
            finalScores.put(team.getColor().getId(), team.getScore());
        }
        Bukkit.getPluginManager().callEvent(new WarbridgeGameEndEvent(winner, winnerName, finalScores));
        plugin.broadcast("gameover", Collections.singletonMap("%winner%", winnerName));
    }

    public void resetArenaForNextGame() {
        cleanupPlacedBlocks();
        clearSpectators();
        respawning.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            resetPlayer(player);
            playerTeams.remove(player.getUniqueId());
            if (lobbySpawn != null) player.teleport(lobbySpawn);
        }
        resetScores();
        resetAllMatchStats();
        roundState = RoundState.NONE;
        roundNumber = 0;
        gameSeconds = 0;
        countdown = 0;
        winnerName = "None";
    }

    private void assignTeams() {
        Map<UUID, TeamColor> selected = new HashMap<UUID, TeamColor>(playerTeams);
        for (GameTeam team : teams.values()) {
            team.getPlayers().clear();
        }
        playerTeams.clear();
        List<Player> players = new ArrayList<Player>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isSpectator(player.getUniqueId())) players.add(player);
        }
        Collections.shuffle(players, random);

        int ideal = plugin.getFileManager().get("game.yml").getInt("teamplayer", 2);
        List<Player> unassigned = new ArrayList<Player>();
        for (Player player : players) {
            TeamColor preferred = selected.get(player.getUniqueId());
            GameTeam team = preferred == null ? null : teams.get(preferred);
            if (team != null && (ideal <= 0 || team.size() < ideal)) {
                team.getPlayers().add(player.getUniqueId());
                playerTeams.put(player.getUniqueId(), preferred);
            } else {
                unassigned.add(player);
            }
        }

        List<GameTeam> teamList = new ArrayList<GameTeam>(teams.values());
        for (Player player : unassigned) {
            GameTeam target = findBestTeamForJoin(teamList, ideal);
            target.getPlayers().add(player.getUniqueId());
            playerTeams.put(player.getUniqueId(), target.getColor());
        }
    }

    private GameTeam findBestTeamForJoin(List<GameTeam> teamList, final int ideal) {
        List<GameTeam> available = new ArrayList<GameTeam>();
        if (ideal > 0) {
            for (GameTeam team : teamList) {
                if (team.size() < ideal) available.add(team);
            }
        }
        if (available.isEmpty()) available = teamList;
        return Collections.min(available, new Comparator<GameTeam>() {
            @Override
            public int compare(GameTeam o1, GameTeam o2) {
                return Integer.compare(o1.size(), o2.size());
            }
        });
    }


    public void registerPlacedBlock(Location location, Material previousType, byte previousData) {
        LocationKey key = new LocationKey(location);
        if (!placedBlocks.containsKey(key)) {
            placedBlocks.put(key, new BlockStateSnapshot(location, previousType, previousData));
        }
    }

    public boolean isPlacedBlock(Location location) {
        return placedBlocks.containsKey(new LocationKey(location));
    }

    public void removePlacedBlock(Location location) {
        placedBlocks.remove(new LocationKey(location));
    }

    public void cleanupPlacedBlocks() {
        for (BlockStateSnapshot snapshot : placedBlocks.values()) {
            snapshot.location.getBlock().setType(snapshot.previousType);
            snapshot.location.getBlock().setData(snapshot.previousData);
        }
        placedBlocks.clear();
    }

    public void killPlayer(final Player player, Player killer, boolean awardKill) {
        if (player == null) return;
        getStats(player.getUniqueId()).addDeath();
        plugin.getMessages().send(player, "death-message", mapOf("%seconds%", String.valueOf(plugin.getFileManager().get("game.yml").getInt("respawn-delay", 3))));
        playConfiguredSound(player, "death");
        if (awardKill && killer != null && !killer.equals(player)) {
            PlayerStats killerStats = getStats(killer.getUniqueId());
            killerStats.addKill();
            killerStats.addExp(plugin.getFileManager().get("game.yml").getInt("rewards.kill-exp", 0));
            killerStats.addCoins(plugin.getFileManager().get("game.yml").getInt("rewards.kill-coins", 0));
            String killAnnouncement = plugin.getMessages().getRandom(killer, "announcements.kill", mapOf("%killer%", killer.getName(), "%victim%", player.getName()));
            for (Player online : Bukkit.getOnlinePlayers()) online.sendMessage(killAnnouncement);
            playConfiguredSound(killer, "kill");
            Bukkit.getPluginManager().callEvent(new WarbridgePlayerKillEvent(player, killer, true));
        }
        final int delay = plugin.getFileManager().get("game.yml").getInt("respawn-delay", 3);
        Bukkit.getPluginManager().callEvent(new WarbridgePlayerDeathEvent(player, killer, delay));
        resetPlayer(player);
        respawning.add(player.getUniqueId());
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setCanPickupItems(false);
        player.setGameMode(GameMode.SPECTATOR);
        final TeamColor color = playerTeams.get(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline() || isSpectator(player.getUniqueId())) return;
                respawning.remove(player.getUniqueId());
                resetPlayer(player);
                GameTeam team = color == null ? null : teams.get(color);
                if (team != null && team.getSpawn() != null) {
                    player.teleport(team.getSpawn());
                    giveKit(player, color);
                    int protection = plugin.getFileManager().get("game.yml").getInt("respawn-protection-seconds", 0);
                    if (protection > 0) player.setNoDamageTicks(protection * 20);
                    playConfiguredSound(player, "respawn");
                    Bukkit.getPluginManager().callEvent(new WarbridgePlayerRespawnEvent(player, color));
                } else if (lobbySpawn != null) {
                    player.teleport(lobbySpawn);
                    Bukkit.getPluginManager().callEvent(new WarbridgePlayerRespawnEvent(player, color));
                }
            }
        }, delay * 20L);
    }

    private void playConfiguredSoundToAll(String key) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            playConfiguredSound(player, key);
        }
    }

    private void playConfiguredSound(Player player, String key) {
        if (player == null) return;
        FileConfiguration sounds = plugin.getFileManager().get("sounds.yml");
        if (!sounds.getBoolean(key + ".enabled", false)) return;
        String name = sounds.getString(key + ".sound", "LEVEL_UP");
        float volume = (float) sounds.getDouble(key + ".volume", 1.0D);
        float pitch = (float) sounds.getDouble(key + ".pitch", 1.0D);
        try {
            player.playSound(player.getLocation(), Sound.valueOf(name), volume, pitch);
        } catch (Exception ignored) {
        }
    }

    private void checkEmptyTeams() {
        if (!plugin.getFileManager().get("game.yml").getBoolean("disconnect-rule.enabled", true)) return;
        int aliveTeams = 0;
        GameTeam lastAlive = null;
        for (GameTeam team : teams.values()) {
            int count = getEffectiveTeamPlayers(team.getColor());
            if (count > 0) { aliveTeams++; lastAlive = team; }
        }
        if (aliveTeams == 0) {
            if (emptyTeamTask == null) {
                emptyTeamPendingWinner = null;
                plugin.broadcast("all-teams-empty", null);
                emptyTeamTask = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override public void run() { emptyTeamTask = null; finishGame(null); }
                }, plugin.getFileManager().get("game.yml").getInt("disconnect-rule.empty-team-end-delay", 15) * 20L);
            }
            return;
        }
        if (teams.size() <= 2) {
            if (aliveTeams == 1) {
                final GameTeam winner = lastAlive;
                if (emptyTeamTask == null) {
                    emptyTeamPendingWinner = winner == null ? null : winner.getColor().getId();
                    plugin.broadcast("team-empty-warning", mapOf("%team%", winner == null ? "None" : winner.getColor().getChatColor() + winner.getDisplayName(), "%seconds%", String.valueOf(plugin.getFileManager().get("game.yml").getInt("disconnect-rule.empty-team-end-delay", 15))));
                    emptyTeamTask = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override public void run() { emptyTeamTask = null; finishGame(winner); }
                    }, plugin.getFileManager().get("game.yml").getInt("disconnect-rule.empty-team-end-delay", 15) * 20L);
                }
            } else if (emptyTeamTask != null) {
                cancelEmptyTeamTask();
                plugin.broadcast("team-recovered", null);
            }
            return;
        }
        if (aliveTeams == 1) {
            final GameTeam winner = lastAlive;
            if (emptyTeamTask == null) {
                emptyTeamPendingWinner = winner == null ? null : winner.getColor().getId();
                plugin.broadcast("team-empty-warning", mapOf("%team%", winner == null ? "None" : winner.getColor().getChatColor() + winner.getDisplayName(), "%seconds%", String.valueOf(plugin.getFileManager().get("game.yml").getInt("disconnect-rule.empty-team-end-delay", 15))));
                emptyTeamTask = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override public void run() { emptyTeamTask = null; finishGame(winner); }
                }, plugin.getFileManager().get("game.yml").getInt("disconnect-rule.empty-team-end-delay", 15) * 20L);
            }
        } else if (emptyTeamTask != null) {
            cancelEmptyTeamTask();
            plugin.broadcast("team-recovered", null);
        }
    }

    public int getEffectiveTeamPlayers(TeamColor color) {
        GameTeam team = teams.get(color);
        if (team == null) return 0;
        int count = 0;
        for (UUID uuid : new java.util.HashSet<UUID>(team.getPlayers())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && !isSpectator(uuid)) count++;
        }
        return count;
    }

    public void prepareLobbyPlayer(Player player) {
        if (player == null) return;
        resetPlayer(player);
        if (lobbySpawn != null) player.teleport(lobbySpawn);
        giveTeamSelector(player);
    }

    public void ensureLobbySelectorItems() {
        if (!isTeamSelectionStage()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isSpectator(player.getUniqueId()) || isRespawning(player.getUniqueId())) continue;
            giveTeamSelector(player);
        }
    }

    public void giveTeamSelector(Player player) {
        if (player == null || !isTeamSelectionStage()) return;
        FileConfiguration menus = plugin.getFileManager().get("menus.yml");
        PlayerInventory inv = player.getInventory();
        ItemStack emerald = new ItemStack(Material.EMERALD, 1);
        org.bukkit.inventory.meta.ItemMeta meta = emerald.getItemMeta();
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', menus.getString("team-selector.item.name", "&a队伍选择器")));
        java.util.List<String> lore = new java.util.ArrayList<String>();
        for (String line : menus.getStringList("team-selector.item.lore")) {
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
        }
        TeamColor selected = playerTeams.get(player.getUniqueId());
        if (selected != null) lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', menus.getString("team-selector.item.selected-format", "&7当前队伍: %team%")
                .replace("%team%", selected.getChatColor() + getTeamDisplayName(selected))));
        meta.setLore(lore);
        emerald.setItemMeta(meta);
        inv.setItem(menus.getInt("team-selector.item.slot", 4), emerald);
        player.updateInventory();
    }

    public boolean isTeamSelectionStage() {
        return state == GameState.PENDING || state == GameState.HOLDING;
    }

    public boolean selectTeam(Player player, TeamColor color) {
        if (player == null || color == null || !isTeamSelectionStage() || isSpectator(player.getUniqueId())) return false;
        GameTeam team = teams.get(color);
        if (team == null) return false;
        int ideal = plugin.getFileManager().get("game.yml").getInt("teamplayer", 2);
        TeamColor old = playerTeams.get(player.getUniqueId());
        if (old == color) {
            player.sendMessage(plugin.getMessages().get(player, "team-selector-already"));
            return true;
        }
        if (ideal > 0 && getSelectedPlayersCount(color) >= ideal) {
            player.sendMessage(plugin.getMessages().get(player, "team-selector-full", mapOf("%team%", color.getChatColor() + team.getDisplayName())));
            return false;
        }
        if (old != null && teams.containsKey(old)) {
            teams.get(old).getPlayers().remove(player.getUniqueId());
        }
        playerTeams.put(player.getUniqueId(), color);
        team.getPlayers().add(player.getUniqueId());
        player.sendMessage(plugin.getMessages().get(player, "team-selector-joined", mapOf("%team%", color.getChatColor() + team.getDisplayName())));
        giveTeamSelector(player);
        return true;
    }

    public int getSelectedPlayersCount(TeamColor color) {
        GameTeam team = teams.get(color);
        return team == null ? 0 : team.size();
    }

    public int getRemainingSlots(TeamColor color) {
        int ideal = plugin.getFileManager().get("game.yml").getInt("teamplayer", 2);
        if (ideal <= 0) return Integer.MAX_VALUE;
        return Math.max(0, ideal - getSelectedPlayersCount(color));
    }

    public String getTeamDisplayName(TeamColor color) {
        GameTeam team = teams.get(color);
        return team == null ? color.getId() : team.getDisplayName();
    }

    public void resetPlayer(Player player) {
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setFallDistance(0F);
        player.setExp(0F);
        player.setLevel(0);
        player.setHealth(player.getMaxHealth());
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setCanPickupItems(true);
        player.setNoDamageTicks(0);
        player.setGameMode(GameMode.SURVIVAL);
        getStats(player.getUniqueId()).clearBowCooldown();
    }

    public void giveKit(Player player, TeamColor color) {
        if (player == null) return;
        FileConfiguration kits = plugin.getFileManager().get("kits.yml");
        PlayerInventory inv = player.getInventory();

        ItemStack helmet = buildArmorItem(kits, "default-kit.armor.helmet", color, "LEATHER_HELMET");
        ItemStack chest = buildArmorItem(kits, "default-kit.armor.chestplate", color, "LEATHER_CHESTPLATE");
        ItemStack leggings = buildArmorItem(kits, "default-kit.armor.leggings", color, "LEATHER_LEGGINGS");
        ItemStack boots = buildArmorItem(kits, "default-kit.armor.boots", color, "LEATHER_BOOTS");
        inv.setHelmet(helmet);
        inv.setChestplate(chest);
        inv.setLeggings(leggings);
        inv.setBoots(boots);

        ConfigurationSection itemsSection = kits.getConfigurationSection("default-kit.items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                String path = "default-kit.items." + key;
                Material material = Material.matchMaterial(kits.getString(path + ".material", "STONE_SWORD"));
                if (material == null) continue;
                int amount = kits.getInt(path + ".amount", 1);
                int slot = kits.getInt(path + ".slot", 0);
                ItemStack item = new ItemStack(material, amount);
                if (material == Material.WOOL) item.setDurability(color.getWoolData());
                applyConfiguredEnchants(item, kits.getConfigurationSection(path + ".enchants"));
                inv.setItem(slot, item);
            }
        }
        player.updateInventory();
    }

    private ItemStack buildArmorItem(FileConfiguration kits, String path, TeamColor color, String fallbackMaterial) {
        String materialName = kits.getString(path + ".material", kits.getString(path, fallbackMaterial));
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.matchMaterial(fallbackMaterial);
        if (material == null) material = Material.LEATHER_CHESTPLATE;
        ItemStack stack = new ItemStack(material);
        dyeLeather(stack, color);
        applyConfiguredEnchants(stack, kits.getConfigurationSection(path + ".enchants"));
        return stack;
    }

    private void dyeLeather(ItemStack stack, TeamColor color) {
        if (stack == null || color == null || !(stack.getItemMeta() instanceof LeatherArmorMeta)) return;
        LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
        meta.setColor(color.getLeatherColor());
        stack.setItemMeta(meta);
    }

    private void applyConfiguredEnchants(ItemStack stack, ConfigurationSection enchantSection) {
        if (stack == null || stack.getType() == Material.AIR || enchantSection == null) return;
        for (String enchantName : enchantSection.getKeys(false)) {
            Enchantment enchantment = Enchantment.getByName(enchantName.toUpperCase(Locale.ENGLISH));
            if (enchantment == null) continue;
            int level = enchantSection.getInt(enchantName, 1);
            stack.addUnsafeEnchantment(enchantment, level);
        }
    }

    public void recordDamage(Player victim, Player attacker) {
        if (victim == null || attacker == null) return;
        lastDamager.put(victim.getUniqueId(), attacker.getUniqueId());
        lastDamageTime.put(victim.getUniqueId(), System.currentTimeMillis());
    }

    public Player findRecentDamager(Player player) {
        UUID attackerId = lastDamager.get(player.getUniqueId());
        Long when = lastDamageTime.get(player.getUniqueId());
        if (attackerId == null || when == null) return null;
        int seconds = plugin.getFileManager().get("game.yml").getInt("last-damage-seconds", 8);
        if (System.currentTimeMillis() - when > seconds * 1000L) return null;
        return Bukkit.getPlayer(attackerId);
    }

    private void spawnFirework(Location location, TeamColor color) {
        if (location == null || location.getWorld() == null) return;
        Firework firework = location.getWorld().spawn(location.clone().add(0, 1, 0), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder().withColor(color.getLeatherColor()).with(FireworkEffect.Type.BALL_LARGE).trail(true).build());
        meta.setPower(0);
        firework.setFireworkMeta(meta);
    }

    private Sound getCountdownSound() {
        try {
            return Sound.valueOf(plugin.getFileManager().get("game.yml").getString("countdown-sound", "NOTE_PLING"));
        } catch (Exception ignored) {
            return Sound.NOTE_PLING;
        }
    }

    public void joinPlayer(Player player) {
        if (player == null) return;
        Bukkit.getPluginManager().callEvent(new WarbridgePlayerJoinGameEvent(player, state));
        if (state == GameState.EDIT && !player.hasPermission("warbridge.edit")) {
            BungeeUtil.sendToServer(plugin, player, plugin.getConfig().getString("fallbackserver", "lobby"));
            return;
        }
        if (state == GameState.GAMING) {
            if (plugin.getConfig().getBoolean("allow-spectator-join-when-gaming", true)) {
                setSpectator(player, true);
            } else {
                BungeeUtil.sendToServer(plugin, player, plugin.getConfig().getString("fallbackserver", "lobby"));
            }
            return;
        }
        if (state == GameState.GAMEOVER) {
            if (lobbySpawn != null) player.teleport(lobbySpawn);
            return;
        }
        setSpectator(player, false);
        prepareLobbyPlayer(player);
    }

    public void removePlayer(Player player) {
        if (player == null) return;
        boolean spectator = spectators.contains(player.getUniqueId());
        spectators.remove(player.getUniqueId());
        respawning.remove(player.getUniqueId());
        TeamColor color = playerTeams.remove(player.getUniqueId());
        Bukkit.getPluginManager().callEvent(new WarbridgePlayerLeaveGameEvent(player, color, spectator));
        if (color != null && teams.containsKey(color)) {
            teams.get(color).getPlayers().remove(player.getUniqueId());
        }
        if (state == GameState.GAMING) checkEmptyTeams();
    }

    public void setSpectator(Player player, boolean spectator) {
        if (player == null) return;
        if (spectator) {
            spectators.add(player.getUniqueId());
            playerTeams.remove(player.getUniqueId());
            for (GameTeam team : teams.values()) team.getPlayers().remove(player.getUniqueId());
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setCanPickupItems(false);
            player.setGameMode(GameMode.ADVENTURE);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, true, false));
            if (spectatorSpawn != null) player.teleport(spectatorSpawn);
            else if (lobbySpawn != null) player.teleport(lobbySpawn);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player)) {
                    online.hidePlayer(player);
                }
            }
            player.sendMessage(plugin.getMessages().get(player, "now-spectator"));
        } else {
            spectators.remove(player.getUniqueId());
            respawning.remove(player.getUniqueId());
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setCanPickupItems(true);
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.showPlayer(player);
            }
        }
    }

    private void clearSpectators() {
        for (UUID uuid : new HashSet<UUID>(spectators)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) setSpectator(player, false);
        }
        spectators.clear();
    }

    public void toggleEdit() {
        if (state == GameState.EDIT) {
            setState(GameState.PENDING);
        } else {
            forceEdit();
        }
    }

    public void forceEdit() {
        cleanupPlacedBlocks();
        clearRuntimeEntities();
        for (GameTeam team : teams.values()) team.clearCage();
        cancelEmptyTeamTask();
        roundState = RoundState.NONE;
        setState(GameState.EDIT);
        winnerName = "None";
        clearSpectators();
        respawning.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            resetPlayer(player);
            playerTeams.remove(player.getUniqueId());
            if (lobbySpawn != null) player.teleport(lobbySpawn);
        }
    }

    public void sendFallback(Player player) {
        if (player == null) return;
        player.sendMessage(plugin.getMessages().get(player, "leave-to-lobby"));
        BungeeUtil.sendToServer(plugin, player, plugin.getConfig().getString("fallbackserver", "lobby"));
    }

    public void saveMapConfig() throws IOException {
        FileConfiguration map = plugin.getFileManager().get("map.yml");
        map.set("world", lobbySpawn != null && lobbySpawn.getWorld() != null ? lobbySpawn.getWorld().getName() : (spectatorSpawn != null && spectatorSpawn.getWorld() != null ? spectatorSpawn.getWorld().getName() : "world"));
        map.set("lobby.spawn", Loc.serialize(lobbySpawn));
        map.set("spectator.spawn", Loc.serialize(spectatorSpawn));
        map.set("regions.game-pos1", gameRegion == null ? "" : Loc.serialize(gameRegion.getMin()));
        map.set("regions.game-pos2", gameRegion == null ? "" : Loc.serialize(gameRegion.getMax()));
        map.set("teams", null);
        for (GameTeam team : teams.values()) {
            String path = "teams." + team.getColor().getId();
            map.set(path + ".name", team.getDisplayName());
            map.set(path + ".spawn", Loc.serialize(team.getSpawn()));
            map.set(path + ".cage-pos1", team.getCage() == null ? "" : Loc.serialize(team.getCage().getMin()));
            map.set(path + ".cage-pos2", team.getCage() == null ? "" : Loc.serialize(team.getCage().getMax()));
            map.set(path + ".portal-pos1", team.getPortal() == null ? "" : Loc.serialize(team.getPortal().getMin()));
            map.set(path + ".portal-pos2", team.getPortal() == null ? "" : Loc.serialize(team.getPortal().getMax()));
        }
        plugin.getFileManager().save("map.yml");
    }

    public boolean isInGameRegion(Location location) {
        return gameRegion != null && gameRegion.contains(location);
    }

    public boolean isPortal(Location location) {
        for (GameTeam team : teams.values()) {
            if (team.getPortal() != null && team.getPortal().contains(location)) return true;
        }
        return false;
    }

    public GameTeam getPortalAt(Location location) {
        for (GameTeam team : teams.values()) {
            if (team.getPortal() != null && team.getPortal().contains(location)) return team;
        }
        return null;
    }

    public boolean isCage(Location location) {
        for (GameTeam team : teams.values()) {
            if (team.getCage() != null && team.getCage().contains(location)) return true;
        }
        return false;
    }

    public boolean isProtectedSpawn(Location location) {
        for (GameTeam team : teams.values()) {
            if (team.getSpawn() == null || location == null || team.getSpawn().getWorld() == null || !team.getSpawn().getWorld().equals(location.getWorld())) continue;
            if (team.getSpawn().distanceSquared(location) <= 9.0D) return true;
        }
        return false;
    }

    public void setLobbySpawn(Location lobbySpawn) { this.lobbySpawn = lobbySpawn == null ? null : lobbySpawn.clone(); }
    public void setSpectatorSpawn(Location spectatorSpawn) { this.spectatorSpawn = spectatorSpawn == null ? null : spectatorSpawn.clone(); }
    public void setGameRegion(Location pos1, Location pos2) {
        this.gameRegion = (pos1 == null || pos2 == null) ? null : new Cuboid(pos1, pos2);
        this.cachedGamePos1 = this.gameRegion == null ? null : this.gameRegion.getMin();
    }

    public int getCountdown() { return countdown; }
    public int getRoundNumber() { return roundNumber; }
    public int getGameSeconds() { return gameSeconds; }
    public String getWinnerName() { return winnerName; }
    public int getGameoverCountdown() { return gameoverCountdown; }
    public PlayerStats getStats(UUID uuid) { if (!stats.containsKey(uuid)) stats.put(uuid, new PlayerStats()); return stats.get(uuid); }
    public TeamColor getPlayerTeam(UUID uuid) { return playerTeams.get(uuid); }
    public GameTeam getPlayerTeamObject(UUID uuid) { TeamColor color = playerTeams.get(uuid); return color == null ? null : teams.get(color); }
    public String getEmptyTeamPendingWinner() { return emptyTeamPendingWinner; }
    public int getPlayersCount() { return Bukkit.getOnlinePlayers().size(); }
    public int getActivePlayersCount() { return Bukkit.getOnlinePlayers().size() - spectators.size(); }
    public boolean isSpectator(UUID uuid) { return spectators.contains(uuid); }
    public boolean isRespawning(UUID uuid) { return respawning.contains(uuid); }
    public Location getLobbySpawn() { return lobbySpawn == null ? null : lobbySpawn.clone(); }
    public Location getSpectatorSpawn() { return spectatorSpawn == null ? null : spectatorSpawn.clone(); }


    public GameState getState() { return state; }
    public RoundState getRoundState() { return roundState; }
    public java.util.Map<TeamColor, GameTeam> getTeams() { return teams; }

    public void setPoints(int points) {
        plugin.getFileManager().get("game.yml").set("points", points);
    }

    public void setMinPlayers(int minPlayers) {
        plugin.getFileManager().get("game.yml").set("min-players", minPlayers);
    }

    public void setMaxPlayers(int maxPlayers) {
        plugin.getFileManager().get("game.yml").set("max-players", maxPlayers);
    }

    public void setTeamPlayers(int teamPlayers) {
        plugin.getFileManager().get("game.yml").set("teamplayer", teamPlayers);
    }

    private void resetScores() {
        for (GameTeam team : teams.values()) team.setScore(0);
    }

    private GameTeam findLeader() {
        GameTeam leader = null;
        for (GameTeam team : teams.values()) {
            if (leader == null || team.getScore() > leader.getScore()) leader = team;
        }
        return leader;
    }

    private Map<String, String> mapOf(String... values) {
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i + 1 < values.length; i += 2) map.put(values[i], values[i + 1]);
        return map;
    }

    private static final class LocationKey {
        private final String world;
        private final int x;
        private final int y;
        private final int z;

        private LocationKey(Location location) {
            this.world = location.getWorld() == null ? "" : location.getWorld().getName();
            this.x = location.getBlockX();
            this.y = location.getBlockY();
            this.z = location.getBlockZ();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LocationKey)) return false;
            LocationKey key = (LocationKey) o;
            return x == key.x && y == key.y && z == key.z && world.equals(key.world);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new Object[]{world, x, y, z});
        }
    }

    private static final class BlockStateSnapshot {
        private final Location location;
        private final Material previousType;
        private final byte previousData;

        private BlockStateSnapshot(Location location, Material previousType, byte previousData) {
            this.location = location;
            this.previousType = previousType;
            this.previousData = previousData;
        }
    }
}
