package me.bartus47.multik;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ChestConfigManager {
    private final Multik plugin;
    private File file;
    private FileConfiguration config;

    public ChestConfigManager(Multik plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "chests.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
                config = YamlConfiguration.loadConfiguration(file);
                config.set("active", true);

                config.set("chests.LOOT.name", "LOOT");
                config.set("chests.LOOT.interval", 20);
                config.set("chests.LOOT.open_delay", 15);
                config.set("chests.LOOT.items", List.of("DIAMOND:1"));

                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public Set<String> getChestKeys() {
        ConfigurationSection section = config.getConfigurationSection("chests");
        if (section == null) return Collections.emptySet();
        return section.getKeys(false);
    }

    public boolean isActive() {
        return config.getBoolean("active", true);
    }

    public void setActive(boolean active) {
        config.set("active", active);
        save();
    }

    public int getOpenDelay(String key) {
        return config.getInt("chests." + key + ".open_delay", 15);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}