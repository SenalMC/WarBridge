package top.cnuo.warbridge;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import top.cnuo.warbridge.api.SimpleWarbridgeApi;
import top.cnuo.warbridge.api.WarbridgeApi;
import top.cnuo.warbridge.command.WarbridgeCommand;
import top.cnuo.warbridge.config.FileManager;
import top.cnuo.warbridge.config.MessageManager;
import top.cnuo.warbridge.game.GameSession;
import top.cnuo.warbridge.listener.BlockListener;
import top.cnuo.warbridge.listener.BowListener;
import top.cnuo.warbridge.listener.CombatListener;
import top.cnuo.warbridge.listener.PlayerConnectionListener;
import top.cnuo.warbridge.listener.GameplayRuleListener;
import top.cnuo.warbridge.listener.PlayerMoveListener;
import top.cnuo.warbridge.listener.ServerPingListener;
import top.cnuo.warbridge.listener.TeamSelectorListener;
import top.cnuo.warbridge.placeholder.PlaceholderService;
import top.cnuo.warbridge.placeholder.WarbridgeExpansion;
import top.cnuo.warbridge.scoreboard.BoardManager;

import java.util.Map;

public class WarbridgePlugin extends JavaPlugin {
    private FileManager fileManager;
    private MessageManager messages;
    private PlaceholderService placeholderService;
    private GameSession gameSession;
    private BoardManager boardManager;
    private WarbridgeApi api;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        fileManager = new FileManager(this);
        fileManager.saveDefaults();
        api = new SimpleWarbridgeApi(this);
        reloadPlugin();

        WarbridgeCommand command = new WarbridgeCommand(this);
        getCommand("warbridge").setExecutor(command);
        getCommand("warbridge").setTabCompleter(command);

        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BowListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GameplayRuleListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ServerPingListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TeamSelectorListener(this), this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new WarbridgeExpansion(this).register();
            getLogger().info("Hooked PlaceholderAPI.");
        }

        getLogger().info("[Warbridge]Desgin by Chirnuo https://github.com/SenalMC/Warbridge");
        gameSession.startHeartbeat();
    }

    @Override
    public void onDisable() {
        getLogger().info("[Warbridge]Desgin by Chirnuo https://github.com/SenalMC/Warbridge");
        Bukkit.getServicesManager().unregisterAll(this);
        if (boardManager != null) boardManager.shutdown();
        if (gameSession != null) gameSession.shutdown();
    }

    public void reloadPlugin() {
        reloadConfig();
        fileManager.reloadAll();
        messages = new MessageManager(this);
        placeholderService = new PlaceholderService(this);
        if (gameSession != null) gameSession.shutdown();
        gameSession = new GameSession(this);
        Bukkit.getServicesManager().unregisterAll(this);
        if (boardManager != null) boardManager.shutdown();
        boardManager = new BoardManager(this);
        boardManager.start();
        Bukkit.getServicesManager().register(WarbridgeApi.class, api, this, ServicePriority.Normal);
    }

    public void broadcast(String key, Map<String, String> replacements) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(messages.get(player, key, replacements));
        }
    }

    public FileManager getFileManager() { return fileManager; }
    public MessageManager getMessages() { return messages; }
    public PlaceholderService getPlaceholderService() { return placeholderService; }
    public GameSession getGameSession() { return gameSession; }
    public BoardManager getBoardManager() { return boardManager; }
    public WarbridgeApi getApi() { return api; }
    public FileConfiguration getNamedConfig(String name) { return fileManager.get(name); }
}
