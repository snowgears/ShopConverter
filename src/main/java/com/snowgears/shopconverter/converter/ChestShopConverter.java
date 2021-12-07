package com.snowgears.shopconverter.converter;

import com.Acrobot.Breeze.Utils.MaterialUtil;
import com.Acrobot.Breeze.Utils.PriceUtil;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.SellShop;
import com.snowgears.shopconverter.ShopConverter;
import org.bukkit.Chunk;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.UUID;

public class ChestShopConverter implements Converter {

    @Override
    public int convertChunk(Chunk chunk){

        int numConverted = 0;
        HashSet<UUID> convertedShopsByUUID = new HashSet<>();

        for (BlockState blockState : chunk.getTileEntities()) {
            if (blockState instanceof Sign) {
                Sign sign = (Sign) blockState;
                if(ChestShopSign.isValid(sign)){
                    try {
                        //all of this code is taken from the ChestShop plugin source
                        String name = sign.getLine(0);
                        String quantity = sign.getLine(1);
                        String prices = sign.getLine(2);
                        String material = sign.getLine(3);

                        String ownerName = NameManager.getFullUsername(name);
                        UUID uuid = NameManager.getUUID(ownerName);

                        if (uuid != null) {
                            int amount = Integer.parseInt(quantity);
                            double price = PriceUtil.getBuyPrice(prices); //PriceUtil.getSellPrice(prices)); //maybe make a sell price option later
                            ItemStack item = MaterialUtil.getItem(material);
                            boolean isAdmin = NameManager.isAdminShop(uuid);

                            BlockFace signDirection = ShopConverter.formBlocksFromSign(sign);
                            if(signDirection != null) {
                                SellShop updatedShop = new SellShop(blockState.getLocation(), uuid, price, amount, isAdmin, signDirection);
                                Shop.getPlugin().getShopHandler().addShop(updatedShop);
                                updatedShop.setItemStack(item);
                                updatedShop.updateSign();

                                ShopConverter.shopPlugin.getShopHandler().addShop(updatedShop);
                                convertedShopsByUUID.add(updatedShop.getOwnerUUID());
                                numConverted++;
                            }
                        }
                        System.out.println("[ShopConverter] Converted ChestShop at "+ShopConverter.getCleanLocation(blockState.getLocation(), true));
                    } catch (Exception e){
                        System.out.println("[ShopConverter] ERROR: ChestShop at "+ShopConverter.getCleanLocation(blockState.getLocation(), true)+" could not be converted!");
                    }
                }
            }
        }
        for(UUID ownerUUID : convertedShopsByUUID) {
            ShopConverter.shopPlugin.getShopHandler().saveShops(ownerUUID);
        }
        return numConverted;
    }

    @Override
    public int convertAll(){
        //ChestShop has no easy way to find all ChestShops in the world / on the server
        //for ChestShop, we will convert shops in loaded chunks as they come up
        return 0;
    }

    @Override
    public String getShopPluginName(){
        return "ChestShop";
    }

    @Override
    public boolean runAtChunkLoad(){
        return true;
    }
}
