package me.bartus47.multik;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import java.util.List;

public class LootInteractionListener implements Listener {
    private final Multik plugin;

    public LootInteractionListener(Multik plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.CHEST) return;
        Location loc = event.getClickedBlock().getLocation();
        if (!plugin.getLootManager().getSpawnTimes().containsKey(loc)) return;

        event.setCancelled(true);
        handleClaim(event.getPlayer(), loc);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (plugin.getLootManager().getSpawnTimes().containsKey(loc)) {
            event.setCancelled(true);
            handleClaim(event.getPlayer(), loc);
        }
    }

    private void handleClaim(Player player, Location loc) {
        String type = plugin.getLootManager().getChestTypes().get(loc);
        long spawnTime = plugin.getLootManager().getSpawnTimes().get(loc);
        int delay = plugin.getChestConfigManager().getOpenDelay(type);

        long diff = System.currentTimeMillis() - spawnTime;
        if (diff < (long) delay * 60 * 1000) {
            long r = ((long) delay * 60 * 1000 - diff) / 1000;
            player.sendMessage(ChatColor.RED + "Locked! Wait: " + String.format("%d:%02d", r/60, r%60));
            return;
        }

        Gildie guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "You need a guild!");
            return;
        }

        // AWARD 10 POINTS (Changed from 20)
        guild.addPoints(10);
        plugin.getGuildManager().saveGuilds();

        List<String> items = plugin.getChestConfigManager().getConfig().getStringList("chests." + type + ".items");
        for (String s : items) {
            try {
                String[] p = s.split(":");
                Material m = Material.matchMaterial(p[0]);
                int amt = (p.length > 1) ? Integer.parseInt(p[1]) : 1;
                if (m != null) plugin.getGuildInventoryManager().getTakeOnlyChest(guild.getTag()).addItem(new ItemStack(m, amt));
            } catch (Exception ignored) {}
        }

        // Global broadcast message
        String chestName = plugin.getChestConfigManager().getConfig().getString("chests." + type + ".name", type);
        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "LOOT > " +
                ChatColor.YELLOW + player.getName() + " claimed " + ChatColor.GOLD + chestName +
                ChatColor.YELLOW + " for guild " + ChatColor.GOLD + guild.getTag().toUpperCase() +
                ChatColor.GREEN + " (+10 points)!");

        plugin.getLootManager().removeChest(loc);
    }
}