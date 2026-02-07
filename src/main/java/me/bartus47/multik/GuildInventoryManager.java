package me.bartus47.multik;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuildInventoryManager {

    private final Multik plugin;
    private final Map<String, Inventory> sharedChests = new HashMap<>();
    private final Map<String, Inventory> takeOnlyChests = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public GuildInventoryManager(Multik plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "inventories.yml");
        loadInventories();
    }

    public Inventory getSharedChest(String tag) {
        return sharedChests.computeIfAbsent(tag.toLowerCase(), k ->
                Bukkit.createInventory(null, 54, "Guild Chest: " + tag.toUpperCase())); // 54 slots
    }

    public Inventory getTakeOnlyChest(String tag) {
        return takeOnlyChests.computeIfAbsent(tag.toLowerCase(), k ->
                Bukkit.createInventory(null, 27, "Guild Rewards: " + tag.toUpperCase())); // 27 slots
    }

    public void saveInventories() {
        config = new YamlConfiguration();
        saveMapToConfig(sharedChests, "shared");
        saveMapToConfig(takeOnlyChests, "takeonly");
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    private void saveMapToConfig(Map<String, Inventory> map, String path) {
        for (Map.Entry<String, Inventory> entry : map.entrySet()) {
            config.set(path + "." + entry.getKey(), entry.getValue().getContents());
        }
    }

    private void loadInventories() {
        if (!file.exists()) return;
        config = YamlConfiguration.loadConfiguration(file);

        loadSection("shared", sharedChests, 54, "Guild Chest: ");
        loadSection("takeonly", takeOnlyChests, 27, "Guild Rewards: ");
    }

    private void loadSection(String path, Map<String, Inventory> map, int size, String title) {
        if (config.getConfigurationSection(path) == null) return;
        for (String tag : config.getConfigurationSection(path).getKeys(false)) {
            Inventory inv = Bukkit.createInventory(null, size, title + tag.toUpperCase());
            List<?> list = config.getList(path + "." + tag);
            if (list != null) {
                ItemStack[] items = list.toArray(new ItemStack[0]);
                inv.setContents(items);
            }
            map.put(tag.toLowerCase(), inv);
        }
    }
}