package me.bartus47.multik;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class GuildListener implements Listener {

    private final GuildManager guildManager;
    private final Multik plugin;

    public GuildListener(GuildManager guildManager, Multik plugin) {
        this.guildManager = guildManager;
        this.plugin = plugin;
    }

    // USUNIĘTO onChat - TO KLUCZOWE, ABY DZIAŁAŁ CONFIG ESSENTIALS

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Gildie toGuild = guildManager.getGuildAtLocation(event.getTo());
        Gildie fromGuild = guildManager.getGuildAtLocation(event.getFrom());

        // Check if entered a new guild territory
        if (toGuild != null && !toGuild.equals(fromGuild)) {

            // Check if intruder (not member/leader/co-leader)
            if (!toGuild.getMembers().contains(player.getUniqueId()) &&
                    !toGuild.getLeader().equals(player.getUniqueId())) {

                List<Player> membersToNotify = new ArrayList<>();
                for (UUID memberUUID : toGuild.getMembers()) {
                    Player member = Bukkit.getPlayer(memberUUID);
                    if (member != null && member.isOnline()) {
                        membersToNotify.add(member);
                    }
                }

                if (membersToNotify.isEmpty()) return;

                BossBar alertBar = Bukkit.createBossBar(
                        ChatColor.RED + player.getName() + " entered your territory",
                        BarColor.RED,
                        BarStyle.SOLID
                );

                for (Player p : membersToNotify) {
                    alertBar.addPlayer(p);
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        alertBar.removeAll();
                    }
                }.runTaskLater(plugin, 100L);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (isProtectedBlock(block.getType())) {
            Gildie guildAtLoc = guildManager.getGuildAtLocation(block.getLocation());
            if (guildAtLoc == null) return;

            if (guildAtLoc.getMembers().contains(event.getPlayer().getUniqueId()) ||
                    guildAtLoc.getLeader().equals(event.getPlayer().getUniqueId())) return;

            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Protected territory of " + guildAtLoc.getName());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Gildie guildAtLoc = guildManager.getGuildAtLocation(event.getBlock().getLocation());
        if (guildAtLoc == null) return;
        if (guildAtLoc.getMembers().contains(event.getPlayer().getUniqueId()) ||
                guildAtLoc.getLeader().equals(event.getPlayer().getUniqueId())) return;

        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.RED + "Cannot break blocks here.");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Gildie guildAtLoc = guildManager.getGuildAtLocation(event.getBlock().getLocation());
        if (guildAtLoc == null) return;
        if (guildAtLoc.getMembers().contains(event.getPlayer().getUniqueId()) ||
                guildAtLoc.getLeader().equals(event.getPlayer().getUniqueId())) return;

        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.RED + "Cannot place blocks here.");
    }

    private boolean isProtectedBlock(Material mat) {
        String name = mat.toString();
        return name.contains("CHEST") || name.contains("BARREL") || name.contains("SHULKER") || name.contains("DOOR");
    }
}