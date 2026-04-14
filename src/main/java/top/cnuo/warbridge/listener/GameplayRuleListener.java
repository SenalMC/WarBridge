package top.cnuo.warbridge.listener;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import top.cnuo.warbridge.WarbridgePlugin;
import top.cnuo.warbridge.game.GameState;
import top.cnuo.warbridge.game.RoundState;

public class GameplayRuleListener implements Listener {
    private final WarbridgePlugin plugin;

    public GameplayRuleListener(WarbridgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (plugin.getGameSession().getState() == GameState.EDIT) return;
        if (!plugin.getFileManager().get("game.yml").getBoolean("hunger.enabled", false)) {
            event.setCancelled(true);
            ((Player) event.getEntity()).setFoodLevel(20);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.getGameSession().getState() == GameState.EDIT && event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        if (!plugin.getFileManager().get("game.yml").getBoolean("item-drop.enabled", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        if (plugin.getGameSession().getState() == GameState.EDIT && event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        if (!plugin.getFileManager().get("game.yml").getBoolean("item-pickup.enabled", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (plugin.getGameSession().getState() == GameState.EDIT && player.getGameMode() == GameMode.CREATIVE) return;
        if (!plugin.getFileManager().get("game.yml").getBoolean("inventory.rearrange-enabled", false)) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
                event.setCancelled(true);
            }
            if (event.isShiftClick()) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (plugin.getGameSession().getState() == GameState.EDIT && player.getGameMode() == GameMode.CREATIVE) return;
        if (!plugin.getFileManager().get("game.yml").getBoolean("inventory.rearrange-enabled", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGameSession().getState() == GameState.EDIT && player.getGameMode() == GameMode.CREATIVE) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (event.getItem() != null && event.getItem().getType() == Material.EMERALD && plugin.getGameSession().isTeamSelectionStage()) return;
        FileConfiguration game = plugin.getFileManager().get("game.yml");
        Material type = block.getType();
        if (isContainer(type) && !game.getBoolean("interaction.chest", false)) {
            event.setCancelled(true); return;
        }
        if (isLever(type) && !game.getBoolean("interaction.lever", false)) {
            event.setCancelled(true); return;
        }
        if (isButton(type) && !game.getBoolean("interaction.button", false)) {
            event.setCancelled(true); return;
        }
        if (isDoor(type) && !game.getBoolean("interaction.door", false)) {
            event.setCancelled(true); return;
        }
        if (isPressurePlate(type) && !game.getBoolean("interaction.pressure-plate", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEnvironmentDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (plugin.getGameSession().getState() == GameState.EDIT && player.getGameMode() == GameMode.CREATIVE) return;
        if (plugin.getGameSession().getState() != GameState.GAMING || plugin.getGameSession().getRoundState() != RoundState.RUNNING) {
            return;
        }
        FileConfiguration game = plugin.getFileManager().get("game.yml");
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.FALL && !game.getBoolean("damage.fall", false)) {
            event.setCancelled(true);
        } else if ((cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK) && !game.getBoolean("damage.fire", false)) {
            event.setCancelled(true);
        } else if ((cause == EntityDamageEvent.DamageCause.LAVA) && !game.getBoolean("damage.lava", false)) {
            event.setCancelled(true);
        }
    }

    private boolean isContainer(Material material) {
        String name = material.name();
        return name.contains("CHEST") || name.contains("FURNACE") || name.contains("DISPENSER") || name.contains("DROPPER") || name.contains("HOPPER") || material == Material.WORKBENCH || material == Material.ENCHANTMENT_TABLE || material == Material.ANVIL;
    }

    private boolean isLever(Material material) { return material == Material.LEVER; }
    private boolean isButton(Material material) { return material == Material.STONE_BUTTON || material == Material.WOOD_BUTTON; }
    private boolean isDoor(Material material) {
        String name = material.name();
        return name.endsWith("_DOOR") || material == Material.WOODEN_DOOR || material == Material.IRON_DOOR_BLOCK;
    }
    private boolean isPressurePlate(Material material) {
        return material == Material.STONE_PLATE || material == Material.WOOD_PLATE || material == Material.GOLD_PLATE || material == Material.IRON_PLATE;
    }
}
