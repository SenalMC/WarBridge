package top.cnuo.warbridge.command;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import top.cnuo.warbridge.WarbridgePlugin;
import top.cnuo.warbridge.game.GameState;
import top.cnuo.warbridge.game.GameTeam;
import top.cnuo.warbridge.game.TeamColor;
import top.cnuo.warbridge.util.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class WarbridgeCommand implements CommandExecutor, TabCompleter {
    private final WarbridgePlugin plugin;
    private final Map<UUID, Location> selectionOne = new HashMap<UUID, Location>();
    private final Map<UUID, Location> selectionTwo = new HashMap<UUID, Location>();

    public WarbridgeCommand(WarbridgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Text.color("&e/warbridge edit|stop|reload|check|spectate|leave|setup"));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ENGLISH);
        if ("edit".equals(sub)) {
            if (!sender.hasPermission("warbridge.edit")) return noPerm(sender);
            plugin.getGameSession().toggleEdit();
            sender.sendMessage(plugin.getGameSession().getState() == GameState.EDIT ? plugin.getMessages().get(null, "edit-enter") : plugin.getMessages().get(null, "edit-exit"));
            return true;
        }
        if ("stop".equals(sub)) {
            if (!sender.hasPermission("warbridge.admin")) return noPerm(sender);
            plugin.getGameSession().forceEdit();
            sender.sendMessage(plugin.getMessages().get(null, "edit-enter"));
            return true;
        }
        if ("reload".equals(sub)) {
            if (!sender.hasPermission("warbridge.admin")) return noPerm(sender);
            plugin.reloadPlugin();
            sender.sendMessage(plugin.getMessages().get(null, "reload"));
            return true;
        }
        if ("check".equals(sub)) {
            if (!sender.hasPermission("warbridge.admin")) return noPerm(sender);
            sender.sendMessage(plugin.getMessages().get(null, "check-header"));
            for (String line : plugin.getGameSession().getStartCheckSummary()) {
                sender.sendMessage(Text.color("&7- &f" + line));
            }
            java.util.List<String> problems = plugin.getGameSession().getStartCheckProblems();
            if (problems.isEmpty()) {
                sender.sendMessage(plugin.getMessages().get(null, "check-ok"));
            } else {
                sender.sendMessage(plugin.getMessages().get(null, "check-fail"));
                for (String problem : problems) {
                    sender.sendMessage(Text.color("&c- " + problem));
                }
            }
            return true;
        }
        if ("spectate".equals(sub)) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessages().get(null, "only-player"));
                return true;
            }
            Player player = (Player) sender;
            plugin.getGameSession().setSpectator(player, !plugin.getGameSession().isSpectator(player.getUniqueId()));
            return true;
        }
        if ("leave".equals(sub)) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessages().get(null, "only-player"));
                return true;
            }
            plugin.getGameSession().sendFallback((Player) sender);
            return true;
        }
        if ("setup".equals(sub)) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessages().get(null, "only-player"));
                return true;
            }
            return handleSetup((Player) sender, Arrays.copyOfRange(args, 1, args.length));
        }
        sender.sendMessage(plugin.getMessages().get(null, "unknown-subcommand"));
        return true;
    }




    private boolean handleSetup(Player player, String[] args) {
        if (!player.hasPermission("warbridge.edit")) return noPerm(player);
        if (plugin.getGameSession().getState() != GameState.EDIT) {
            player.sendMessage(plugin.getMessages().get(player, "setup-only-edit"));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(Text.color("&e/warbridge setup addteam <color> <name>"));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ENGLISH);
        try {
            if ("addteam".equals(sub)) {
                if (args.length < 3) return usage(player, "/warbridge setup addteam <color> <name>");
                TeamColor color = TeamColor.fromId(args[1]);
                if (color == null) return fail(player, "unknown-team");
                plugin.getGameSession().setTeam(color, args[2]);
                plugin.getGameSession().saveMapConfig();
                player.sendMessage(plugin.getMessages().get(player, "team-added", Collections.singletonMap("%team%", color.getChatColor() + args[2])));
                return true;
            }
            if ("setteamspawn".equals(sub)) {
                GameTeam team = requireTeam(player, args, 1); if (team == null) return true;
                team.setSpawn(player.getLocation());
                plugin.getGameSession().saveMapConfig();
                player.sendMessage(plugin.getMessages().get(player, "team-spawn-set"));
                return true;
            }
            if ("setcagepos1".equals(sub)) {
                selectionOne.put(player.getUniqueId(), player.getLocation());
                player.sendMessage(plugin.getMessages().get(player, "cage-pos1-set"));
                return true;
            }
            if ("setcagepos2".equals(sub)) {
                GameTeam team = requireTeam(player, args, 1); if (team == null) return true;
                Location a = selectionOne.get(player.getUniqueId());
                if (a == null) return usage(player, "请先执行 /warbridge setup setcagepos1 <color>");
                team.setCage(new top.cnuo.warbridge.util.Cuboid(a, player.getLocation()));
                plugin.getGameSession().saveMapConfig();
                player.sendMessage(plugin.getMessages().get(player, "cage-set"));
                return true;
            }
            if ("setportalpos1".equals(sub)) {
                selectionOne.put(player.getUniqueId(), player.getLocation());
                player.sendMessage(plugin.getMessages().get(player, "portal-pos1-set"));
                return true;
            }
            if ("setportalpos2".equals(sub)) {
                GameTeam team = requireTeam(player, args, 1); if (team == null) return true;
                Location a = selectionOne.get(player.getUniqueId());
                if (a == null) return usage(player, "请先执行 /warbridge setup setportalpos1 <color>");
                team.setPortal(new top.cnuo.warbridge.util.Cuboid(a, player.getLocation()));
                plugin.getGameSession().saveMapConfig();
                player.sendMessage(plugin.getMessages().get(player, "portal-set"));
                return true;
            }
            if ("setlobbyspawn".equals(sub)) {
                plugin.getGameSession().setLobbySpawn(player.getLocation());
                plugin.getGameSession().saveMapConfig();
                player.sendMessage(plugin.getMessages().get(player, "lobby-set"));
                return true;
            }
            if ("setspectatorspawn".equals(sub)) {
                plugin.getGameSession().setSpectatorSpawn(player.getLocation());
                plugin.getGameSession().saveMapConfig();
                player.sendMessage(plugin.getMessages().get(player, "spectator-set"));
                return true;
            }
            if ("setgamepos1".equals(sub)) {
                selectionTwo.put(player.getUniqueId(), player.getLocation());
                player.sendMessage(plugin.getMessages().get(player, "game-pos1-set"));
                return true;
            }
            if ("setgamepos2".equals(sub)) {
                Location a = selectionTwo.get(player.getUniqueId());
                if (a == null) return usage(player, "请先执行 /warbridge setup setgamepos1");
                plugin.getGameSession().setGameRegion(a, player.getLocation());
                plugin.getGameSession().saveMapConfig();
                player.sendMessage(plugin.getMessages().get(player, "game-set"));
                return true;
            }
            if ("finish".equals(sub)) {
                java.util.List<String> problems = plugin.getGameSession().getStartCheckProblems();
                if (problems.isEmpty()) {
                    player.sendMessage(plugin.getMessages().get(player, "setup-finish-ok"));
                } else {
                    player.sendMessage(plugin.getMessages().get(player, "setup-finish-fail"));
                    for (String problem : problems) player.sendMessage(Text.color("&c- " + problem));
                }
                return true;
            }
            if ("points".equals(sub)) {
                plugin.getGameSession().setPoints(parsePositive(args, 1));
                plugin.getFileManager().save("game.yml");
                player.sendMessage(plugin.getMessages().get(player, "points-set"));
                return true;
            }
            if ("minplayer".equals(sub)) {
                plugin.getGameSession().setMinPlayers(parsePositive(args, 1));
                plugin.getFileManager().save("game.yml");
                player.sendMessage(plugin.getMessages().get(player, "minplayer-set"));
                return true;
            }
            if ("maxplayer".equals(sub)) {
                plugin.getGameSession().setMaxPlayers(parsePositive(args, 1));
                plugin.getFileManager().save("game.yml");
                player.sendMessage(plugin.getMessages().get(player, "maxplayer-set"));
                return true;
            }
            if ("teamplayer".equals(sub)) {
                plugin.getGameSession().setTeamPlayers(parsePositive(args, 1));
                plugin.getFileManager().save("game.yml");
                player.sendMessage(plugin.getMessages().get(player, "teamplayer-set"));
                return true;
            }
        } catch (IOException e) {
            player.sendMessage(Text.color("&c保存配置失败: " + e.getMessage()));
            return true;
        } catch (Exception e) {
            player.sendMessage(Text.color("&c参数错误: " + e.getMessage()));
            return true;
        }
        return usage(player, "未知 setup 子命令。");
    }

    private int parsePositive(String[] args, int index) {
        if (args.length <= index) throw new IllegalArgumentException("缺少数字参数");
        int value = Integer.parseInt(args[index]);
        if (value <= 0) throw new IllegalArgumentException("数值必须大于 0");
        return value;
    }

    private GameTeam requireTeam(Player player, String[] args, int index) {
        if (args.length <= index) {
            usage(player, "缺少队伍颜色参数");
            return null;
        }
        TeamColor color = TeamColor.fromId(args[index]);
        if (color == null || !plugin.getGameSession().getTeams().containsKey(color)) {
            player.sendMessage(plugin.getMessages().get(player, "unknown-team"));
            return null;
        }
        return plugin.getGameSession().getTeams().get(color);
    }

    private boolean noPerm(CommandSender sender) {
        sender.sendMessage(plugin.getMessages().get(null, "no-permission"));
        return true;
    }

    private boolean usage(Player player, String usage) {
        player.sendMessage(Text.color("&e" + usage));
        return true;
    }

    private boolean fail(Player player, String key) {
        player.sendMessage(plugin.getMessages().get(player, key));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("edit", "stop", "reload", "check", "spectate", "leave", "setup"), args[0]);
        }
        if (args.length == 2 && "setup".equalsIgnoreCase(args[0])) {
            return filter(Arrays.asList("addteam", "setteamspawn", "setcagepos1", "setcagepos2", "setportalpos1", "setportalpos2", "setlobbyspawn", "setspectatorspawn", "setgamepos1", "setgamepos2", "finish", "points", "minplayer", "maxplayer", "teamplayer"), args[1]);
        }
        if (args.length == 3 && "setup".equalsIgnoreCase(args[0])) {
            String second = args[1].toLowerCase(Locale.ENGLISH);
            if ("addteam".equals(second) || "setteamspawn".equals(second) || "setcagepos2".equals(second) || "setportalpos2".equals(second)) {
                return filter(Arrays.asList("red", "yellow", "green", "blue"), args[2]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String input) {
        String lower = input.toLowerCase(Locale.ENGLISH);
        List<String> result = new ArrayList<String>();
        for (String item : list) {
            if (item.toLowerCase(Locale.ENGLISH).startsWith(lower)) result.add(item);
        }
        return result;
    }
}
