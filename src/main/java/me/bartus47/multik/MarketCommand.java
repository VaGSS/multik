package me.bartus47.multik;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MarketCommand implements CommandExecutor, TabCompleter {

    private final MarketManager marketManager;
    private final GuildManager guildManager; // NOWE: Potrzebne do sprawdzania rang
    private final String PREFIX = ChatColor.GOLD + "[Rynek] " + ChatColor.GRAY;

    // ZMIANA: Konstruktor teraz przyjmuje GuildManager
    public MarketCommand(MarketManager marketManager, GuildManager guildManager) {
        this.marketManager = marketManager;
        this.guildManager = guildManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Tylko dla graczy!");
            return true;
        }

        if (args.length == 0) {
            marketManager.openMarketGUI(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("pomoc") || args[0].equalsIgnoreCase("?")) {
            sendHelpMessage(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("sprzedaj")) {
            // --- NOWE: Sprawdzanie uprawnień (Lider/Co-Lider) ---
            Gildie playerGuild = guildManager.getGuildByPlayer(player.getUniqueId());
            if (playerGuild == null) {
                player.sendMessage(PREFIX + ChatColor.RED + "Musisz należeć do gildii, aby handlować!");
                return true;
            }
            if (!playerGuild.getLeader().equals(player.getUniqueId()) && !playerGuild.isCoLeader(player.getUniqueId())) {
                player.sendMessage(PREFIX + ChatColor.RED + "Tylko Lider i Co-Liderzy mogą wystawiać przedmioty na rynek!");
                return true;
            }
            // ----------------------------------------------------

            if (args.length < 2) {
                player.sendMessage(PREFIX + ChatColor.RED + "Użycie: /rynek sprzedaj <cena> <ilosc>");
                return true;
            }

            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (handItem.getType() == Material.AIR) {
                player.sendMessage(PREFIX + ChatColor.RED + "Musisz trzymać przedmiot w ręce!");
                return true;
            }

            int price;
            try {
                price = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(PREFIX + ChatColor.RED + "Cena musi być liczbą całkowitą!");
                return true;
            }

            if (price <= 0) {
                player.sendMessage(PREFIX + ChatColor.RED + "Cena musi być dodatnia!");
                return true;
            }

            int amountToSell = handItem.getAmount();
            if (args.length >= 3) {
                try {
                    int requestedAmount = Integer.parseInt(args[2]);
                    if (requestedAmount <= 0 || requestedAmount > handItem.getAmount()) {
                        player.sendMessage(PREFIX + ChatColor.RED + "Nieprawidłowa ilość! Masz w ręce: " + handItem.getAmount());
                        return true;
                    }
                    amountToSell = requestedAmount;
                } catch (NumberFormatException e) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Ilość musi być liczbą całkowitą!");
                    return true;
                }
            }

            ItemStack itemToMarket = handItem.clone();
            itemToMarket.setAmount(amountToSell);

            marketManager.addListing(new MarketItem(player.getUniqueId(), itemToMarket, price));

            if (amountToSell == handItem.getAmount()) {
                player.getInventory().setItemInMainHand(null);
            } else {
                handItem.setAmount(handItem.getAmount() - amountToSell);
            }

            player.sendMessage(PREFIX + "Wystawiono " + ChatColor.WHITE + amountToSell + "x " + itemToMarket.getType().toString()
                    + ChatColor.GRAY + " za " + ChatColor.GOLD + price + " pkt gildii.");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.5f);
            return true;
        }

        sendHelpMessage(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> commands = List.of("sprzedaj", "pomoc");
            StringUtil.copyPartialMatches(args[0], commands, completions);
        }
        else if (args.length == 2 && args[0].equalsIgnoreCase("sprzedaj")) {
            completions.add("<cena>");
        }
        else if (args.length == 3 && args[0].equalsIgnoreCase("sprzedaj")) {
            completions.add("<ilosc>");
        }
        Collections.sort(completions);
        return completions;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
        player.sendMessage(ChatColor.GOLD + "       RYNEK - POMOC       ");
        player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
        player.sendMessage(ChatColor.YELLOW + "/rynek" + ChatColor.GRAY + " - Otwórz rynek");
        player.sendMessage(ChatColor.YELLOW + "/rynek sprzedaj <cena> [ilosc]" + ChatColor.GRAY + " - Sprzedaj przedmiot (Lider/Co-Lider)");
        player.sendMessage(ChatColor.YELLOW + "/team info <tag>" + ChatColor.GRAY + " - Sprawdź punkty swojej gildii");
        player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
    }
}