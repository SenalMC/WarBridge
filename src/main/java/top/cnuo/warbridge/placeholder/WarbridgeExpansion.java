package top.cnuo.warbridge.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import top.cnuo.warbridge.WarbridgePlugin;

public class WarbridgeExpansion extends PlaceholderExpansion {
    private final WarbridgePlugin plugin;

    public WarbridgeExpansion(WarbridgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() { return "warbridge"; }

    @Override
    public String getAuthor() { return "Chirnuo"; }

    @Override
    public String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        return plugin.getPlaceholderService().apply(player, "%warbridge_" + params + "%");
    }
}
