package me.bartus47.multik;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class BuffConfigManager {
    private final Multik plugin;
    private File file;
    private FileConfiguration config;

    public BuffConfigManager(Multik plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "buffs.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
                config = YamlConfiguration.loadConfiguration(file);

                // --- HEADER DOCUMENTATION ---
                config.options().header(
                        "============================================================\n" +
                                "                  MULTIK BUFFS CONFIGURATION\n" +
                                "============================================================\n" +
                                "AVAILABLE BUFF TYPES:\n" +
                                " - extra_hearts: (Integer) Adds permanent extra hearts (1 heart = 2 HP).\n" +
                                " - damage_bonus: (Double) Percent damage increase (e.g., 0.1 = +10%).\n" +
                                " - crit_chance: (Double 0.0-1.0) Chance to deal 150% damage.\n" +
                                " - crit_multiplier: (Double) Damage multiplier on crit (default 1.5).\n" +
                                " - shield_break_chance: (Double 0.0-1.0) Chance to disable enemy shield.\n" +
                                " - shield_break_multiplier: (Double) Damage multiplier if shield breaks.\n" +
                                " - slowness_chance: (Double 0.0-1.0) Chance to slow enemy on hit.\n" +
                                " - slowness_duration: (Integer) Duration of slowness in seconds.\n" +
                                " - wither_chance: (Double 0.0-1.0) Chance to wither enemy on hit.\n" +
                                " - wither_duration: (Integer) Duration of wither in seconds.\n" +
                                " - reach_bonus: (Double) Extra reach distance in blocks (Raytrace).\n" +
                                " - kill_heal: (Double) Hearts healed on kill (e.g. 5.0 = 2.5 hearts).\n" +
                                " - effects: (List) Permanent potion effects. Format: 'EFFECT_NAME:AMPLIFIER'\n" +
                                "   (Example: 'SPEED:0' is Speed I, 'SPEED:1' is Speed II)\n" +
                                "============================================================\n" +
                                "SPECIAL ABILITIES:\n" +
                                " - regen_ability: Configures the 'Adrenaline' healing mechanic.\n" +
                                "============================================================\n"
                );
                config.options().copyHeader(true);

                // --- DEBUFFS ---
                config.set("debuffs.heart_loss_1.threshold_min", 800);
                config.set("debuffs.heart_loss_1.threshold_max", 900);
                config.set("debuffs.heart_loss_1.hearts_removed", 1);

                config.set("debuffs.heart_loss_2.threshold_max", 799);
                config.set("debuffs.heart_loss_2.hearts_removed", 2);
                config.set("debuffs.heart_loss_2.glowing", true);

                // --- BUFF TIERS ---
                createTier("1050", "extra_hearts", 1);

                createTier("1100", "effects", List.of("FIRE_RESISTANCE:0"));

                createTier("1150", "extra_hearts", 1);

                createTier("1200", "crit_chance", 0.10, "crit_multiplier", 1.5);

                createTier("1250", "extra_hearts", 1);

                createTier("1300", "damage_bonus", 0.10);

                createTier("1350", "extra_hearts", 1);

                createTier("1400", "crit_chance", 0.20, "crit_multiplier", 1.5);

                // Added duration here
                createTier("1450", "slowness_chance", 0.10, "slowness_duration", 5);

                createTier("1500", "extra_hearts", 1);

                createTier("1550", "effects", List.of("SPEED:0"));

                createTier("1600", "shield_break_chance", 0.20, "shield_break_multiplier", 1.5);

                createTier("1650", "crit_chance", 0.30);

                createTier("1700", "damage_bonus", 0.20);

                createTier("1750", "effects", List.of("DAMAGE_RESISTANCE:0"));

                // Added duration here
                createTier("1800", "wither_chance", 0.10, "wither_duration", 5);

                createTier("1850", "reach_bonus", 1.0);

                createTier("1900", "kill_heal", 10.0);

                createTier("1950", "damage_bonus", 0.30);

                // Special Ability Config
                config.set("buffs.2000.regen_ability.enabled", true);
                config.set("buffs.2000.regen_ability.trigger_hearts", 3);
                config.set("buffs.2000.regen_ability.total_heal", 10);
                config.set("buffs.2000.regen_ability.duration", 10);
                config.set("buffs.2000.regen_ability.cooldown", 900);

                config.save(file);
            } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    private void createTier(String points, String key, Object value) {
        config.set("buffs." + points + "." + key, value);
    }

    private void createTier(String points, String key1, Object val1, String key2, Object val2) {
        config.set("buffs." + points + "." + key1, val1);
        config.set("buffs." + points + "." + key2, val2);
    }

    public FileConfiguration getConfig() { return config; }
}