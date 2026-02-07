package me.bartus47.multik;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.reflect.StructureModifier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class TabListManager {

    private final Multik plugin;
    private final GuildManager guildManager;
    private final PlayerManager playerManager;
    private final ProtocolManager protocolManager;

    private final Map<Integer, WrappedGameProfile> fakeProfiles = new HashMap<>();
    private final UUID BASE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public TabListManager(Multik plugin, GuildManager guildManager, PlayerManager playerManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.playerManager = playerManager;
        this.protocolManager = ProtocolLibrary.getProtocolManager();

        initializeFakeProfiles();
    }

    private void initializeFakeProfiles() {
        for (int i = 0; i < 80; i++) {
            String name = String.format("!%03d", i);
            UUID uuid = new UUID(BASE_UUID.getMostSignificantBits(), BASE_UUID.getLeastSignificantBits() + i);
            fakeProfiles.put(i, new WrappedGameProfile(uuid, name));
        }
    }

    public void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    sendFunnyGuildsTab(player);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // --- METODY POMOCNICZE (REFLEKSJA) ---

    // 1. Znajduje typ pakietu po nazwie (omija brak pola w starym jarze)
    private PacketType getPacketType(String name) {
        try {
            Field field = PacketType.Play.Server.class.getField(name);
            return (PacketType) field.get(null);
        } catch (Exception e) {
            return PacketType.Play.Server.PLAYER_INFO;
        }
    }

    // 2. Ustawia akcje używając DEDYKOWANEJ metody z ProtocolLib (to naprawia crash serwera)
    private void setPlayerInfoActions(PacketContainer packet, EnumSet<EnumWrappers.PlayerInfoAction> actions) {
        try {
            // Szukamy metody getPlayerInfoActions() w PacketContainer
            // Ta metoda zwraca modifier, który AUTOMATYCZNIE konwertuje enumy!
            Method method = PacketContainer.class.getMethod("getPlayerInfoActions");

            // Wywołujemy: StructureModifier modifier = packet.getPlayerInfoActions();
            Object modifierObj = method.invoke(packet);

            if (modifierObj instanceof StructureModifier) {
                // Wywołujemy: modifier.write(0, actions);
                ((StructureModifier<EnumSet<EnumWrappers.PlayerInfoAction>>) modifierObj).write(0, actions);
            }
        } catch (Exception e) {
            // Jeśli coś pójdzie nie tak (np. stara wersja ProtocolLib na serwerze),
            // próbujemy metody awaryjnej (dla starych wersji MC < 1.19.3)
            try {
                if (packet.getType() == PacketType.Play.Server.PLAYER_INFO) {
                    packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void sendFunnyGuildsTab(Player player) {
        player.setPlayerListHeaderFooter(
                ChatColor.AQUA + "" + ChatColor.BOLD + "   KozMc   \n" + ChatColor.DARK_GRAY + "Serwer PvP",
                "\n" + ChatColor.YELLOW + "Strona: " + ChatColor.WHITE + "www.kozmc.pl"
        );

        String[] slots = new String[80];
        Arrays.fill(slots, " ");
        fillTabContent(slots, player);

        try {
            // 1. Tworzymy pakiet UPDATE (dla 1.20)
            PacketType type = getPacketType("PLAYER_INFO_UPDATE");
            PacketContainer packet = protocolManager.createPacket(type);

            // 2. Ustawiamy akcje przez naszą bezpieczną metodę
            EnumSet<EnumWrappers.PlayerInfoAction> actions = EnumSet.of(
                    EnumWrappers.PlayerInfoAction.ADD_PLAYER,
                    EnumWrappers.PlayerInfoAction.UPDATE_LISTED,
                    EnumWrappers.PlayerInfoAction.UPDATE_LATENCY,
                    EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME
            );
            setPlayerInfoActions(packet, actions);

            // 3. Budujemy dane
            List<PlayerInfoData> dataList = new ArrayList<>();

            for (int i = 0; i < 80; i++) {
                WrappedGameProfile profile = fakeProfiles.get(i);
                WrappedChatComponent displayName = WrappedChatComponent.fromText(slots[i]);

                // Ping = -1 (zazwyczaj powoduje brak kresek lub ciemne kreski)
                PlayerInfoData data = new PlayerInfoData(
                        profile,
                        -1,
                        EnumWrappers.NativeGameMode.SURVIVAL,
                        displayName
                );
                dataList.add(data);
            }

            // 4. Wrzucamy listę (index 1 to standard dla UPDATE)
            try {
                packet.getPlayerInfoDataLists().write(1, dataList);
            } catch (Exception e) {
                // Fallback na 0 (dla starszych wersji pakietu)
                packet.getPlayerInfoDataLists().write(0, dataList);
            }

            protocolManager.sendServerPacket(player, packet);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Metoda rozlewająca (dodaje spacje)
    private String pad(String text) {
        return text + "      ";
    }

    private void fillTabContent(String[] slots, Player viewer) {
        // --- KOLUMNA 1: ONLINE ---
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        slots[0] = pad(ChatColor.AQUA + "" + ChatColor.BOLD + "ONLINE");
        for(int i = 0; i < 19; i++) {
            if(i < onlinePlayers.size()) {
                String name = onlinePlayers.get(i).getName();
                if (name.length() > 12) name = name.substring(0, 12);
                slots[1 + i] = pad(ChatColor.GRAY + name);
            } else {
                slots[1 + i] = pad(ChatColor.DARK_GRAY + "-");
            }
        }

        // --- KOLUMNA 2: TOP TEAMS ---
        List<Gildie> topGuilds = new ArrayList<>(guildManager.getGuilds());
        topGuilds.sort((g1, g2) -> Integer.compare(g2.getPoints(), g1.getPoints()));

        slots[20] = pad(ChatColor.GOLD + "" + ChatColor.BOLD + "TOP TEAMS");
        for(int i = 0; i < 19; i++) {
            if(i < topGuilds.size()) {
                Gildie g = topGuilds.get(i);
                slots[21 + i] = pad(ChatColor.YELLOW + String.valueOf(i+1) + ". " + g.getTag() + " " + ChatColor.WHITE + g.getPoints());
            } else {
                slots[21 + i] = pad(ChatColor.DARK_GRAY + "-");
            }
        }

        // --- KOLUMNA 3: TOP KILLS ---
        LinkedHashMap<String, Integer> topKillers = playerManager.getTopKillers(19);
        List<Map.Entry<String, Integer>> killerEntries = new ArrayList<>(topKillers.entrySet());

        slots[40] = pad(ChatColor.RED + "" + ChatColor.BOLD + "TOP KILLS");
        for(int i = 0; i < 19; i++) {
            if(i < killerEntries.size()) {
                Map.Entry<String, Integer> entry = killerEntries.get(i);
                slots[41 + i] = pad(ChatColor.WHITE + entry.getKey() + " " + ChatColor.RED + entry.getValue());
            } else {
                slots[41 + i] = pad(ChatColor.DARK_GRAY + "-");
            }
        }

        // --- KOLUMNA 4: TWOJE STATY ---
        slots[60] = pad(ChatColor.GREEN + "" + ChatColor.BOLD + "TWOJE STATY");
        slots[62] = pad(ChatColor.GRAY + "Nick:");
        slots[63] = pad(ChatColor.WHITE + viewer.getName());
        slots[65] = pad(ChatColor.GRAY + "Zabójstwa:");
        slots[66] = pad(ChatColor.RED + "" + playerManager.getKills(viewer.getUniqueId()));

        // Ping jako tekst - DODANY
        slots[68] = pad(ChatColor.GRAY + "Ping:");
        slots[69] = pad(ChatColor.GREEN + "" + viewer.getPing() + "ms");

        Gildie g = guildManager.getGuildByPlayer(viewer.getUniqueId());
        slots[71] = pad(ChatColor.GRAY + "Gildia:");
        slots[72] = pad((g != null) ? ChatColor.GOLD + g.getTag() : ChatColor.RED + "Brak");
    }
}