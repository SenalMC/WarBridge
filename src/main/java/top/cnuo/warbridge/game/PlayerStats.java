package top.cnuo.warbridge.game;

public class PlayerStats {
    private int kills;
    private int deaths;
    private int goals;
    private int coins;
    private int exp;
    private long bowCooldownUntil;

    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getGoals() { return goals; }
    public int getCoins() { return coins; }
    public int getExp() { return exp; }
    public long getBowCooldownUntil() { return bowCooldownUntil; }
    public void setBowCooldownUntil(long bowCooldownUntil) { this.bowCooldownUntil = bowCooldownUntil; }

    public void addKill() { this.kills++; }
    public void addDeath() { this.deaths++; }
    public void addGoal() { this.goals++; }
    public void addCoins(int coins) { this.coins += coins; }
    public void addExp(int exp) { this.exp += exp; }
    public void resetMatch() { this.kills = 0; this.deaths = 0; this.goals = 0; this.bowCooldownUntil = 0L; }
    public void clearBowCooldown() { this.bowCooldownUntil = 0L; }
}
