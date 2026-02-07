package me.bartus47.multik;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TeamCommand implements CommandExecutor, TabCompleter {

    private final Multik plugin;
    private final GuildManager guildManager;

    public TeamCommand(Multik plugin, GuildManager guildManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                sendHelp(player);
                break;

            case "item": {
                Gildie g = guildManager.getGuildByPlayer(player.getUniqueId());
                if (g == null) {
                    player.sendMessage(ChatColor.RED + "You must be in a guild to use this!");
                    return true;
                }
                if (args.length > 1 && args[1].equalsIgnoreCase("rewards")) {
                    player.openInventory(plugin.getGuildInventoryManager().getTakeOnlyChest(g.getTag()));
                } else {
                    player.openInventory(plugin.getGuildInventoryManager().getSharedChest(g.getTag()));
                }
                break;
            }

            case "teams":
                player.sendMessage(ChatColor.GOLD + "------- Server Teams -------");
                if (guildManager.getGuilds().isEmpty()) {
                    player.sendMessage(ChatColor.GRAY + "No teams created yet.");
                } else {
                    for (Gildie g : guildManager.getGuilds()) {
                        player.sendMessage(ChatColor.YELLOW + g.getName() + ChatColor.GRAY + " [" + g.getTag() + "] - Points: " + g.getPoints());
                    }
                }
                player.sendMessage(ChatColor.GOLD + "---------------------------");
                break;

            case "pvp": {
                Gildie g = guildManager.getGuildByPlayer(player.getUniqueId());
                if (g == null) {
                    player.sendMessage(ChatColor.RED + "You are not in a team.");
                    return true;
                }
                if (!g.getLeader().equals(player.getUniqueId()) && !g.isCoLeader(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only Leader and Co-Leaders can toggle PvP.");
                    return true;
                }
                boolean newState = !g.isPvpEnabled();
                g.setPvpEnabled(newState);
                guildManager.saveGuilds();
                String status = newState ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED";
                player.sendMessage(ChatColor.YELLOW + "Friendly fire (PvP) is now " + status + ChatColor.YELLOW + " for your team.");
                break;
            }

            case "givepoints": {
                if (!player.isOp()) {
                    player.sendMessage(ChatColor.RED + "Unknown command.");
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /team givepoints <amount> <tag>");
                    return true;
                }
                int amount;
                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid amount.");
                    return true;
                }
                Gildie targetG = guildManager.getGuild(args[2]);
                if (targetG == null) {
                    player.sendMessage(ChatColor.RED + "Guild not found.");
                    return true;
                }
                targetG.addPoints(amount);
                guildManager.saveGuilds();
                player.sendMessage(ChatColor.GREEN + "Added " + amount + " points to guild " + targetG.getTag());
                break;
            }

            case "create": {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /team create <tag> <name>");
                    return true;
                }
                String tag = args[1];
                String name = args[2];
                if (guildManager.getGuildByPlayer(player.getUniqueId()) != null) {
                    player.sendMessage(ChatColor.RED + "You are already in a team!");
                    return true;
                }
                if (guildManager.isTooCloseToSpawn(player.getLocation())) {
                    player.sendMessage(ChatColor.RED + "You cannot create a guild this close to Spawn (0,0).");
                    return true;
                }
                long lastDelete = guildManager.getPlayerDeleteCooldown(player.getUniqueId());
                long timeSinceDelete = System.currentTimeMillis() - lastDelete;
                long oneHour = 3600000L;
                if (timeSinceDelete < oneHour) {
                    long minutesLeft = TimeUnit.MILLISECONDS.toMinutes(oneHour - timeSinceDelete);
                    player.sendMessage(ChatColor.RED + "You must wait " + minutesLeft + " minutes before creating a new one.");
                    return true;
                }
                if (guildManager.isTooCloseToOtherGuild(player.getLocation())) {
                    player.sendMessage(ChatColor.RED + "You are too close to another guild! Minimum distance is 150 blocks.");
                    return true;
                }
                if (guildManager.createGuild(tag, name, player.getUniqueId(), player.getLocation())) {
                    Gildie g = guildManager.getGuild(tag);
                    g.addMember(player.getUniqueId());
                    guildManager.saveGuilds();
                    player.sendMessage(ChatColor.GREEN + "Team " + name + " created!");
                    Bukkit.broadcastMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "GUILD > " + ChatColor.YELLOW + player.getName() + " created a new guild: " + ChatColor.GOLD + name + " [" + tag + "]");
                } else {
                    player.sendMessage(ChatColor.RED + "A team with that tag already exists.");
                }
                break;
            }

            case "add":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /team add <player>");
                    return true;
                }
                Gildie myGuild = guildManager.getGuildByPlayer(player.getUniqueId());
                if (myGuild == null) {
                    player.sendMessage(ChatColor.RED + "You are not in a team!");
                    return true;
                }
                if (!myGuild.getLeader().equals(player.getUniqueId()) && !myGuild.isCoLeader(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only Leader or Co-Leaders can invite players.");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Player not found or offline.");
                    return true;
                }
                if (guildManager.getGuildByPlayer(target.getUniqueId()) != null) {
                    player.sendMessage(ChatColor.RED + "That player is already in a team.");
                    return true;
                }
                guildManager.sendInvite(target.getUniqueId(), myGuild.getTag());
                player.sendMessage(ChatColor.GREEN + "Invite sent to " + target.getName() + "!");
                target.sendMessage(ChatColor.AQUA + "You have been invited to join team " + ChatColor.GOLD + myGuild.getName() + ChatColor.AQUA + "!");
                target.sendMessage(ChatColor.AQUA + "Type " + ChatColor.WHITE + "/team accept " + myGuild.getTag() + ChatColor.AQUA + " to join.");
                break;

            case "accept":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /team accept <tag>");
                    return true;
                }
                String acceptTag = args[1];
                if (!guildManager.hasInvite(player.getUniqueId(), acceptTag)) {
                    player.sendMessage(ChatColor.RED + "You do not have an invite for that team.");
                    return true;
                }
                Gildie guildToJoin = guildManager.getGuild(acceptTag);
                if (guildToJoin == null) {
                    player.sendMessage(ChatColor.RED + "That team no longer exists.");
                    return true;
                }
                if (guildManager.getGuildByPlayer(player.getUniqueId()) != null) {
                    player.sendMessage(ChatColor.RED + "You are already in a team!");
                    return true;
                }
                guildToJoin.addMember(player.getUniqueId());
                guildManager.removeInvite(player.getUniqueId(), acceptTag);
                guildManager.saveGuilds();
                player.sendMessage(ChatColor.GREEN + "You have joined " + guildToJoin.getName() + "!");
                Bukkit.broadcastMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "GUILD > " + ChatColor.YELLOW + player.getName() + " has joined guild " + ChatColor.GOLD + guildToJoin.getTag() + "!");
                break;

            case "leave":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /team leave <tag>");
                    return true;
                }
                String tagToLeave = args[1];
                Gildie currentGuild = guildManager.getGuildByPlayer(player.getUniqueId());
                if (currentGuild == null) {
                    player.sendMessage(ChatColor.RED + "You are not in a team.");
                    return true;
                }
                if (!currentGuild.getTag().equalsIgnoreCase(tagToLeave)) {
                    player.sendMessage(ChatColor.RED + "You are not in the team '" + tagToLeave + "'.");
                    return true;
                }
                if (currentGuild.getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You are the Leader! You cannot leave. Use /team delete to disband.");
                    return true;
                }
                currentGuild.removeMember(player.getUniqueId());
                guildManager.saveGuilds();
                player.sendMessage(ChatColor.GREEN + "You have left " + currentGuild.getName() + ".");
                Bukkit.broadcastMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "GUILD > " + ChatColor.YELLOW + player.getName() + " has left guild " + ChatColor.GOLD + currentGuild.getTag() + "!");
                break;

            case "remove":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /team remove <player>");
                    return true;
                }
                Gildie removeGuild = guildManager.getGuildByPlayer(player.getUniqueId());
                if (removeGuild == null) {
                    player.sendMessage(ChatColor.RED + "You are not in a team!");
                    return true;
                }
                if (!removeGuild.getLeader().equals(player.getUniqueId()) && !removeGuild.isCoLeader(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only Leader or Co-Leaders can remove players.");
                    return true;
                }
                OfflinePlayer targetToRemove = Bukkit.getOfflinePlayer(args[1]);
                if (!removeGuild.getMembers().contains(targetToRemove.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "That player is not in your team.");
                    return true;
                }
                if (targetToRemove.getUniqueId().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You cannot remove yourself.");
                    return true;
                }
                if (removeGuild.isCoLeader(player.getUniqueId()) && removeGuild.isCoLeader(targetToRemove.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Co-Leaders cannot remove other Co-Leaders.");
                    return true;
                }
                removeGuild.removeMember(targetToRemove.getUniqueId());
                guildManager.saveGuilds();
                player.sendMessage(ChatColor.GREEN + "Removed " + targetToRemove.getName() + " from the team.");
                Bukkit.broadcastMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "GUILD > " + ChatColor.YELLOW + (targetToRemove.getName() != null ? targetToRemove.getName() : "Unknown") + " was removed from guild " + ChatColor.GOLD + removeGuild.getTag() + "!");
                break;

            case "changeleader":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /team changeleader <player>");
                    return true;
                }
                Gildie leaderGuild = guildManager.getGuildByPlayer(player.getUniqueId());
                if (leaderGuild == null) {
                    player.sendMessage(ChatColor.RED + "You are not in a team.");
                    return true;
                }
                if (!leaderGuild.getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only the current Leader can change the leader.");
                    return true;
                }
                OfflinePlayer newLeader = Bukkit.getOfflinePlayer(args[1]);
                if (!leaderGuild.getMembers().contains(newLeader.getUniqueId()) && !newLeader.getUniqueId().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "That player is not a member of your guild.");
                    return true;
                }
                leaderGuild.setLeader(newLeader.getUniqueId());
                leaderGuild.removeCoLeader(newLeader.getUniqueId());
                guildManager.saveGuilds();
                player.sendMessage(ChatColor.GREEN + "You have transferred leadership to " + newLeader.getName() + ".");
                if (newLeader.isOnline()) {
                    ((Player) newLeader).sendMessage(ChatColor.GREEN + "You are now the Leader of " + leaderGuild.getName() + "!");
                }
                break;

            case "advance":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /team advance <player>");
                    return true;
                }
                Gildie advGuild = guildManager.getGuildByPlayer(player.getUniqueId());
                if (advGuild == null) { player.sendMessage(ChatColor.RED + "Not in a team."); return true; }
                if (!advGuild.getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only the Leader can advance members.");
                    return true;
                }
                OfflinePlayer targetAdv = Bukkit.getOfflinePlayer(args[1]);
                if (!advGuild.getMembers().contains(targetAdv.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Player not in guild.");
                    return true;
                }
                if (advGuild.isCoLeader(targetAdv.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Player is already a Co-Leader.");
                    return true;
                }
                if (targetAdv.getUniqueId().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Cannot advance yourself.");
                    return true;
                }
                advGuild.addCoLeader(targetAdv.getUniqueId());
                guildManager.saveGuilds();
                player.sendMessage(ChatColor.GREEN + "Promoted " + targetAdv.getName() + " to Co-Leader.");
                if (targetAdv.isOnline()) ((Player) targetAdv).sendMessage(ChatColor.GREEN + "You were promoted to Co-Leader!");
                break;

            case "degrade":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /team degrade <player>");
                    return true;
                }
                Gildie degGuild = guildManager.getGuildByPlayer(player.getUniqueId());
                if (degGuild == null) { player.sendMessage(ChatColor.RED + "Not in a team."); return true; }
                if (!degGuild.getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only the Leader can degrade members.");
                    return true;
                }
                OfflinePlayer targetDeg = Bukkit.getOfflinePlayer(args[1]);
                if (!degGuild.isCoLeader(targetDeg.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Player is not a Co-Leader.");
                    return true;
                }
                degGuild.removeCoLeader(targetDeg.getUniqueId());
                guildManager.saveGuilds();
                player.sendMessage(ChatColor.GREEN + "Demoted " + targetDeg.getName() + " to Member.");
                if (targetDeg.isOnline()) ((Player) targetDeg).sendMessage(ChatColor.RED + "You were demoted to Member.");
                break;

            case "delete": {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /team delete <tag>");
                    return true;
                }
                String tagToDelete = args[1];
                Gildie guildToDelete = guildManager.getGuild(tagToDelete);
                if (guildToDelete == null) {
                    player.sendMessage(ChatColor.RED + "Guild not found.");
                    return true;
                }
                if (!guildToDelete.getLeader().equals(player.getUniqueId()) && !player.isOp()) {
                    player.sendMessage(ChatColor.RED + "Only the Leader can delete this team.");
                    return true;
                }
                long age = System.currentTimeMillis() - guildToDelete.getCreationTime();
                long oneHour = 3600000L;
                if (age < oneHour && !player.isOp()) {
                    long minutesLeft = TimeUnit.MILLISECONDS.toMinutes(oneHour - age);
                    player.sendMessage(ChatColor.RED + "Guild is too new! You must wait " + minutesLeft + " more minutes.");
                    return true;
                }
                if (guildToDelete.getPoints() < 900) {
                    player.sendMessage(ChatColor.RED + "You cannot delete this guild! It has less than 900 points.");
                    return true;
                }
                if (guildManager.deleteGuild(tagToDelete)) {
                    guildManager.setPlayerDeleteCooldown(player.getUniqueId());
                    player.sendMessage(ChatColor.GREEN + "Guild " + tagToDelete + " deleted.");
                    Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "GUILD > " + ChatColor.YELLOW + "Guild " + ChatColor.GOLD + guildToDelete.getName() + " [" + guildToDelete.getTag() + "]" + ChatColor.YELLOW + " has been disbanded!");
                }
                break;
            }

            case "resettimers": {
                if (!player.isOp()) {
                    player.sendMessage(ChatColor.RED + "Command not found.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /team resettimers <player|tag>");
                    return true;
                }
                String targetArg = args[1];
                Gildie g = guildManager.getGuild(targetArg);
                if (g != null) {
                    g.setCreationTime(0);
                    guildManager.saveGuilds();
                    player.sendMessage(ChatColor.GREEN + "Reset creation timer for guild " + g.getName());
                    return true;
                }
                OfflinePlayer targetP = Bukkit.getOfflinePlayer(targetArg);
                guildManager.resetPlayerDeleteCooldown(targetP.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "Reset delete cooldown for player " + targetArg);
                break;
            }

            case "info":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /team info <tag>");
                    return true;
                }
                Gildie infoGuild = guildManager.getGuild(args[1]);
                if (infoGuild == null) { player.sendMessage(ChatColor.RED + "Team not found."); return true; }
                player.sendMessage(ChatColor.GOLD + "--- " + infoGuild.getName() + " [" + infoGuild.getTag() + "] ---");
                OfflinePlayer infoLeader = Bukkit.getOfflinePlayer(infoGuild.getLeader());
                player.sendMessage(ChatColor.YELLOW + "Leader: " + (infoLeader.getName() != null ? infoLeader.getName() : "Unknown"));
                player.sendMessage(ChatColor.YELLOW + "Points: " + infoGuild.getPoints());
                player.sendMessage(ChatColor.YELLOW + "PvP: " + (infoGuild.isPvpEnabled() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
                player.sendMessage(ChatColor.YELLOW + "Location: " + infoGuild.getWorldName() + " (" + infoGuild.getCenterX() + ", " + infoGuild.getCenterZ() + ")");
                player.sendMessage(ChatColor.YELLOW + "Members:");
                for (UUID memberUUID : infoGuild.getMembers()) {
                    OfflinePlayer p = Bukkit.getOfflinePlayer(memberUUID);
                    String prefix = ChatColor.GRAY + "-";
                    if (memberUUID.equals(infoGuild.getLeader())) prefix = ChatColor.GOLD + "*";
                    else if (infoGuild.isCoLeader(memberUUID)) prefix = ChatColor.GREEN + "+";
                    player.sendMessage(prefix + " " + (p.getName() != null ? p.getName() : "Unknown"));
                }
                break;

            default:
                player.sendMessage(ChatColor.RED + "Command not found. Type /team help for help.");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("help", "teams", "create", "add", "remove", "leave", "accept", "changeleader", "info", "delete", "advance", "degrade", "item", "pvp"));
            if (sender.isOp()) {
                subCommands.add("resettimers");
                subCommands.add("givepoints");
            }
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("item")) {
                completions.add("rewards");
            } else if (Arrays.asList("add", "remove", "changeleader", "advance", "degrade", "resettimers").contains(sub)) {
                List<String> playerNames = new ArrayList<>();
                Bukkit.getOnlinePlayers().forEach(p -> playerNames.add(p.getName()));
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
            } else if (Arrays.asList("accept", "info", "delete", "leave", "resettimers", "givepoints").contains(sub)) {
                List<String> guildTags = new ArrayList<>();
                guildManager.getGuilds().forEach(g -> guildTags.add(g.getTag()));
                StringUtil.copyPartialMatches(args[1], guildTags, completions);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("givepoints") && sender.isOp()) {
            List<String> guildTags = new ArrayList<>();
            guildManager.getGuilds().forEach(g -> guildTags.add(g.getTag()));
            StringUtil.copyPartialMatches(args[2], guildTags, completions);
        }
        Collections.sort(completions);
        return completions;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "------- Team Commands -------");
        player.sendMessage(ChatColor.YELLOW + "/team help " + ChatColor.WHITE + "- List commands");
        player.sendMessage(ChatColor.YELLOW + "/team teams " + ChatColor.WHITE + "- List all teams");
        player.sendMessage(ChatColor.YELLOW + "/team create <tag> <name> " + ChatColor.WHITE + "- Create a new team");
        player.sendMessage(ChatColor.YELLOW + "/team add <player> " + ChatColor.WHITE + "- Invite a player");
        player.sendMessage(ChatColor.YELLOW + "/team remove <player> " + ChatColor.WHITE + "- Remove a player");
        player.sendMessage(ChatColor.YELLOW + "/team advance <player> " + ChatColor.WHITE + "- Promote to Co-Leader");
        player.sendMessage(ChatColor.YELLOW + "/team degrade <player> " + ChatColor.WHITE + "- Demote Co-Leader");
        player.sendMessage(ChatColor.YELLOW + "/team leave <tag> " + ChatColor.WHITE + "- Leave your team");
        player.sendMessage(ChatColor.YELLOW + "/team accept <tag> " + ChatColor.WHITE + "- Accept an invite");
        player.sendMessage(ChatColor.YELLOW + "/team changeleader <player> " + ChatColor.WHITE + "- Transfer leadership");
        player.sendMessage(ChatColor.YELLOW + "/team info <tag> " + ChatColor.WHITE + "- View team info");
        player.sendMessage(ChatColor.YELLOW + "/team item " + ChatColor.WHITE + "- Open shared chest");
        player.sendMessage(ChatColor.YELLOW + "/team item rewards " + ChatColor.WHITE + "- Open rewards chest");
        player.sendMessage(ChatColor.YELLOW + "/team pvp " + ChatColor.WHITE + "- Toggle friendly fire");
        player.sendMessage(ChatColor.YELLOW + "/team delete <tag> " + ChatColor.WHITE + "- Delete team");
        player.sendMessage(ChatColor.GOLD + "-----------------------------");
    }
}