package fr.cubex.statssync.commands;

import fr.cubex.statssync.CubexStatsSync;
import fr.cubex.statssync.tasks.StatsSyncTask;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StatsSyncCommand implements CommandExecutor, TabCompleter {

    private final CubexStatsSync plugin;

    public StatsSyncCommand(CubexStatsSync plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
             final String prefix = colorize(plugin.getConfig().getString("messages.prefix", "[CubeX] "));

        if (!sender.hasPermission("cubex.statssync.admin")) {
            sender.sendMessage(prefix + colorize(plugin.getConfig().getString("messages.no-permission", "&cPermission refusée.")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(prefix + "&eUsage : /statssync <reload|sync>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reload();
                sender.sendMessage(prefix + colorize(plugin.getConfig().getString("messages.reload-success", "&aRechargé.")));
            }
            case "sync" -> {
                final int count = plugin.getServer().getOnlinePlayers().size();
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    new StatsSyncTask(plugin).syncAll(plugin.getServer().getOnlinePlayers());
                    String msg = plugin.getConfig().getString("messages.sync-success", "&aSync OK (%count%)")
                            .replace("%count%", String.valueOf(count));
                    sender.sendMessage(prefix + colorize(msg));
                });
            }
            default -> sender.sendMessage(prefix + "&eUsage : /statssync <reload|sync>");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload", "sync");
        }
        return List.of();
    }

    private String colorize(String s) {
        return s == null ? "" : s.replace("&", "§");
    }
}
