package me.bartus47.multik;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class KothManager {
    private final Multik plugin;
    private final KothConfigManager config;
    private BukkitRunnable activeTask;
    private BossBar bossBar;

    private Gildie capturingGuild = null;
    private int currentPoints = 0;
    private boolean isEventActive = false;
    private final Map<Location, BlockData> areaSnapshot = new HashMap<>();

    public KothManager(Multik plugin, KothConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void scheduleStart() {
        if (isEventActive) return;
        int[] intervals = {30, 20, 10, 5, 3, 2, 1};

        new BukkitRunnable() {
            int timer = 30 * 60;
            @Override
            public void run() {
                if (timer <= 0) {
                    startKoth();
                    this.cancel();
                    return;
                }
                for (int i : intervals) {
                    if (timer == i * 60) {
                        Bukkit.broadcastMessage(ChatColor.GOLD + "KOTH starts in " + i + " minutes at (0, 0)!");
                    }
                }
                timer--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void startKoth() {
        isEventActive = true;
        currentPoints = 0;
        capturingGuild = null;
        saveAreaSnapshot();

        bossBar = Bukkit.createBossBar(ChatColor.YELLOW + "KOTH: No one capturing", BarColor.BLUE, BarStyle.SOLID);
        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);

        activeTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isEventActive) { this.cancel(); return; }
                updateWoolBorder();
                updateCaptureLogic();
            }
        };
        activeTask.runTaskTimer(plugin, 0L, 20L);
    }

    private void saveAreaSnapshot() {
        areaSnapshot.clear();
        World world = Bukkit.getWorlds().get(0);
        int radius = config.getRadius();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int highestY = world.getHighestBlockYAt(x, z);
                for (int y = 60; y <= highestY + 1; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.AIR) {
                        areaSnapshot.put(block.getLocation(), block.getBlockData().clone());
                    }
                }
            }
        }
    }

    private void updateWoolBorder() {
        World world = Bukkit.getWorlds().get(0);
        int radius = config.getRadius();
        for (int x = -radius; x <= radius; x++) {
            ensureWoolAtSurface(world, x, radius);
            ensureWoolAtSurface(world, x, -radius);
        }
        for (int z = -radius; z <= radius; z++) {
            ensureWoolAtSurface(world, radius, z);
            ensureWoolAtSurface(world, -radius, z);
        }
    }

    private void ensureWoolAtSurface(World world, int x, int z) {
        for (int y = world.getMaxHeight() - 1; y > 0; y--) {
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            if (type == Material.RED_WOOL) return;
            if (type.isAir() || type.toString().contains("LEAVES") || type.toString().contains("LOG") ||
                    type == Material.GRASS || type == Material.TALL_GRASS) {
                continue;
            }
            block.setType(Material.RED_WOOL);
            break;
        }
    }

    private void updateCaptureLogic() {
        Map<Gildie, Integer> guildsOnHill = new HashMap<>();
        int radius = config.getRadius();

        for (Player p : Bukkit.getOnlinePlayers()) {
            Location loc = p.getLocation();
            // FIXED: Checks if player is inside the square (radius 25) at (0,0) and above Y=60
            if (Math.abs(loc.getX()) <= radius && Math.abs(loc.getZ()) <= radius && loc.getY() >= 60) {
                Gildie g = plugin.getGuildManager().getGuildByPlayer(p.getUniqueId());
                if (g != null) {
                    guildsOnHill.put(g, guildsOnHill.getOrDefault(g, 0) + 1);
                }
            }
        }

        if (guildsOnHill.size() > 1) {
            bossBar.setTitle(ChatColor.RED + "Contested! (" + currentPoints + "/" + config.getMaxPoints() + ")");
            return;
        }

        if (guildsOnHill.isEmpty()) {
            if (currentPoints > 0) currentPoints--;
            updateBossBar();
            return;
        }

        Gildie activeGuild = guildsOnHill.keySet().iterator().next();
        if (capturingGuild == null || !capturingGuild.equals(activeGuild)) {
            if (currentPoints > 0) currentPoints--; else capturingGuild = activeGuild;
        } else {
            currentPoints += 1;
        }

        if (currentPoints >= config.getMaxPoints()) winEvent(capturingGuild);
        updateBossBar();
    }

    private void winEvent(Gildie winningGuild) {
        isEventActive = false;
        String winMessage = "Team " + winningGuild.getTag().toUpperCase() + " won the King of The Hill event and got 100 points";
        for (Player p : Bukkit.getOnlinePlayers()) {
            Gildie playerGuild = plugin.getGuildManager().getGuildByPlayer(p.getUniqueId());
            if (playerGuild != null && playerGuild.getTag().equalsIgnoreCase(winningGuild.getTag())) {
                p.sendMessage(ChatColor.GREEN + winMessage);
            } else {
                p.sendMessage(ChatColor.RED + winMessage);
            }
        }
        winningGuild.addPoints(100);
        for (ItemStack item : config.getRewards()) {
            plugin.getGuildInventoryManager().getTakeOnlyChest(winningGuild.getTag()).addItem(item);
        }
        stopEvent();
        plugin.getGuildManager().saveGuilds();
    }

    public void stopEvent() {
        isEventActive = false;
        if (bossBar != null) bossBar.removeAll();
        Bukkit.broadcastMessage(ChatColor.YELLOW + "KOTH area will be restored in 1 minute.");
        new BukkitRunnable() {
            @Override
            public void run() {
                restoreArea();
                Bukkit.broadcastMessage(ChatColor.GREEN + "KOTH area has been restored.");
            }
        }.runTaskLater(plugin, 1200L);
    }

    private void restoreArea() {
        World world = Bukkit.getWorlds().get(0);
        int radius = config.getRadius();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = 60; y <= world.getMaxHeight(); y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }
        areaSnapshot.forEach((loc, data) -> loc.getBlock().setBlockData(data));
        areaSnapshot.clear();
    }

    private void updateBossBar() {
        String name = (capturingGuild != null) ? capturingGuild.getTag().toUpperCase() : "None";
        bossBar.setTitle(ChatColor.GOLD + "KOTH: " + name + " (" + currentPoints + "/" + config.getMaxPoints() + ")");
        bossBar.setProgress(Math.min(1.0, (double) currentPoints / config.getMaxPoints()));
    }

    public BossBar getBossBar() { return bossBar; }
}