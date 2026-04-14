package top.cnuo.warbridge.listener;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import top.cnuo.warbridge.WarbridgePlugin;
import top.cnuo.warbridge.game.GameState;
import top.cnuo.warbridge.game.RoundState;
import top.cnuo.warbridge.game.TeamColor;

public class CombatListener implements Listener {
    private final WarbridgePlugin plugin;

    public CombatListener(WarbridgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (plugin.getGameSession().isSpectator(player.getUniqueId()) || plugin.getGameSession().isRespawning(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getGameSession().getState() != GameState.GAMING || plugin.getGameSession().getRoundState() != RoundState.RUNNING) {
            event.setCancelled(true);
            return;
        }
        if (player.getHealth() - event.getFinalDamage() <= 0) {
            event.setCancelled(true);
            Player killer = plugin.getGameSession().findRecentDamager(player);
            plugin.getGameSession().killPlayer(player, killer, killer != null);
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        Player attacker = unwrap(event.getDamager());
        if (attacker != null) {
            if (plugin.getGameSession().isSpectator(attacker.getUniqueId()) || plugin.getGameSession().isRespawning(attacker.getUniqueId())) { event.setCancelled(true); return; }
            if (plugin.getGameSession().isRespawning(victim.getUniqueId()) || plugin.getGameSession().isSpectator(victim.getUniqueId())) { event.setCancelled(true); return; }
            TeamColor attackerTeam = plugin.getGameSession().getPlayerTeam(attacker.getUniqueId());
            TeamColor victimTeam = plugin.getGameSession().getPlayerTeam(victim.getUniqueId());
            if (!plugin.getFileManager().get("game.yml").getBoolean("friendly-fire", false) && attackerTeam != null && attackerTeam == victimTeam) {
                event.setCancelled(true);
                return;
            }
            plugin.getGameSession().recordDamage(victim, attacker);
        }
    }

    private Player unwrap(Entity entity) {
        if (entity instanceof Player) return (Player) entity;
        if (entity instanceof Arrow) {
            Arrow arrow = (Arrow) entity;
            if (arrow.getShooter() instanceof Player) return (Player) arrow.getShooter();
        }
        return null;
    }
}
