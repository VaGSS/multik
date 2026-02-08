package me.bartus47.multik;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KothConfigManager {
    private final Multik plugin;
    private File file;
    private FileConfiguration config;

    public KothConfigManager(Multik plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "koth.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
                config = YamlConfiguration.loadConfiguration(file);
                // Default rewards
                List<String> items = new ArrayList<>();
                items.add("DIAMOND:10");
                items.add("NETHERITE_INGOT:1");
                config.set("rewards.items", items);
                config.set("settings.max_points", 100);
                config.set("settings.radius", 25);
                // NEW: Default coordinates
                config.set("settings.x", 0);
                config.set("settings.y", 60);
                config.set("settings.z", 0);
                config.save(file);
            } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void setRewards(ItemStack[] contents) {
        List<String> itemList = new ArrayList<>();
        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR) {
                itemList.add(item.getType().toString() + ":" + item.getAmount());
            }
        }
        config.set("rewards.items", itemList);
        save();
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<ItemStack> getRewards() {
        List<ItemStack> rewardList = new ArrayList<>();
        for (String s : config.getStringList("rewards.items")) {
            String[] split = s.split(":");
            Material m = Material.matchMaterial(split[0]);
            int amt = (split.length > 1) ? Integer.parseInt(split[1]) : 1;
            if (m != null) rewardList.add(new ItemStack(m, amt));
        }
        return rewardList;
    }

    public int getMaxPoints() { return config.getInt("settings.max_points", 100); }
    public int getRadius() { return config.getInt("settings.radius", 25); }

    // NEW: Getters for coordinates
    public int getCenterX() { return config.getInt("settings.x", 0); }
    public int getCenterY() { return config.getInt("settings.y", 60); }
    public int getCenterZ() { return config.getInt("settings.z", 0); }
}