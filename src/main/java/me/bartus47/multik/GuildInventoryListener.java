package me.bartus47.multik;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;

public class GuildInventoryListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        if (title.startsWith("Guild Rewards:")) {
            Inventory clickedInventory = event.getClickedInventory();
            if (clickedInventory == null) return;

            // 1. Block Shift-Clicking from player inventory into the rewards chest
            if (clickedInventory.equals(event.getView().getBottomInventory())) {
                if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.setCancelled(true);
                }
            }

            // 2. Block all placement actions directly into the rewards chest slots
            if (clickedInventory.equals(event.getView().getTopInventory())) {
                switch (event.getAction()) {
                    case PLACE_ALL:
                    case PLACE_ONE:
                    case PLACE_SOME:
                    case SWAP_WITH_CURSOR:
                    case HOTBAR_SWAP:
                    case HOTBAR_MOVE_AND_READD:
                    case COLLECT_TO_CURSOR:
                        event.setCancelled(true);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();

        // 3. Block dragging (painting) items into the rewards chest slots
        if (title.startsWith("Guild Rewards:")) {
            int size = event.getView().getTopInventory().getSize();
            for (int slot : event.getRawSlots()) {
                if (slot < size) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }
}