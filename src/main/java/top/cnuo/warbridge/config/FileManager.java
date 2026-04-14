package top.cnuo.warbridge.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.cnuo.warbridge.WarbridgePlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class FileManager {
    private static final String[] FILES = new String[]{"game.yml", "map.yml", "kits.yml", "board.yml", "messages.yml", "message_en.yml", "motd.yml", "sounds.yml", "menus.yml", "Help.yml", "Help_en.yml", "TECHNICAL_DOC.yml", "TECHNICAL_DOC_en.yml"};
    private final WarbridgePlugin plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<String, FileConfiguration>();

    public FileManager(WarbridgePlugin plugin) {
        this.plugin = plugin;
    }

    public void saveDefaults() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        for (String name : FILES) {
            File file = new File(plugin.getDataFolder(), name);
            if (file.exists()) continue;
            try {
                InputStream stream = plugin.getResource(name);
                if (stream != null) {
                    Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    file.createNewFile();
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to save default file: " + name, e);
            }
        }
    }

    public void reloadAll() {
        configs.clear();
        for (String name : FILES) {
            configs.put(name, YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), name)));
        }
    }

    public FileConfiguration get(String name) {
        FileConfiguration cfg = configs.get(name);
        if (cfg == null) {
            cfg = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), name));
            configs.put(name, cfg);
        }
        return cfg;
    }

    public void save(String name) throws IOException {
        get(name).save(new File(plugin.getDataFolder(), name));
    }
}
