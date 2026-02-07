package me.bartus47.multik;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
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
            sender.sendMessage(ChatColor.RED + "Usage: /chests <activate|disable>");
            return true;
        }

        if (args[0].equalsIgnoreCase("activate")) {
            configManager.load();
            configManager.setActive(true);
            plugin.getLootManager().startAllTimers();
            sender.sendMessage(ChatColor.GREEN + "Loot system ACTIVATED. First chests will spawn after their intervals.");
        } else if (args[0].equalsIgnoreCase("disable")) {
            configManager.setActive(false);
            plugin.getLootManager().stopAllTimers();
            sender.sendMessage(ChatColor.RED + "Loot system DISABLED.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.isOp()) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("activate", "disable"), new ArrayList<>());
        }
        return Collections.emptyList();
    }
}