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

    public KothConfigManager getConfigManager() {
        return config;
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
                        Bukkit.broadcastMessage(ChatColor.GOLD + "KOTH starts in " + i +
                                " minutes at (" + config.getCenterX() + ", " + config.getCenterZ() + ")!");
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

        // 1. SAVE: Snapshot twice the radius to ensure we cover the border and surrounding area
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

        // Use configured values
        int cx = config.getCenterX();
        int cy = config.getCenterY();
        int cz = config.getCenterZ();
        // Double radius for safety/cleanup
        int saveRadius = config.getRadius() * 2;

        for (int x = cx - saveRadius; x <= cx + saveRadius; x++) {
            for (int z = cz - saveRadius; z <= cz + saveRadius; z++) {
                // Save from configured Y up to the sky
                for (int y = cy; y <= world.getMaxHeight(); y++) {
                    Block block = world.getBlockAt(x, y, z);
                    // Only save non-air blocks to save memory
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
        int cx = config.getCenterX();
        int cz = config.getCenterZ();

        for (int x = cx - radius; x <= cx + radius; x++) {
            ensureWoolAtSurface(world, x, cz + radius);
            ensureWoolAtSurface(world, x, cz - radius);
        }
        for (int z = cz - radius; z <= cz + radius; z++) {
            ensureWoolAtSurface(world, cx + radius, z);
            ensureWoolAtSurface(world, cx - radius, z);
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
            // Prevent lag: do not update if already red wool
            block.setType(Material.RED_WOOL, false);
            break;
        }
    }

    private void updateCaptureLogic() {
        Map<Gildie, Integer> guildsOnHill = new HashMap<>();
        int radius = config.getRadius();
        int cx = config.getCenterX();
        int cy = config.getCenterY(); // Use Config Y
        int cz = config.getCenterZ();

        for (Player p : Bukkit.getOnlinePlayers()) {
            Location loc = p.getLocation();
            if (Math.abs(loc.getX() - cx) <= radius &&
                    Math.abs(loc.getZ() - cz) <= radius &&
                    loc.getY() >= cy) {

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
        int cx = config.getCenterX();
        int cy = config.getCenterY();
        int cz = config.getCenterZ();
        int saveRadius = config.getRadius() * 2;

        // Iterate over the EXACT same volume as the snapshot
        for (int x = cx - saveRadius; x <= cx + saveRadius; x++) {
            for (int z = cz - saveRadius; z <= cz + saveRadius; z++) {
                for (int y = cy; y <= world.getMaxHeight(); y++) {
                    Block block = world.getBlockAt(x, y, z);
                    Location loc = block.getLocation();

                    if (areaSnapshot.containsKey(loc)) {
                        // 1. If it was in the snapshot (it was a block), restore it
                        block.setBlockData(areaSnapshot.get(loc), false);
                    } else {
                        // 2. If it is NOT in the snapshot, it was AIR originally
                        // so we set it to AIR (removing any player builds/wool borders)
                        if (block.getType() != Material.AIR) {
                            block.setType(Material.AIR, false);
                        }
                    }
                }
            }
        }
        areaSnapshot.clear();
    }

    private void updateBossBar() {
        String name = (capturingGuild != null) ? capturingGuild.getTag().toUpperCase() : "None";
        bossBar.setTitle(ChatColor.GOLD + "KOTH: " + name + " (" + currentPoints + "/" + config.getMaxPoints() + ")");
        bossBar.setProgress(Math.min(1.0, (double) currentPoints / config.getMaxPoints()));
    }

    public BossBar getBossBar() { return bossBar; }
}