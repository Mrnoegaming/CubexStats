package fr.cubex.statssync;

import fr.cubex.statssync.commands.StatsSyncCommand;
import fr.cubex.statssync.database.DatabaseManager;
import fr.cubex.statssync.listeners.PlayerListener;
import fr.cubex.statssync.tasks.StatsSyncTask;
import org.bukkit.plugin.java.JavaPlugin;

public class CubexStatsSync extends JavaPlugin {

    private static CubexStatsSync instance;
    private DatabaseManager databaseManager;
    private StatsSyncTask syncTask;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // Connexion base de données
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.connect()) {
            getLogger().severe("Impossible de se connecter à la base de données. Désactivation du plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        databaseManager.createTable();
        getLogger().info("Connexion MariaDB établie.");

        // Listener
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Tâche de sync périodique
        long intervalTicks = getConfig().getLong("sync.interval", 300) * 20L;
        syncTask = new StatsSyncTask(this);
        syncTask.runTaskTimerAsynchronously(this, intervalTicks, intervalTicks);

        // Commande
        StatsSyncCommand cmd = new StatsSyncCommand(this);
        getCommand("statssync").setExecutor(cmd);
        getCommand("statssync").setTabCompleter(cmd);

        getLogger().info("Cubex-StatsSync activé avec succès !");
    }

    @Override
    public void onDisable() {
        // Sync finale des joueurs encore connectés
        if (databaseManager != null && databaseManager.isConnected()) {
            getLogger().info("Sync finale avant arrêt...");
            new StatsSyncTask(this).syncAll(getServer().getOnlinePlayers());
            databaseManager.disconnect();
        }
        getLogger().info("Cubex-StatsSync désactivé.");
    }

    public void reload() {
        reloadConfig();
        if (syncTask != null) {
            syncTask.cancel();
        }
        long intervalTicks = getConfig().getLong("sync.interval", 300) * 20L;
        syncTask = new StatsSyncTask(this);
        syncTask.runTaskTimerAsynchronously(this, intervalTicks, intervalTicks);
    }

    public static CubexStatsSync getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
