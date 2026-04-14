package top.cnuo.warbridge.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.cnuo.warbridge.WarbridgePlugin;
import top.cnuo.warbridge.game.GameSession;
import top.cnuo.warbridge.game.GameTeam;
import top.cnuo.warbridge.game.TeamColor;
import top.cnuo.warbridge.util.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TeamSelectorListener implements Listener {
    private final WarbridgePlugin plugin;

    public TeamSelectorListener(WarbridgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        GameSession session = plugin.getGameSession();
        if (!session.isTeamSelectionStage() || session.isSpectator(player.getUniqueId()) || session.isRespawning(player.getUniqueId())) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.EMERALD) return;
        if (!isSelectorItemSlot(player, item)) return;
        event.setCancelled(true);
        openMenu(player);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (event.getInventory() == null || event.getInventory().getType() != InventoryType.CHEST) return;
        String title = getMenuTitle();
        if (!title.equals(event.getInventory().getName())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;
        if (current.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        if (current.getType() == Material.NETHER_STAR) {
            TeamColor target = plugin.getGameSession().getSmallestTeamColor();
            if (target != null) plugin.getGameSession().selectTeam(player, target);
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() { @Override public void run() { if (player.isOnline()) openMenu(player); } }, 1L);
            return;
        }
        if (current.getType() != Material.WOOL) return;
        TeamColor clicked = null;
        short durability = current.getDurability();
        for (TeamColor color : TeamColor.values()) {
            if (color.getWoolData() == durability) {
                clicked = color;
                break;
            }
        }
        if (clicked == null) return;
        boolean ok = plugin.getGameSession().selectTeam(player, clicked);
        if (ok) {
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    if (player.isOnline()) openMenu(player);
                }
            }, 1L);
        }
    }

    private void openMenu(Player player) {
        GameSession session = plugin.getGameSession();
        FileConfiguration menus = plugin.getFileManager().get("menus.yml");
        Inventory inventory = Bukkit.createInventory(null, menus.getInt("team-selector.menu.size", 9), getMenuTitle());
        int[] slots = new int[]{1, 3, 5, 7};
        int idx = 0;
        for (Map.Entry<TeamColor, GameTeam> entry : session.getTeams().entrySet()) {
            if (idx >= slots.length) break;
            TeamColor color = entry.getKey();
            GameTeam team = entry.getValue();
            ItemStack wool = new ItemStack(Material.WOOL, 1, color.getWoolData());
            ItemMeta meta = wool.getItemMeta();
            String displayName = menus.getString("team-selector.team-item.name", "%team_color%%team_name%");
            displayName = displayName.replace("%team_name%", team.getDisplayName()).replace("%team_color%", color.getChatColor().toString());
            meta.setDisplayName(Text.color(displayName));
            List<String> lore = new ArrayList<String>();
            int current = session.getSelectedPlayersCount(color);
            int max = menus.getInt("team-selector.max-per-team-override", plugin.getFileManager().get("game.yml").getInt("teamplayer", 2));
            int remaining = session.getRemainingSlots(color);
            for (String line : menus.getStringList("team-selector.team-item.lore")) {
                lore.add(Text.color(line
                        .replace("%team_name%", team.getDisplayName())
                        .replace("%team_color%", color.getChatColor().toString())
                        .replace("%current%", String.valueOf(current))
                        .replace("%max%", max <= 0 ? "∞" : String.valueOf(max))
                        .replace("%remaining%", remaining == Integer.MAX_VALUE ? "∞" : String.valueOf(remaining))));
            }
            TeamColor selected = session.getPlayerTeam(player.getUniqueId());
            if (selected == color) {
                for (String line : menus.getStringList("team-selector.team-item.selected-lore")) lore.add(Text.color(line));
            } else if (remaining == 0) {
                for (String line : menus.getStringList("team-selector.team-item.full-lore")) lore.add(Text.color(line));
            } else {
                for (String line : menus.getStringList("team-selector.team-item.click-lore")) lore.add(Text.color(line));
            }
            meta.setLore(lore);
            wool.setItemMeta(meta);
            inventory.setItem(slots[idx], wool);
            idx++;
        }
        if (menus.getBoolean("team-selector.random.enabled", true)) {
            ItemStack random = new ItemStack(Material.NETHER_STAR);
            ItemMeta randomMeta = random.getItemMeta();
            randomMeta.setDisplayName(Text.color(menus.getString("team-selector.random.name", "&b随机分配")));
            randomMeta.setLore(colorList(menus.getStringList("team-selector.random.lore")));
            random.setItemMeta(randomMeta);
            inventory.setItem(menus.getInt("team-selector.random.slot", 0), random);
        }
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta meta = close.getItemMeta();
        meta.setDisplayName(Text.color(menus.getString("team-selector.close.name", "&c关闭")));
        meta.setLore(colorList(menus.getStringList("team-selector.close.lore")));
        close.setItemMeta(meta);
        inventory.setItem(menus.getInt("team-selector.close.slot", 8), close);
        player.openInventory(inventory);
    }

    private boolean isSelectorItemSlot(Player player, ItemStack item) {
        return player.getInventory().getHeldItemSlot() == plugin.getFileManager().get("menus.yml").getInt("team-selector.item.slot", 4);
    }

    private String getMenuTitle() {
        return Text.color(plugin.getFileManager().get("menus.yml").getString("team-selector.menu.title", "&8选择队伍"));
    }

    private List<String> colorList(List<String> lines) {
        List<String> out = new ArrayList<String>();
        for (String line : lines) out.add(Text.color(line));
        return out;
    }
}
