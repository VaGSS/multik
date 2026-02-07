package me.bartus47.multik;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerManager {

    private final Multik plugin;
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, String> dailyRewardPoints = new HashMap<>();
    private final Map<UUID, Long> loginTimes = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public PlayerManager(Multik plugin) {
        this.plugin = plugin;
        loadFile();
        startDailyRewardTask();
    }

    public void registerLogin(UUID uuid) {
        loginTimes.put(uuid, System.currentTimeMillis());
    }

    public void registerLogout(UUID uuid) {
        loginTimes.remove(uuid);
    }

    private void startDailyRewardTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();

                    // Skip if already rewarded today
                    if (dailyRewardPoints.getOrDefault(uuid, "").equals(today)) continue;

                    long sessionStart = loginTimes.getOrDefault(uuid, System.currentTimeMillis());
                    if (System.currentTimeMillis() - sessionStart >= 3600000L) { // 1 hour threshold
                        Gildie guild = plugin.getGuildManager().getGuildByPlayer(uuid);
                        if (guild != null) {
                            guild.addPoints(20); // Award 20 points
                            plugin.getGuildManager().saveGuilds();
                            dailyRewardPoints.put(uuid, today);
                            saveFile();
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // Check every minute
    }

    public void addKill(UUID playerUUID) {
        kills.put(playerUUID, getKills(playerUUID) + 1);
        saveFile();
    }

    public int getKills(UUID playerUUID) {
        return kills.getOrDefault(playerUUID, 0);
    }

    public LinkedHashMap<String, Integer> getTopKillers(int limit) {
        return kills.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        e -> {
                            String name = Bukkit.getOfflinePlayer(e.getKey()).getName();
                            return (name != null) ? name : "Unknown";
                        },
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private void loadFile() {
        file = new File(plugin.getDataFolder(), "players.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);

        if (config.contains("kills")) {
            for (String key : config.getConfigurationSection("kills").getKeys(false)) {
                kills.put(UUID.fromString(key), config.getInt("kills." + key));
            }
        }
        if (config.contains("dailyReward")) {
            for (String key : config.getConfigurationSection("dailyReward").getKeys(false)) {
                dailyRewardPoints.put(UUID.fromString(key), config.getString("dailyReward." + key));
            }
        }
    }

    private void saveFile() {
        if (config == null) return;
        for (Map.Entry<UUID, Integer> entry : kills.entrySet()) {
            config.set("kills." + entry.getKey().toString(), entry.getValue());
        }
        for (Map.Entry<UUID, String> entry : dailyRewardPoints.entrySet()) {
            config.set("dailyReward." + entry.getKey().toString(), entry.getValue());
        }
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }
}