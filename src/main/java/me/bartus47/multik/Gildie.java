package me.bartus47.multik;

import org.bukkit.Location;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Gildie {
    private String tag;
    private String name;
    private UUID leader;
    private List<UUID> members;
    private List<UUID> coLeaders;

    private String worldName;
    private int centerX;
    private int centerZ;
    private static final int RADIUS = 25;

    private int points;
    private long creationTime;
    private boolean pvpEnabled; // NEW: PvP Toggle status

    // Create New
    public Gildie(String tag, String name, UUID leader, Location center) {
        this.tag = tag;
        this.name = name;
        this.leader = leader;
        this.members = new ArrayList<>();
        this.coLeaders = new ArrayList<>();
        this.worldName = center.getWorld().getName();
        this.centerX = center.getBlockX();
        this.centerZ = center.getBlockZ();
        this.points = 1000;
        this.creationTime = System.currentTimeMillis();
        this.pvpEnabled = false; // Default: No Friendly Fire
    }

    // Load Existing
    public Gildie(String tag, String name, UUID leader, String worldName, int x, int z, int points, long creationTime, boolean pvpEnabled) {
        this.tag = tag;
        this.name = name;
        this.leader = leader;
        this.members = new ArrayList<>();
        this.coLeaders = new ArrayList<>();
        this.worldName = worldName;
        this.centerX = x;
        this.centerZ = z;
        this.points = points;
        this.creationTime = creationTime;
        this.pvpEnabled = pvpEnabled;
    }

    public String getTag() { return tag; }
    public String getName() { return name; }

    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }

    public List<UUID> getMembers() { return members; }
    public List<UUID> getCoLeaders() { return coLeaders; }

    public void addCoLeader(UUID uuid) {
        if (!coLeaders.contains(uuid)) coLeaders.add(uuid);
    }
    public void removeCoLeader(UUID uuid) {
        coLeaders.remove(uuid);
    }
    public boolean isCoLeader(UUID uuid) {
        return coLeaders.contains(uuid);
    }

    public String getWorldName() { return worldName; }
    public int getCenterX() { return centerX; }
    public int getCenterZ() { return centerZ; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public void addPoints(int amount) { this.points += amount; }
    public void removePoints(int amount) { this.points -= amount; }

    public long getCreationTime() { return creationTime; }
    public void setCreationTime(long creationTime) { this.creationTime = creationTime; }

    public boolean isPvpEnabled() { return pvpEnabled; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }

    public void addMember(UUID playerUUID) {
        if (!members.contains(playerUUID)) members.add(playerUUID);
    }

    public void removeMember(UUID playerUUID) {
        members.remove(playerUUID);
        coLeaders.remove(playerUUID);
    }

    public boolean isInside(Location loc) {
        if (!loc.getWorld().getName().equals(this.worldName)) return false;
        int minX = centerX - RADIUS;
        int maxX = centerX + RADIUS;
        int minZ = centerZ - RADIUS;
        int maxZ = centerZ + RADIUS;
        return loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
    }
}