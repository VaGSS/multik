package me.bartus47.multik;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LootManager {
    private final Multik plugin;
    private final ChestConfigManager configManager;
    private final Random random = new Random();

    private final Map<String, BukkitTask> activeTasks = new HashMap<>();
    private final Map<Location, Long> chestSpawnTimes = new HashMap<>();
    private final Map<Location, String> chestTypes = new HashMap<>();
    private final Map<Location, ArmorStand> holograms = new HashMap<>();

    public LootManager(Multik plugin, ChestConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        if (configManager.isActive()) {
            startAllTimers();
        }
    }

    public void startAllTimers() {
        stopAllTimers();
        for (String key : configManager.getChestKeys()) {
            int interval = configManager.getConfig().getInt("chests." + key + ".interval", 20);
            long ticks = (long) interval * 60 * 20;

            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (configManager.isActive()) {
                        spawnChest(key);
                    }
                }
            }.runTaskTimer(plugin, ticks, ticks);

            activeTasks.put(key, task);
        }
    }

    public void stopAllTimers() {
        activeTasks.values().forEach(BukkitTask::cancel);
        activeTasks.clear();
        cleanupAllChests();
    }

    private void spawnChest(String key) {
        World world = Bukkit.getWorlds().get(0);
        int x = random.nextInt(3001) - 1500;
        int z = random.nextInt(3001) - 1500;
        int y = world.getHighestBlockYAt(x, z) + 1;
        Location loc = new Location(world, x, y, z);

        loc.getBlock().setType(Material.CHEST);
        chestSpawnTimes.put(loc, System.currentTimeMillis());
        chestTypes.put(loc, key);

        String name = configManager.getConfig().getString("chests." + key + ".name", key);
        ArmorStand as = (ArmorStand) world.spawnEntity(loc.clone().add(0.5, 1.2, 0.5), EntityType.ARMOR_STAND);
        as.setVisible(false);
        as.setGravity(false);
        as.setMarker(true);
        as.setCustomNameVisible(true);
        as.setCustomName(ChatColor.GOLD + "" + ChatColor.BOLD + name);

        holograms.put(loc, as);

        Bukkit.broadcastMessage(ChatColor.GOLD + name + " chest appeared at " + x + ", " + y + ", " + z);
    }

    public void removeChest(Location loc) {
        // FIXED: Ensure the ArmorStand is explicitly removed from the world
        if (holograms.containsKey(loc)) {
            ArmorStand as = holograms.get(loc);
            if (as != null) {
                as.remove();
            }
            holograms.remove(loc);
        }

        chestSpawnTimes.remove(loc);
        chestTypes.remove(loc);
        loc.getBlock().setType(Material.AIR);
    }

    private void cleanupAllChests() {
        holograms.forEach((loc, as) -> {
            loc.getBlock().setType(Material.AIR);
            if (as != null) as.remove();
        });
        holograms.clear();
        chestSpawnTimes.clear();
        chestTypes.clear();
    }

    public Map<Location, Long> getSpawnTimes() { return chestSpawnTimes; }
    public Map<Location, String> getChestTypes() { return chestTypes; }
}