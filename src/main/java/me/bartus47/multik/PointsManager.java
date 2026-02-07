package me.bartus47.multik;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PointsManager {

    private final Multik plugin; // ZMIANA: SimpleMarket -> Multik
    private File file;
    private FileConfiguration config;

    public PointsManager(Multik plugin) { // ZMIANA: Konstruktor przyjmuje Multik
        this.plugin = plugin;
        loadFile();
    }

    public double getPoints(UUID playerUUID) {
        if (config == null) return 0.0;
        return config.getDouble("points." + playerUUID.toString(), 0.0);
    }

    public void addPoints(UUID playerUUID, double amount) {
        double current = getPoints(playerUUID);
        config.set("points." + playerUUID.toString(), current + amount);
        saveFile();
    }

    public boolean takePoints(UUID playerUUID, double amount) {
        double current = getPoints(playerUUID);
        if (current >= amount) {
            config.set("points." + playerUUID.toString(), current - amount);
            saveFile();
            return true;
        }
        return false;
    }

    private void loadFile() {
        file = new File(plugin.getDataFolder(), "points.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    private void saveFile() {
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }
}