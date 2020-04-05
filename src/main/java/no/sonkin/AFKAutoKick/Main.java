package no.sonkin.AFKAutoKick;

import net.ess3.api.events.AfkStatusChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedList;

public class Main extends JavaPlugin implements Listener {
    private int maxPlayers;
    private LinkedList<Player> afkPlayers;
    private String kickMessage;
    private int offset;
    private int autocheck;
    private boolean logToConsole;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        maxPlayers = getServer().getMaxPlayers();
        afkPlayers = new LinkedList<>();

        kickMessage = getConfig().getString("kick-message");
        if (kickMessage == null) {
            kickMessage = "You were kicked because the server is almost full. Please rejoin :)";
        }
        offset = getConfig().getInt("offset");
        autocheck = getConfig().getInt("autocheck");
        logToConsole = getConfig().getBoolean("logToConsole", true);

        if (autocheck > 0) {
            // If the server is full, the playerJoinEvent will never trigger, so we have to check regularly if someone is afk
            long timeBetween = autocheck * 60 * 20; // Runs every 15 minutes
            getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
                if (logToConsole)
                    getLogger().info("Checking if an afk player must be kicked.");
                tryKickAFK();
            }, timeBetween, timeBetween);
        }
    }

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        tryKickAFK();
    }

    @EventHandler
    void onAfkEvent(AfkStatusChangeEvent event) {
        Player player = event.getAffected().getBase();
        if (player.isOp() || player.hasPermission("afkautokick.bypass")) {
            return;
        }
        if (event.getValue()) {
            afkPlayers.add(player);
        } else {
            afkPlayers.remove(player);
        }
    }

    /**
     * Kicks the player who has been afk the longest if the server is full or almost full
     */
    private void tryKickAFK() {
        if (getServer().getOnlinePlayers().size() >= maxPlayers - offset) {
            // getServer().getLogger().severe(afkPlayers.toString());
            while (afkPlayers.size() > 0) {
                Player player = afkPlayers.pop();
                if (player.isOnline()) {
                    if (logToConsole)
                        getLogger().info("Kicking " + player.getName());
                    player.kickPlayer(kickMessage);
                    return;
                }
            }
        }
    }
}
