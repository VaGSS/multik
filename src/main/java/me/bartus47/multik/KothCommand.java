package me.bartus47.multik;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

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
            sender.sendMessage(ChatColor.RED + "Usage: /koth <activate|activatenow|disable|setloot>");
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
        else if (sub.equals("setloot")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            Player player = (Player) sender;
            Block target = player.getTargetBlockExact(5);

            if (target == null || target.getType() != Material.CHEST) {
                player.sendMessage(ChatColor.RED + "You must be looking at a Chest block!");
                return true;
            }

            Chest chest = (Chest) target.getState();
            Inventory inv = chest.getInventory();

            kothManager.getConfigManager().setRewards(inv.getContents());
            player.sendMessage(ChatColor.GREEN + "KOTH rewards updated from chest contents!");
            player.sendMessage(ChatColor.GRAY + "You can edit points/radius in koth.yml manually.");
        }
        return true;
    }
}