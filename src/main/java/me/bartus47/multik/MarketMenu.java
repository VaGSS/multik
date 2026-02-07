package me.bartus47.multik;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class MarketMenu {

    private final MarketManager marketManager;

    public MarketMenu(MarketManager marketManager) {
        this.marketManager = marketManager;
    }

    public void open(Player player, int page) {
        List<MarketItem> allListings = marketManager.getAllListings();

        int itemsPerPage = 45;
        int totalItems = allListings.size();
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);

        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Rynek ofert (Strona " + (page + 1) + ")");

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            MarketItem listing = allListings.get(i);
            inv.setItem(slotIndex, createListingItem(listing, player));
            slotIndex++;
        }

        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
        }

        if (page > 0) {
            inv.setItem(45, createGuiItem(Material.ARROW, ChatColor.YELLOW + "« Poprzednia strona",
                    ChatColor.GRAY + "Kliknij, aby cofnąć"));
        }

        inv.setItem(49, createGuiItem(Material.BOOK, ChatColor.GOLD + "Strona " + (page + 1) + " / " + totalPages,
                ChatColor.GRAY + "Ilość ofert: " + ChatColor.WHITE + totalItems));

        if (page < totalPages - 1) {
            inv.setItem(53, createGuiItem(Material.ARROW, ChatColor.YELLOW + "Następna strona »",
                    ChatColor.GRAY + "Kliknij, aby przejść dalej"));
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    private ItemStack createListingItem(MarketItem listing, Player viewer) {
        ItemStack item = listing.getItemStack().clone();
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        lore.add(" ");
        lore.add(ChatColor.DARK_GRAY + "----------------------");

        boolean isOwner = listing.getSellerUUID().equals(viewer.getUniqueId());

        if (isOwner) {
            lore.add(ChatColor.GRAY + "Sprzedawca: " + ChatColor.GREEN + "TY (Twoja oferta)");
        } else {
            GuildManager gm = marketManager.getPlugin().getGuildManager();
            Gildie sellerGuild = gm.getGuildByPlayer(listing.getSellerUUID());

            String sellerDisplay;
            if (sellerGuild != null) {
                // ZMIANA: Tutaj też bez nawiasów i wielkimi literami dla spójności
                // Format: TAG NazwaGildii
                sellerDisplay = ChatColor.GOLD + sellerGuild.getTag().toUpperCase() + " " + sellerGuild.getName();
            } else {
                sellerDisplay = ChatColor.RED + Bukkit.getOfflinePlayer(listing.getSellerUUID()).getName();
            }
            lore.add(ChatColor.GRAY + "Sprzedawca: " + sellerDisplay);
        }

        lore.add(ChatColor.GRAY + "Cena: " + ChatColor.GREEN + listing.getPrice() + " pkt gildii");
        lore.add(ChatColor.DARK_GRAY + "----------------------");

        if (isOwner) {
            lore.add(ChatColor.RED + "" + ChatColor.BOLD + "Kliknij, aby WYCOFAĆ!");
        } else {
            lore.add(ChatColor.YELLOW + "Kliknij, aby kupić!");
        }

        NamespacedKey key = new NamespacedKey(marketManager.getPlugin(), "market-id");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, listing.getId().toString());

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            meta.setLore(List.of(lore));
        }
        item.setItemMeta(meta);
        return item;
    }
}