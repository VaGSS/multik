package me.bartus47.multik;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class BuffManager implements Listener {
    private final Multik plugin;
    private final BuffConfigManager configManager;
    private final Random random = new Random();

    private final List<BuffTier> cachedTiers = new ArrayList<>();
    private final Map<UUID, Long> regenCooldowns = new HashMap<>();
    private final Map<UUID, Integer> activeRegenTasks = new HashMap<>();

    public BuffManager(Multik plugin, BuffConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        reloadBuffs();
        startUpdateTask();
    }

    public void reloadBuffs() {
        cachedTiers.clear();
        FileConfiguration cfg = configManager.getConfig();
        ConfigurationSection buffsSection = cfg.getConfigurationSection("buffs");

        if (buffsSection != null) {
            for (String key : buffsSection.getKeys(false)) {
                try {
                    int points = Integer.parseInt(key);
                    ConfigurationSection section = buffsSection.getConfigurationSection(key);
                    if (section != null) {
                        cachedTiers.add(new BuffTier(points, section));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        cachedTiers.sort(Comparator.comparingInt(t -> t.requiredPoints));
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                FileConfiguration cfg = configManager.getConfig();

                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (activeRegenTasks.containsKey(p.getUniqueId())) {
                        handleActiveRegen(p);
                    }

                    Gildie g = plugin.getGuildManager().getGuildByPlayer(p.getUniqueId());
                    if (g == null) {
                        resetAttributes(p);
                        continue;
                    }

                    int points = g.getPoints();
                    PlayerStats stats = calculateStats(points);

                    // Apply Hearts
                    double baseHealth = 20.0;
                    if (points >= 800 && points <= 900) baseHealth -= (cfg.getInt("debuffs.heart_loss_1.hearts_removed") * 2);
                    else if (points < 800) baseHealth -= (cfg.getInt("debuffs.heart_loss_2.hearts_removed") * 2);

                    baseHealth += (stats.extraHearts * 2);

                    AttributeInstance healthAttr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (healthAttr != null && healthAttr.getBaseValue() != baseHealth) {
                        healthAttr.setBaseValue(baseHealth);
                    }

                    // Apply Effects
                    if (points < 800 && cfg.getBoolean("debuffs.heart_loss_2.glowing"))
                        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false));

                    for (PotionEffect effect : stats.effects) {
                        p.addPotionEffect(effect);
                    }

                    checkRegenAbility(p, points, currentTime, cfg);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private PlayerStats calculateStats(int points) {
        PlayerStats stats = new PlayerStats();
        for (BuffTier tier : cachedTiers) {
            if (points >= tier.requiredPoints) {
                stats.extraHearts += tier.extraHearts;
                stats.damageBonus += tier.damageBonus;
                stats.critChance = Math.max(stats.critChance, tier.critChance);
                stats.critMultiplier = Math.max(stats.critMultiplier, tier.critMultiplier);
                stats.shieldBreakChance = Math.max(stats.shieldBreakChance, tier.shieldBreakChance);
                stats.slownessChance = Math.max(stats.slownessChance, tier.slownessChance);
                stats.slownessDuration = Math.max(stats.slownessDuration, tier.slownessDuration);
                stats.witherChance = Math.max(stats.witherChance, tier.witherChance);
                stats.witherDuration = Math.max(stats.witherDuration, tier.witherDuration);
                stats.reachBonus = Math.max(stats.reachBonus, tier.reachBonus);
                stats.killHeal = Math.max(stats.killHeal, tier.killHeal);
                stats.effects.addAll(tier.effects);
            }
        }
        return stats;
    }

    private void handleActiveRegen(Player p) {
        int secondsLeft = activeRegenTasks.get(p.getUniqueId());
        if (secondsLeft > 0) {
            double max = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double newHealth = Math.min(max, p.getHealth() + 2.0);
            p.setHealth(newHealth);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
            activeRegenTasks.put(p.getUniqueId(), secondsLeft - 1);
        } else {
            activeRegenTasks.remove(p.getUniqueId());
            p.sendMessage(ChatColor.GREEN + "Adrenaline rush ended.");
        }
    }

    private void checkRegenAbility(Player p, int points, long currentTime, FileConfiguration cfg) {
        String path = "buffs.2000.regen_ability";
        if (points >= 2000 && cfg.getBoolean(path + ".enabled")) {
            double triggerHealth = cfg.getInt(path + ".trigger_hearts") * 2.0;
            if (p.getHealth() < triggerHealth && !activeRegenTasks.containsKey(p.getUniqueId())) {
                long lastTime = regenCooldowns.getOrDefault(p.getUniqueId(), 0L);
                long cooldownMillis = cfg.getInt(path + ".cooldown") * 1000L;

                if (currentTime - lastTime >= cooldownMillis) {
                    int duration = cfg.getInt(path + ".duration");
                    activeRegenTasks.put(p.getUniqueId(), duration);
                    regenCooldowns.put(p.getUniqueId(), currentTime);
                    p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Adrenaline! " + ChatColor.YELLOW + "Rapid healing activated!");
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 2.0f);
                }
            }
        }
    }

    private void resetAttributes(Player p) {
        AttributeInstance hp = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (hp != null) hp.setBaseValue(hp.getDefaultValue());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR || event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Gildie g = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (g == null) return;

        PlayerStats stats = calculateStats(g.getPoints());
        if (stats.reachBonus <= 0) return;

        double baseReach = 3.0;
        double totalReach = baseReach + stats.reachBonus;

        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();

        RayTraceResult result = player.getWorld().rayTraceEntities(
                eyeLoc, direction, totalReach, 0.5,
                entity -> entity instanceof LivingEntity && !entity.equals(player)
        );

        if (result != null && result.getHitEntity() != null) {
            LivingEntity target = (LivingEntity) result.getHitEntity();
            if (eyeLoc.distance(target.getEyeLocation()) > baseReach) {
                player.attack(target);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player victim)) return;

        Gildie attackerGuild = plugin.getGuildManager().getGuildByPlayer(attacker.getUniqueId());
        Gildie victimGuild = plugin.getGuildManager().getGuildByPlayer(victim.getUniqueId());

        if (attackerGuild != null && victimGuild != null && attackerGuild.getTag().equals(victimGuild.getTag())) {
            if (!attackerGuild.isPvpEnabled()) {
                event.setCancelled(true);
                attacker.sendMessage(ChatColor.RED + "Friendly fire is disabled!");
                return;
            }
        }

        if (attackerGuild == null) return;
        PlayerStats stats = calculateStats(attackerGuild.getPoints());

        double multiplier = 1.0 + stats.damageBonus;

        if (stats.critChance > 0 && random.nextDouble() < stats.critChance) {
            multiplier += (stats.critMultiplier - 1.0);
            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
        }

        if (stats.shieldBreakChance > 0 && victim.isBlocking()) {
            if (random.nextDouble() < stats.shieldBreakChance) {
                disableShield(victim);
                attacker.sendMessage(ChatColor.GOLD + "Shield pierced!");
                victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_SHIELD_BREAK, 1f, 1f);
            }
        }

        event.setDamage(event.getDamage() * multiplier);

        if (stats.slownessChance > 0 && random.nextDouble() < stats.slownessChance) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, stats.slownessDuration * 20, 0));
        }
        if (stats.witherChance > 0 && random.nextDouble() < stats.witherChance) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, stats.witherDuration * 20, 0));
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        Gildie g = plugin.getGuildManager().getGuildByPlayer(killer.getUniqueId());
        if (g == null) return;

        PlayerStats stats = calculateStats(g.getPoints());

        if (stats.killHeal > 0) {
            double max = killer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double newHealth = Math.min(max, killer.getHealth() + stats.killHeal);
            killer.setHealth(newHealth);
            killer.sendMessage(ChatColor.GREEN + "Healed " + (stats.killHeal/2) + " hearts!");
        }
    }

    private void disableShield(Player player) {
        if (player.getInventory().getItemInMainHand().getType() == Material.SHIELD) {
            player.setCooldown(Material.SHIELD, 100);
        } else if (player.getInventory().getItemInOffHand().getType() == Material.SHIELD) {
            player.setCooldown(Material.SHIELD, 100);
        }
    }

    private static class BuffTier {
        int requiredPoints;
        int extraHearts = 0;
        double damageBonus = 0;
        double critChance = 0;
        double critMultiplier = 0;
        double shieldBreakChance = 0;
        double slownessChance = 0;
        int slownessDuration = 3; // Default 3s
        double witherChance = 0;
        int witherDuration = 3; // Default 3s
        double reachBonus = 0;
        double killHeal = 0;
        List<PotionEffect> effects = new ArrayList<>();

        public BuffTier(int points, ConfigurationSection sec) {
            this.requiredPoints = points;
            if (sec.contains("extra_hearts")) this.extraHearts = sec.getInt("extra_hearts");
            if (sec.contains("damage_bonus")) this.damageBonus = sec.getDouble("damage_bonus");
            if (sec.contains("crit_chance")) this.critChance = sec.getDouble("crit_chance");
            if (sec.contains("crit_multiplier")) this.critMultiplier = sec.getDouble("crit_multiplier");
            if (sec.contains("shield_break_chance")) this.shieldBreakChance = sec.getDouble("shield_break_chance");

            if (sec.contains("slowness_chance")) this.slownessChance = sec.getDouble("slowness_chance");
            if (sec.contains("slowness_duration")) this.slownessDuration = sec.getInt("slowness_duration");

            if (sec.contains("wither_chance")) this.witherChance = sec.getDouble("wither_chance");
            if (sec.contains("wither_duration")) this.witherDuration = sec.getInt("wither_duration");

            if (sec.contains("reach_bonus")) this.reachBonus = sec.getDouble("reach_bonus");
            if (sec.contains("kill_heal")) this.killHeal = sec.getDouble("kill_heal");

            if (sec.contains("effects")) {
                for (String s : sec.getStringList("effects")) {
                    try {
                        String[] split = s.split(":");
                        PotionEffectType type = PotionEffectType.getByName(split[0]);
                        int amplifier = Integer.parseInt(split[1]);
                        if (type != null) {
                            effects.add(new PotionEffect(type, 40, amplifier, false, false));
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private static class PlayerStats {
        int extraHearts = 0;
        double damageBonus = 0;
        double critChance = 0;
        double critMultiplier = 1.0;
        double shieldBreakChance = 0;
        double slownessChance = 0;
        int slownessDuration = 0;
        double witherChance = 0;
        int witherDuration = 0;
        double reachBonus = 0;
        double killHeal = 0;
        List<PotionEffect> effects = new ArrayList<>();
    }
}