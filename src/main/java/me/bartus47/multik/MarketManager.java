package me.bartus47.multik;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MarketManager {

    private final Multik plugin;
    private final List<MarketItem> listings = new ArrayList<>();
    private final MarketMenu marketMenu;
    private File dataFile;
    private FileConfiguration dataConfig;

    public MarketManager(Multik plugin) {
        this.plugin = plugin;
        loadMarketData();
        this.marketMenu = new MarketMenu(this);
    }

    public Multik getPlugin() {
        return plugin;
    }

    public void addListing(MarketItem item) {
        listings.add(item);
        saveMarketData();
    }

    public void removeListing(MarketItem item) {
        listings.remove(item);
        saveMarketData();
    }

    public MarketItem getListingById(UUID id) {
        for (MarketItem item : listings) {
            if (item.getId().equals(id)) return item;
        }
        return null;
    }

    public void openMarketGUI(Player player) {
        marketMenu.open(player, 0);
    }

    public void openMarketGUI(Player player, int page) {
        marketMenu.open(player, page);
    }

    public List<MarketItem> getAllListings() {
        return listings;
    }

    public void loadMarketData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        listings.clear();
        if (dataConfig.contains("listings")) {
            for (String key : dataConfig.getConfigurationSection("listings").getKeys(false)) {
                String path = "listings." + key;
                try {
                    UUID seller = UUID.fromString(dataConfig.getString(path + ".seller"));
                    int price = dataConfig.getInt(path + ".price"); // ZMIANA: getDouble -> getInt
                    ItemStack item = dataConfig.getItemStack(path + ".item");
                    listings.add(new MarketItem(UUID.fromString(key), seller, item, price));
                } catch (Exception e) {
                    plugin.getLogger().warning("Blad podczas ladowania oferty: " + key);
                }
            }
        }
    }

    public void saveMarketData() {
        dataConfig.set("listings", null);
        for (MarketItem item : listings) {
            String path = "listings." + item.getId().toString();
            dataConfig.set(path + ".seller", item.getSellerUUID().toString());
            dataConfig.set(path + ".price", item.getPrice());
            dataConfig.set(path + ".item", item.getItemStack());
        }
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }
}