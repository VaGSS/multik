package me.bartus47.multik;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChestCommand implements CommandExecutor, TabCompleter {
    private final Multik plugin;
    private final ChestConfigManager configManager;

    public ChestCommand(Multik plugin, ChestConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Only operators can use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /chests <activate|disable|setloot>");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("activate")) {
            configManager.load();
            configManager.setActive(true);
            plugin.getLootManager().startAllTimers();
            sender.sendMessage(ChatColor.GREEN + "Loot system ACTIVATED. First chests will spawn after their intervals.");
        } else if (sub.equals("disable")) {
            configManager.setActive(false);
            plugin.getLootManager().stopAllTimers();
            sender.sendMessage(ChatColor.RED + "Loot system DISABLED.");
        } else if (sub.equals("setloot")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /chests setloot <chestTypeName>");
                sender.sendMessage(ChatColor.GRAY + "Look at a chest containing the items you want to save.");
                return true;
            }

            Player player = (Player) sender;
            Block target = player.getTargetBlockExact(5);

            if (target == null || target.getType() != Material.CHEST) {
                player.sendMessage(ChatColor.RED + "You must be looking at a Chest block!");
                return true;
            }

            String chestType = args[1];
            Chest chest = (Chest) target.getState();
            Inventory inv = chest.getInventory();

            configManager.updateChestLoot(chestType, inv.getContents());
            player.sendMessage(ChatColor.GREEN + "Saved contents to chest type: " + ChatColor.GOLD + chestType);
            player.sendMessage(ChatColor.GRAY + "You can edit interval/delays in chests.yml manually.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.isOp()) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("activate", "disable", "setloot"), new ArrayList<>());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setloot") && sender.isOp()) {
            return StringUtil.copyPartialMatches(args[1], configManager.getChestKeys(), new ArrayList<>());
        }
        return Collections.emptyList();
    }
}