package me.bartus47.multik;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class Multik extends JavaPlugin implements Listener {

    private GuildManager guildManager;
    private MarketManager marketManager;
    private PlayerManager playerManager;
    private GuildInventoryManager guildInventoryManager;
    private ChestConfigManager chestConfigManager;
    private LootManager lootManager;
    private NameTagManager nameTagManager;
    private TabListManager tabListManager;
    private KothConfigManager kothConfig;
    private KothManager kothManager;

    // NEW BUFF SYSTEMS
    private BuffConfigManager buffConfigManager;
    private BuffManager buffManager;

    @Override
    public void onEnable() {
        // --- 1. CORE DATA & PLAYER STATS ---
        this.guildManager = new GuildManager(this);
        this.playerManager = new PlayerManager(this);
        this.guildInventoryManager = new GuildInventoryManager(this);

        // --- 2. CONFIGS & MANAGERS ---
        this.kothConfig = new KothConfigManager(this);
        this.kothManager = new KothManager(this, kothConfig);

        this.buffConfigManager = new BuffConfigManager(this);
        this.buffManager = new BuffManager(this, buffConfigManager); // Starts buff task

        // --- 3. GILDIE ---
        TeamCommand teamCmd = new TeamCommand(this, guildManager);
        getCommand("team").setExecutor(teamCmd);
        getCommand("team").setTabCompleter(teamCmd);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new GuildListener(guildManager, this), this);
        getServer().getPluginManager().registerEvents(new PointsListener(guildManager), this);
        getServer().getPluginManager().registerEvents(new GuildInventoryListener(), this);

        // Register Buff Listener (Damage events etc.)
        getServer().getPluginManager().registerEvents(buffManager, this);

        // --- 4. RYNEK ---
        this.marketManager = new MarketManager(this);
        MarketCommand marketCmd = new MarketCommand(marketManager, guildManager);
        getCommand("rynek").setExecutor(marketCmd);
        getCommand("rynek").setTabCompleter(marketCmd);

        getServer().getPluginManager().registerEvents(new MarketListener(marketManager, guildManager), this);

        // --- 5. LOOT CHESTS ---
        this.chestConfigManager = new ChestConfigManager(this);
        this.lootManager = new LootManager(this, chestConfigManager);

        ChestCommand chestCmd = new ChestCommand(this, chestConfigManager);
        getCommand("chests").setExecutor(chestCmd);
        getCommand("chests").setTabCompleter(chestCmd);

        getServer().getPluginManager().registerEvents(new LootInteractionListener(this), this);

        // --- 6. KOTH COMMAND ---
        getCommand("koth").setExecutor(new KothCommand(kothManager));

        // --- 7. VISUALS (NAMETAGS & TAB) ---
        this.nameTagManager = new NameTagManager(this, guildManager);
        nameTagManager.startTask();
        getServer().getPluginManager().registerEvents(nameTagManager, this);

        this.tabListManager = new TabListManager(this, guildManager, playerManager);
        tabListManager.startTask();

        getLogger().info("Multik (Full Suite + Buffs + PvP) enabled!");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerManager.registerLogin(event.getPlayer().getUniqueId());
        if (kothManager != null && kothManager.getBossBar() != null) {
            kothManager.getBossBar().addPlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerManager.registerLogout(event.getPlayer().getUniqueId());
    }

    @Override
    public void onDisable() {
        if (guildManager != null) guildManager.saveGuilds();
        if (marketManager != null) marketManager.saveMarketData();
        if (guildInventoryManager != null) guildInventoryManager.saveInventories();
        if (kothManager != null) kothManager.stopEvent();
    }

    public GuildManager getGuildManager() { return guildManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public GuildInventoryManager getGuildInventoryManager() { return guildInventoryManager; }
    public ChestConfigManager getChestConfigManager() { return chestConfigManager; }
    public LootManager getLootManager() { return lootManager; }
}