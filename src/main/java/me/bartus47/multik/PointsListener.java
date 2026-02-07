package me.bartus47.multik;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PointsListener implements Listener {

    private final GuildManager guildManager;

    public PointsListener(GuildManager guildManager) {
        this.guildManager = guildManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) return;
        if (victim.equals(killer)) return;

        Gildie victimGuild = guildManager.getGuildByPlayer(victim.getUniqueId());
        Gildie killerGuild = guildManager.getGuildByPlayer(killer.getUniqueId());

        // Logika odejmowania punktów ofierze
        if (victimGuild != null && killerGuild != null) {

            // Sprawdź czy to nie ta sama gildia (zapobieganie farmieniu)
            if (victimGuild.getTag().equals(killerGuild.getTag())) {
                killer.sendMessage(ChatColor.RED + "Nie otrzymujesz punktów za zabicie członka własnej gildii!");
                return;
            }

            // OBLICZANIE PUNKTÓW (Skalowanie)
            int killerPoints = killerGuild.getPoints();
            int victimPoints = victimGuild.getPoints();
            int pointsChange = 20; // Baza

            // Oblicz różnicę (Killer - Victim)
            int diff = killerPoints - victimPoints;

            // Różnica 1000 to zmiana o 10 punktów. Czyli 100 pkt różnicy = 1 pkt zmiany.
            // diff / 100 da nam liczbę punktów do odjęcia/dodania od bazy.

            int adjustment = diff / 100;

            // Jeśli Killer jest silniejszy (diff > 0), adjustment jest dodatni, więc odejmujemy od nagrody (dostanie mniej).
            // Jeśli Killer jest słabszy (diff < 0), adjustment jest ujemny, więc "odejmując minus" dodajemy do nagrody.
            pointsChange = 20 - adjustment;

            // Ograniczenia (Clamp): Minimum 10, Maksimum 30
            if (pointsChange < 10) pointsChange = 10;
            if (pointsChange > 30) pointsChange = 30;

            // Aplikowanie zmian
            victimGuild.removePoints(pointsChange);
            killerGuild.addPoints(pointsChange);
            guildManager.saveGuilds();

            // Komunikaty
            victim.sendMessage(ChatColor.RED + "Twoja gildia straciła " + pointsChange + " punktów przez twoją śmierć!");
            killer.sendMessage(ChatColor.GREEN + "Twoja gildia zdobyła " + pointsChange + " punktów za zabójstwo!");

        } else if (victimGuild != null) {
            // Jeśli ofiara ma gildię, ale zabójca nie (traci standardowo 10, lub wg uznania)
            victimGuild.removePoints(10);
            guildManager.saveGuilds();
            victim.sendMessage(ChatColor.RED + "Twoja gildia straciła 10 punktów.");
        }
    }
}