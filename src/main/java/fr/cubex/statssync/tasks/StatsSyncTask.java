package fr.cubex.statssync.tasks;

import fr.cubex.statssync.CubexStatsSync;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public class StatsSyncTask extends BukkitRunnable {

    private final CubexStatsSync plugin;

    public StatsSyncTask(CubexStatsSync plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        syncAll(plugin.getServer().getOnlinePlayers());
    }

    /**
     * Synchronise les stats d'une collection de joueurs.
     * Peut être appelé depuis le thread principal ou un thread async.
     */
    public void syncAll(Collection<? extends Player> players) {
        if (players.isEmpty()) return;

        String killsPlaceholder      = plugin.getConfig().getString("placeholders.kills",            "%cubex_kill%");
        String deathsPlaceholder     = plugin.getConfig().getString("placeholders.deaths",           "%cubex_death%");
        String timePlaceholder       = plugin.getConfig().getString("placeholders.time-played-hours","%cubex_statistic_time_played:hours%");

        int synced = 0;
        for (Player player : players) {
            try {
                String rawKills  = PlaceholderAPI.setPlaceholders(player, killsPlaceholder);
                String rawDeaths = PlaceholderAPI.setPlaceholders(player, deathsPlaceholder);
                String rawTime   = PlaceholderAPI.setPlaceholders(player, timePlaceholder);

                int    kills  = parseIntSafe(rawKills);
                int    deaths = parseIntSafe(rawDeaths);
                double hours  = parseDoubleSafe(rawTime);

                plugin.getDatabaseManager().upsertStats(
                        player.getUniqueId(),
                        player.getName(),
                        kills,
                        deaths,
                        hours
                );
                synced++;
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur sync " + player.getName() + " : " + e.getMessage());
            }
        }

        if (synced > 0) {
            plugin.getLogger().info("Stats synchronisées pour " + synced + " joueur(s).");
        }
    }

    // ------------------------------------------------------------------ //

    private int parseIntSafe(String value) {
        if (value == null) return 0;
        try {
            // Certains placeholders retournent des décimaux (ex: "12.0")
            return (int) Double.parseDouble(value.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDoubleSafe(String value) {
        if (value == null) return 0;
        try {
            return Double.parseDouble(value.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
