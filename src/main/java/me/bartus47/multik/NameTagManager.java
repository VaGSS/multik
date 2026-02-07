package me.bartus47.multik;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class NameTagManager implements Listener {

    private final Multik plugin;
    private final GuildManager guildManager;

    public NameTagManager(Multik plugin, GuildManager guildManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
    }

    public void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    updateScoreboard(p);
                    // Hides the player from the default middle section of the Tab list
                    p.setPlayerListName("");
                }
            }
        }.runTaskTimer(plugin, 20L, 60L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        updateScoreboard(player);
        // Ensure they are hidden from the middle list immediately upon joining
        player.setPlayerListName("");
    }

    private void updateScoreboard(Player viewer) {
        Scoreboard sb = viewer.getScoreboard();
        Gildie viewerGuild = guildManager.getGuildByPlayer(viewer.getUniqueId());

        for (Player target : Bukkit.getOnlinePlayers()) {
            String teamName = target.getName();
            Team team = sb.getTeam(teamName);
            if (team == null) {
                team = sb.registerNewTeam(teamName);
            }
            if (!team.hasEntry(target.getName())) {
                team.addEntry(target.getName());
            }

            Gildie targetGuild = guildManager.getGuildByPlayer(target.getUniqueId());

            if (targetGuild == null) {
                team.setPrefix(ChatColor.GRAY + "");
                team.setSuffix("");
                team.setColor(ChatColor.GRAY);
            } else {
                ChatColor relationColor = (viewerGuild != null && viewerGuild.getTag().equals(targetGuild.getTag()))
                        ? ChatColor.GREEN : ChatColor.RED;

                team.setPrefix(relationColor + targetGuild.getTag().toUpperCase() + " ");
                team.setSuffix(ChatColor.WHITE + " [" + targetGuild.getPoints() + "]");
                team.setColor(ChatColor.WHITE);
            }
        }
    }
}