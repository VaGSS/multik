package me.bartus47.multik;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class KothCommand implements CommandExecutor {
    private final KothManager kothManager;

    public KothCommand(KothManager kothManager) {
        this.kothManager = kothManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Only operators can use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /koth <activate|activatenow|disable>");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("activate")) {
            kothManager.scheduleStart();
            sender.sendMessage(ChatColor.GREEN + "KOTH countdown started (30m).");
        }
        else if (sub.equals("activatenow")) {
            kothManager.startKoth();
            sender.sendMessage(ChatColor.GOLD + "KOTH started INSTANTLY!");
        }
        else if (sub.equals("disable")) {
            kothManager.stopEvent();
            sender.sendMessage(ChatColor.RED + "KOTH stopped.");
        }
        return true;
    }
}