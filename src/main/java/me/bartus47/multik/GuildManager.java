package me.bartus47.multik;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GuildManager {
    private final Multik plugin;
    private final Map<String, Gildie> guilds = new HashMap<>();
    private final Map<UUID, Set<String>> pendingInvites = new HashMap<>();
    private final Map<UUID, Long> playerDeleteCooldowns = new HashMap<>();

    private File file;
    private FileConfiguration config;

    public GuildManager(Multik plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "guilds.yml");
        loadGuilds();
    }

    public void setPlayerDeleteCooldown(UUID playerUUID) {
        playerDeleteCooldowns.put(playerUUID, System.currentTimeMillis());
        saveGuilds();
    }

    public long getPlayerDeleteCooldown(UUID playerUUID) {
        return playerDeleteCooldowns.getOrDefault(playerUUID, 0L);
    }

    public void resetPlayerDeleteCooldown(UUID playerUUID) {
        playerDeleteCooldowns.remove(playerUUID);
        saveGuilds();
    }

    public void sendInvite(UUID playerUUID, String tag) {
        pendingInvites.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(tag.toLowerCase());
    }

    public boolean hasInvite(UUID playerUUID, String tag) {
        Set<String> invites = pendingInvites.get(playerUUID);
        return invites != null && invites.contains(tag.toLowerCase());
    }

    public void removeInvite(UUID playerUUID, String tag) {
        Set<String> invites = pendingInvites.get(playerUUID);
        if (invites != null) {
            invites.remove(tag.toLowerCase());
            if (invites.isEmpty()) pendingInvites.remove(playerUUID);
        }
    }

    public Collection<Gildie> getGuilds() {
        return guilds.values();
    }

    public boolean isTooCloseToSpawn(Location loc) {
        double distance = Math.sqrt(Math.pow(loc.getBlockX(), 2) + Math.pow(loc.getBlockZ(), 2));
        return distance < 300;
    }

    public boolean isTooCloseToOtherGuild(Location loc) {
        for (Gildie g : guilds.values()) {
            if (!g.getWorldName().equals(loc.getWorld().getName())) continue;
            double distance = Math.sqrt(Math.pow(g.getCenterX() - loc.getBlockX(), 2) + Math.pow(g.getCenterZ() - loc.getBlockZ(), 2));
            if (distance < 150) return true;
        }
        return false;
    }

    public boolean createGuild(String tag, String name, UUID leader, Location center) {
        if (guilds.containsKey(tag.toLowerCase())) return false;
        Gildie newGuild = new Gildie(tag, name, leader, center);
        guilds.put(tag.toLowerCase(), newGuild);
        saveGuilds();
        return true;
    }

    public boolean deleteGuild(String tag) {
        if (!guilds.containsKey(tag.toLowerCase())) return false;
        guilds.remove(tag.toLowerCase());
        saveGuilds();
        return true;
    }

    public Gildie getGuild(String tag) { return guilds.get(tag.toLowerCase()); }

    public Gildie getGuildByPlayer(UUID uuid) {
        for (Gildie g : guilds.values()) {
            if (g.getMembers().contains(uuid) || g.getLeader().equals(uuid)) return g;
        }
        return null;
    }

    public Gildie getGuildAtLocation(Location loc) {
        for (Gildie g : guilds.values()) {
            if (g.isInside(loc)) return g;
        }
        return null;
    }

    public void saveGuilds() {
        config = new YamlConfiguration();
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }

        for (Gildie g : guilds.values()) {
            String path = "guilds." + g.getTag();
            config.set(path + ".name", g.getName());
            config.set(path + ".leader", g.getLeader().toString());
            config.set(path + ".world", g.getWorldName());
            config.set(path + ".x", g.getCenterX());
            config.set(path + ".z", g.getCenterZ());
            config.set(path + ".points", g.getPoints());
            config.set(path + ".created", g.getCreationTime());
            config.set(path + ".pvp", g.isPvpEnabled()); // SAVE PVP

            List<String> memberStrings = new ArrayList<>();
            for (UUID uuid : g.getMembers()) memberStrings.add(uuid.toString());
            config.set(path + ".members", memberStrings);

            List<String> coStrings = new ArrayList<>();
            for (UUID uuid : g.getCoLeaders()) coStrings.add(uuid.toString());
            config.set(path + ".coleaders", coStrings);
        }
        for (Map.Entry<UUID, Long> entry : playerDeleteCooldowns.entrySet()) {
            config.set("cooldowns." + entry.getKey().toString(), entry.getValue());
        }
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void loadGuilds() {
        if (!file.exists()) return;
        config = YamlConfiguration.loadConfiguration(file);

        if (config.getConfigurationSection("cooldowns") != null) {
            for (String uuidStr : config.getConfigurationSection("cooldowns").getKeys(false)) {
                playerDeleteCooldowns.put(UUID.fromString(uuidStr), config.getLong("cooldowns." + uuidStr));
            }
        }

        if (config.getConfigurationSection("guilds") == null) return;

        for (String tag : config.getConfigurationSection("guilds").getKeys(false)) {
            String path = "guilds." + tag;
            String name = config.getString(path + ".name");
            String wName = config.getString(path + ".world");
            int x = config.getInt(path + ".x");
            int z = config.getInt(path + ".z");
            int points = config.getInt(path + ".points", 1000);
            long created = config.getLong(path + ".created", 0L);
            boolean pvp = config.getBoolean(path + ".pvp", false); // LOAD PVP

            String leaderStr = config.getString(path + ".leader");
            UUID leaderUUID = (leaderStr != null) ? UUID.fromString(leaderStr) : UUID.randomUUID();

            Gildie guild = new Gildie(tag, name, leaderUUID, wName, x, z, points, created, pvp);

            for (String s : config.getStringList(path + ".members")) {
                guild.addMember(UUID.fromString(s));
            }
            for (String s : config.getStringList(path + ".coleaders")) {
                guild.addCoLeader(UUID.fromString(s));
            }
            guilds.put(tag.toLowerCase(), guild);
        }
    }
}