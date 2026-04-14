package top.cnuo.warbridge.listener;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.scheduler.BukkitTask;
import top.cnuo.warbridge.WarbridgePlugin;
import top.cnuo.warbridge.game.PlayerStats;
import top.cnuo.warbridge.util.ActionBarUtil;
import top.cnuo.warbridge.util.Text;
import top.cnuo.warbridge.util.TitleUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class BowListener implements Listener {
    private final WarbridgePlugin plugin;
    private final Map<UUID, BukkitTask> displayTasks = new HashMap<UUID, BukkitTask>();

    public BowListener(WarbridgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        final Player player = (Player) event.getEntity();
        if (event.getBow() == null || event.getBow().getType() != Material.BOW) return;
        if (plugin.getGameSession().isSpectator(player.getUniqueId()) || plugin.getGameSession().isRespawning(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        PlayerStats stats = plugin.getGameSession().getStats(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (stats.getBowCooldownUntil() > now) {
            event.setCancelled(true);
            sendDisplay(player, (stats.getBowCooldownUntil() - now) / 1000D, false, true);
            return;
        }
        FileConfiguration game = plugin.getFileManager().get("game.yml");
        if (!game.getBoolean("bow-cooldown.enabled", true)) return;
        long cd = (long) (game.getDouble("bow-cooldown.seconds", 2.5D) * 1000L);
        stats.setBowCooldownUntil(now + cd);
        sendDisplay(player, cd / 1000D, false, true);
        startTask(player);
    }

    private void startTask(final Player player) {
        BukkitTask old = displayTasks.remove(player.getUniqueId());
        if (old != null) old.cancel();
        BukkitTask task = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override public void run() {
                if (!player.isOnline() || plugin.getGameSession().isSpectator(player.getUniqueId()) || plugin.getGameSession().isRespawning(player.getUniqueId())) {
                    BukkitTask t = displayTasks.remove(player.getUniqueId());
                    if (t != null) t.cancel();
                    return;
                }
                long left = plugin.getGameSession().getStats(player.getUniqueId()).getBowCooldownUntil() - System.currentTimeMillis();
                if (left <= 0) {
                    plugin.getGameSession().getStats(player.getUniqueId()).clearBowCooldown();
                    sendDisplay(player, 0D, true, true);
                    BukkitTask t = displayTasks.remove(player.getUniqueId());
                    if (t != null) t.cancel();
                    return;
                }
                sendDisplay(player, left / 1000D, false, false);
            }
        }, 0L, 2L);
        displayTasks.put(player.getUniqueId(), task);
    }

    private void sendDisplay(Player player, double seconds, boolean ready, boolean withTitle) {
        FileConfiguration game = plugin.getFileManager().get("game.yml");
        boolean useActionBar = game.getBoolean("bow-cooldown.display.actionbar", true);
        boolean useTitle = game.getBoolean("bow-cooldown.display.title", true);
        String time = String.format(Locale.US, "%.1f", seconds);
        if (useActionBar) {
            String msg = ready ? game.getString("bow-cooldown.ready-actionbar", "&a弓箭已就绪") : game.getString("bow-cooldown.actionbar-format", "&e弓箭冷却中: &c%time%s");
            ActionBarUtil.send(player, Text.color(msg.replace("%time%", time)));
        }
        if (useTitle && withTitle) {
            String title = game.getString("bow-cooldown.title.title", "&c弓箭冷却中");
            String subtitle = ready ? game.getString("bow-cooldown.title.ready-subtitle", "&a弓箭已就绪") : game.getString("bow-cooldown.title.subtitle", "&f剩余 &e%time%s");
            int fadeIn = game.getInt("bow-cooldown.title.fadein", 0);
            int stay = game.getInt("bow-cooldown.title.stay", 10);
            int fadeOut = game.getInt("bow-cooldown.title.fadeout", 5);
            TitleUtil.sendTitle(player, Text.color(ready ? "" : title), Text.color(subtitle.replace("%time%", time)), fadeIn, stay, fadeOut);
        }
        if (ready) {
            playReadySound(player);
        }
    }

    private void playReadySound(Player player) {
        FileConfiguration sounds = plugin.getFileManager().get("sounds.yml");
        if (!sounds.getBoolean("bow-ready.enabled", false)) return;
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(sounds.getString("bow-ready.sound", "LEVEL_UP")),
                    (float) sounds.getDouble("bow-ready.volume", 1.0D),
                    (float) sounds.getDouble("bow-ready.pitch", 1.2D));
        } catch (Exception ignored) {
        }
    }
}
