package fr.cubex.statssync.listeners;

import fr.cubex.statssync.CubexStatsSync;
import fr.cubex.statssync.tasks.StatsSyncTask;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class PlayerListener implements Listener {

    private final CubexStatsSync plugin;

    public PlayerListener(CubexStatsSync plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("sync.on-join", true)) return;

        Player player = event.getPlayer();
        // Initialise la ligne en base si elle n'existe pas (upsert avec valeurs 0)
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                new StatsSyncTask(plugin).syncAll(List.of(player))
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!plugin.getConfig().getBoolean("sync.on-quit", true)) return;

        Player player = event.getPlayer();
        // Sync finale au départ du joueur
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                new StatsSyncTask(plugin).syncAll(List.of(player))
        );
    }
}
