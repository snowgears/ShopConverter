package com.snowgears.shopconverter.converter;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.BuyShop;
import com.snowgears.shop.shop.SellShop;
import com.snowgears.shopconverter.ShopConverter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.maxgamer.quickshop.api.QuickShopAPI;

import java.util.UUID;

public class QuickShopConverter implements Converter {

    private QuickShopAPI quickShopAPI;

    public QuickShopConverter(){
        Plugin plugin = Bukkit.getPluginManager().getPlugin("QuickShop");
        if(plugin != null){
            quickShopAPI = (QuickShopAPI)plugin;
        }
    }

    @Override
    public int convertChunk(Chunk chunk){
        return 0;
    }

    @Override
    public int convertAll(){

        int numConverted = 0;

        Block signBlock = null;
        Block chestBlock = null;

        for(org.maxgamer.quickshop.api.shop.Shop shop : quickShopAPI.getShopManager().getAllShops()){
            UUID ownerUUID = shop.getOwner();
            boolean isAdmin = shop.isUnlimited();
            ItemStack is = shop.getItem().clone();
            int amount = is.getAmount();
            is.setAmount(1);
            double price = shop.getPrice();

            chestBlock = shop.getLocation().getBlock();

            shop.delete();

            signBlock = ShopConverter.formBlocksFromChest(chestBlock);

            if(signBlock != null) {
                org.bukkit.block.data.type.Chest chest = (org.bukkit.block.data.type.Chest)chestBlock.getBlockData();

                AbstractShop updatedShop = null;
                if (shop.isBuying()) {
                    updatedShop = new BuyShop(signBlock.getLocation(), ownerUUID, price, amount, isAdmin, chest.getFacing());
                }
                else if (shop.isSelling()) {
                    updatedShop = new SellShop(signBlock.getLocation(), ownerUUID, price, amount, isAdmin, chest.getFacing());
                }
                Shop.getPlugin().getShopHandler().addShop(updatedShop);
                updatedShop.setItemStack(is);
                updatedShop.updateSign();
                updatedShop.load();
                numConverted++;

                System.out.println("[ShopConverter] Converted QuickShop at "+ShopConverter.getCleanLocation(chestBlock.getLocation(), true));
            }
        }
        ShopConverter.shopPlugin.getShopHandler().saveAllShops();

        return numConverted;
    }

    @Override
    public String getShopPluginName(){
        return "QuickShop";
    }

    @Override
    public boolean runAtChunkLoad(){
        return false;
    }
}
