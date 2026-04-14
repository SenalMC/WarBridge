package top.cnuo.warbridge.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WarbridgePlayerKillEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player victim;
    private final Player killer;
    private final boolean rewardApplied;

    public WarbridgePlayerKillEvent(Player victim, Player killer, boolean rewardApplied) {
        this.victim = victim;
        this.killer = killer;
        this.rewardApplied = rewardApplied;
    }

    public Player getVictim() { return victim; }
    public Player getKiller() { return killer; }
    public boolean isRewardApplied() { return rewardApplied; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
