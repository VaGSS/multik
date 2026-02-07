package me.bartus47.multik;

import org.bukkit.inventory.ItemStack;
import java.util.UUID;

public class MarketItem {
    private final UUID id;
    private final UUID sellerUUID;
    private final ItemStack itemStack;
    private final int price; // ZMIANA: double -> int

    public MarketItem(UUID sellerUUID, ItemStack itemStack, int price) {
        this.id = UUID.randomUUID();
        this.sellerUUID = sellerUUID;
        this.itemStack = itemStack;
        this.price = price;
    }

    public MarketItem(UUID id, UUID sellerUUID, ItemStack itemStack, int price) {
        this.id = id;
        this.sellerUUID = sellerUUID;
        this.itemStack = itemStack;
        this.price = price;
    }

    public UUID getId() { return id; }
    public UUID getSellerUUID() { return sellerUUID; }
    public ItemStack getItemStack() { return itemStack; }
    public int getPrice() { return price; } // ZMIANA: double -> int
}