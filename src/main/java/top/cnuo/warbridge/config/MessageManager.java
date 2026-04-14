package top.cnuo.warbridge.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import top.cnuo.warbridge.WarbridgePlugin;
import top.cnuo.warbridge.placeholder.PlaceholderService;
import top.cnuo.warbridge.util.Text;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class MessageManager {
    private final WarbridgePlugin plugin;
    private final Random random = new Random();

    public MessageManager(WarbridgePlugin plugin) {
        this.plugin = plugin;
    }

    public String get(String key) {
        return get(null, key);
    }

    public String get(Player player, String key) {
        FileConfiguration cfg = resolveLocaleConfig();
        String prefix = cfg.getString("prefix", "");
        String raw = cfg.getString(key, key);
        PlaceholderService placeholderService = plugin.getPlaceholderService();
        return Text.color(prefix + placeholderService.apply(player, raw));
    }

    public String get(Player player, String key, Map<String, String> replacements) {
        String msg = get(player, key);
        return applyReplacements(msg, replacements);
    }

    public String getRandom(Player player, String path, Map<String, String> replacements) {
        FileConfiguration cfg = resolveLocaleConfig();
        String prefix = cfg.getString("prefix", "");
        String raw = pickAnnouncement(player, cfg, path);
        raw = applyReplacements(raw, replacements);
        return Text.color(prefix + plugin.getPlaceholderService().apply(player, raw));
    }

    public void send(Player player, String key) {
        if (player == null) return;
        player.sendMessage(get(player, key));
    }

    public void send(Player player, String key, Map<String, String> replacements) {
        if (player == null) return;
        player.sendMessage(get(player, key, replacements));
    }

    public void sendRandom(Player player, String path, Map<String, String> replacements) {
        if (player == null) return;
        player.sendMessage(getRandom(player, path, replacements));
    }

    private FileConfiguration resolveLocaleConfig() {
        String locale = plugin.getConfig().getString("locale", "zh_CN");
        return "en_US".equalsIgnoreCase(locale) || "en".equalsIgnoreCase(locale)
                ? plugin.getFileManager().get("message_en.yml") : plugin.getFileManager().get("messages.yml");
    }

    private String applyReplacements(String msg, Map<String, String> replacements) {
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                msg = msg.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
            }
        }
        return msg;
    }

    private String pickAnnouncement(Player actor, FileConfiguration cfg, String path) {
        String special = pickSpecial(actor, cfg, path + ".special-permissions");
        if (special != null) return special;
        List<String> defaults = cfg.getStringList(path + ".default");
        if (defaults != null && !defaults.isEmpty()) return defaults.get(random.nextInt(defaults.size()));
        String fallback = cfg.getString(path, path);
        return fallback == null ? path : fallback;
    }

    private String pickSpecial(Player actor, FileConfiguration cfg, String path) {
        if (actor == null) return null;
        ConfigurationSection section = cfg.getConfigurationSection(path);
        if (section == null) return null;
        for (String permission : section.getKeys(false)) {
            if (!actor.hasPermission(permission)) continue;
            List<String> messages = section.getStringList(permission);
            if (messages != null && !messages.isEmpty()) return messages.get(random.nextInt(messages.size()));
        }
        return null;
    }
}
