package me.bartus47.multik;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class MarketListener implements Listener {

    private final MarketManager marketManager;
    private final GuildManager guildManager;
    private final String PREFIX = ChatColor.GOLD + "[Rynek] " + ChatColor.GRAY;

    public MarketListener(MarketManager marketManager, GuildManager guildManager) {
        this.marketManager = marketManager;
        this.guildManager = guildManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (!title.startsWith("Rynek")) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        if (event.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        int slot = event.getSlot();
        if (slot >= 45) {
            handleNavigationClick(player, clicked, event.getView().getTitle());
            return;
        }

        handleListingClick(player, clicked);
    }

    private void handleNavigationClick(Player player, ItemStack clicked, String inventoryTitle) {
        int currentPage;
        try {
            String cleanTitle = ChatColor.stripColor(inventoryTitle);
            String pageStr = cleanTitle.substring(cleanTitle.indexOf("Strona ") + 7).replace(")", "");
            currentPage = Integer.parseInt(pageStr) - 1;
        } catch (Exception e) {
            currentPage = 0;
        }

        if (clicked.getType() == Material.ARROW) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            if (clicked.getItemMeta().getDisplayName().contains("Następna")) {
                marketManager.openMarketGUI(player, currentPage + 1);
            } else if (clicked.getItemMeta().getDisplayName().contains("Poprzednia")) {
                marketManager.openMarketGUI(player, currentPage - 1);
            }
        } else if (clicked.getType() == Material.BOOK) {
            marketManager.openMarketGUI(player, currentPage);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
    }

    private void handleListingClick(Player buyer, ItemStack clicked) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = new NamespacedKey(marketManager.getPlugin(), "market-id");
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return;
        }

        String idStr = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        UUID listingID;
        try {
            listingID = UUID.fromString(idStr);
        } catch (IllegalArgumentException e) {
            return;
        }

        MarketItem offer = marketManager.getListingById(listingID);
        if (offer == null) {
            buyer.sendMessage(PREFIX + ChatColor.RED + "Ta oferta już nie istnieje.");
            buyer.playSound(buyer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            buyer.closeInventory();
            return;
        }

        // WYCOFANIE PRZEDMIOTU
        if (offer.getSellerUUID().equals(buyer.getUniqueId())) {
            buyer.getInventory().addItem(offer.getItemStack());
            marketManager.removeListing(offer);

            buyer.sendMessage(PREFIX + ChatColor.YELLOW + "Wycofałeś swój przedmiot z rynku.");
            buyer.playSound(buyer.getLocation(), Sound.UI_LOOM_TAKE_RESULT, 1f, 1f);
            buyer.closeInventory();
            return;
        }

        // --- KUPOWANIE ZA PUNKTY GILDII ---

        // 1. Sprawdź czy kupujący ma gildię
        Gildie buyerGuild = guildManager.getGuildByPlayer(buyer.getUniqueId());
        if (buyerGuild == null) {
            buyer.sendMessage(PREFIX + ChatColor.RED + "Musisz posiadać gildię, aby kupować na rynku!");
            buyer.playSound(buyer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 2. NOWE: Sprawdź rangę kupującego (Lider/Co-Lider)
        if (!buyerGuild.getLeader().equals(buyer.getUniqueId()) && !buyerGuild.isCoLeader(buyer.getUniqueId())) {
            buyer.sendMessage(PREFIX + ChatColor.RED + "Tylko Lider i Co-Liderzy mogą wydawać punkty gildii!");
            buyer.playSound(buyer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 3. Sprawdź czy sprzedający ma gildię
        Gildie sellerGuild = guildManager.getGuildByPlayer(offer.getSellerUUID());
        if (sellerGuild == null) {
            buyer.sendMessage(PREFIX + ChatColor.RED + "Sprzedawca nie posiada już gildii. Oferta jest zablokowana.");
            buyer.playSound(buyer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 4. Sprawdź czy gildia kupującego ma punkty
        int price = offer.getPrice();
        if (buyerGuild.getPoints() < price) {
            buyer.sendMessage(PREFIX + ChatColor.RED + "Twoja gildia ma za mało punktów! (Wymagane: " + price + ")");
            buyer.playSound(buyer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 5. Transakcja
        buyerGuild.removePoints(price);
        sellerGuild.addPoints(price);
        guildManager.saveGuilds();

        buyer.getInventory().addItem(offer.getItemStack());
        marketManager.removeListing(offer);

        buyer.sendMessage(PREFIX + ChatColor.GREEN + "Zakupiono przedmiot! " + ChatColor.GOLD + "-" + price + " pkt gildii.");
        buyer.playSound(buyer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        Player seller = Bukkit.getPlayer(offer.getSellerUUID());
        if (seller != null) {
            seller.sendMessage(PREFIX + ChatColor.GREEN + buyer.getName() + " kupił Twój przedmiot. " + ChatColor.GOLD + "+" + price + " pkt dla Twojej gildii.");
            seller.playSound(seller.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
        }

        buyer.closeInventory();
    }
}