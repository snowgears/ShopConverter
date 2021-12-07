package com.snowgears.shopconverter.converter;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.BuyShop;
import com.snowgears.shop.shop.SellShop;
import com.snowgears.shop.util.EconomyUtils;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.UtilMethods;
import com.snowgears.shopconverter.ShopConverter;
import net.ess3.api.IEssentials;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.UUID;

public class EssentialsShopConverter implements Converter {

    private IEssentials essentials;

    public EssentialsShopConverter(){
        essentials = (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials");
    }

    @Override
    public int convertChunk(Chunk chunk){

        int numConverted = 0;
        HashSet<UUID> convertedShopsByUUID = new HashSet<>();

        for (BlockState blockState : chunk.getTileEntities()) {
            if (blockState instanceof Sign) {
                Sign sign = (Sign) blockState;

                try {
                    AbstractShop updatedShop = null;
                    int stock = 0;
                    double moneyInStock = 0;
                    ItemStack item = null;

                    String type = sign.getLine(0);

                    //'trade' shops are just non-admin player shops. they can be buy or sell
                    if(type.contains("[Trade]")){
                        int amount = 0;
                        double price = 0;
                        UUID ownerUUID = Bukkit.getOfflinePlayer(ChatColor.stripColor(sign.getLine(3))).getUniqueId();

                        //price info is on line 1
                        if(sign.getLine(1).contains("$")) {
                            int colonIndex1 = sign.getLine(1).indexOf(":");
                            if(colonIndex1 != -1) {
                                price = Double.parseDouble(sign.getLine(1).substring(sign.getLine(1).indexOf("$")+1, colonIndex1));
                                moneyInStock = Double.parseDouble(sign.getLine(1).substring(colonIndex1+1));

                                int spaceIndex = sign.getLine(2).indexOf(" ");
                                if(spaceIndex != -1) {
                                    amount = Integer.parseInt(sign.getLine(2).substring(0, spaceIndex));

                                    int colonIndex2 = sign.getLine(2).indexOf(":");
                                    if (colonIndex2 != -1) {
                                        item = essentials.getItemDb().get(sign.getLine(2).substring(spaceIndex + 1, colonIndex2));
                                        stock = Integer.parseInt(sign.getLine(2).substring(colonIndex2+1));


                                        BlockFace facing = ShopConverter.formBlocksFromSign(sign);

                                        updatedShop = new SellShop(sign.getLocation(), ownerUUID, price, amount, false, facing);
                                    }
                                }
                            }
                        }
                        //price info is on line 2
                        else if(sign.getLine(2).contains("$")) {
                            int spaceIndex = sign.getLine(1).indexOf(" ");

                            if(spaceIndex != -1) {
                                amount = Integer.parseInt(sign.getLine(1).substring(0, spaceIndex));

                                int colonIndex1 = sign.getLine(1).indexOf(":");
                                if(colonIndex1 != -1) {

                                    item = essentials.getItemDb().get(sign.getLine(1).substring(spaceIndex + 1, colonIndex1));
                                    stock = Integer.parseInt(sign.getLine(1).substring(colonIndex1+1));

                                    int colonIndex2 = sign.getLine(2).indexOf(":");
                                    if (colonIndex2 != -1) {
                                        price = Double.parseDouble(sign.getLine(2).substring(sign.getLine(2).indexOf("$")+1, colonIndex2));
                                        moneyInStock = Double.parseDouble(sign.getLine(2).substring(colonIndex2+1));

                                        BlockFace facing = ShopConverter.formBlocksFromSign(sign);

                                        updatedShop = new BuyShop(sign.getLocation(), ownerUUID, price, amount, false, facing);
                                    }
                                }
                            }
                        }
                    }
                    //'buy' shops are admin shops that buy items
                    else if(type.contains("[Buy]")){
                        int amount = Integer.parseInt(UtilMethods.cleanNumberText(sign.getLine(1)));
                        if(!sign.getLine(2).equals("exp")) {
                            item = essentials.getItemDb().get(sign.getLine(2));
                            double price = Double.parseDouble(sign.getLine(3).substring(sign.getLine(3).indexOf("$")+1));
                            UUID ownerUUID = Bukkit.getOfflinePlayer(ChatColor.stripColor(sign.getLine(3))).getUniqueId();

                            BlockFace facing = ShopConverter.formBlocksFromSign(sign);
                            updatedShop = new SellShop(sign.getLocation(), ownerUUID, price, amount, true, facing);
                        }
                    }
                    //'sell' shops are admin shops that sell items
                    else if(type.contains("[Sell]")){
                        int amount = Integer.parseInt(UtilMethods.cleanNumberText(sign.getLine(1)));
                        item = essentials.getItemDb().get(sign.getLine(2));
                        double price = Double.parseDouble(sign.getLine(3).substring(sign.getLine(3).indexOf("$")+1));
                        UUID ownerUUID = Bukkit.getOfflinePlayer(ChatColor.stripColor(sign.getLine(3))).getUniqueId();

                        BlockFace facing = ShopConverter.formBlocksFromSign(sign);
                        updatedShop = new BuyShop(sign.getLocation(), ownerUUID, price, amount, true, facing);
                    }

                    if(updatedShop != null && item != null) {

                        updatedShop.setItemStack(item);
                        Shop.getPlugin().getShopHandler().addShop(updatedShop);
                        convertedShopsByUUID.add(updatedShop.getOwnerUUID());
                        numConverted++;

                        //be sure to add back money in extra stock to the chest and extra money to the owner
                        ItemStack stockItems = item.clone();
                        stockItems.setAmount(stock);
                        InventoryUtils.addItem(updatedShop.getInventory(), stockItems, updatedShop.getOwner());
                        EconomyUtils.addFunds(updatedShop.getOwner(), updatedShop.getInventory(), moneyInStock);

                        final AbstractShop fUpdatedShop = updatedShop;
                        ShopConverter.plugin.getServer().getScheduler().scheduleSyncDelayedTask(ShopConverter.plugin, new Runnable() {
                            public void run() {
                                fUpdatedShop.updateSign();
                                fUpdatedShop.load();
                            }
                        }, 1L);

                    }


                } catch (Exception e){
                    System.out.println("[ShopConverter] ERROR: Essentials shop at "+ShopConverter.getCleanLocation(blockState.getLocation(), true)+" could not be converted!");
                    e.printStackTrace();
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
        //EssentialsX has no easy way to find all EssentialsShops in the world / on the server
        //for EssentialsX, we will convert shops in loaded chunks as they come up
        return 0;
    }

    @Override
    public String getShopPluginName(){
        return "EssentialsX";
    }

    @Override
    public boolean runAtChunkLoad(){
        return true;
    }
}
