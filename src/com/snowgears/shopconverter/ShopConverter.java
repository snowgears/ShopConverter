package com.snowgears.shopconverter;


import com.Acrobot.Breeze.Utils.MaterialUtil;
import com.Acrobot.Breeze.Utils.PriceUtil;
import com.Acrobot.ChestShop.ChestShop;
import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.ShopType;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ShopConverter extends JavaPlugin {

    private static ShopConverter plugin;
    private final ConvertListener convertListener = new ConvertListener(this);

    public static ShopConverter getPlugin() {
        return plugin;
    }

    public void onEnable() {
        plugin = this;
        getServer().getPluginManager().registerEvents(convertListener, this);
    }

    public void onDisable() {
        plugin = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(cmd.getName().equalsIgnoreCase("shopconvert") && args.length == 0){
            convertAllShops();
            sender.sendMessage("All ChestShops in any loaded chunks have been converted to Shops.");
            return true;
        }
        return true;
    }

    private void convertAllShops(){
        Shop shop = (Shop) this.getServer().getPluginManager().getPlugin("Shop");
        ChestShop chestShop = (ChestShop) this.getServer().getPluginManager().getPlugin("ChestShop");

        //TODO figure out how to get all shops from chestshop
        //if this is not possible, you will have to scan every block in the world, checking if they are signs, and parsing

        for(World world : plugin.getServer().getWorlds()) {
            for(Chunk chunk : world.getLoadedChunks()) {
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

                                    formChestAndSign(sign);
                                    ShopObject updatedShop = new ShopObject(blockState.getLocation(), uuid, price, amount, isAdmin, ShopType.SELL);
                                    Shop.getPlugin().getShopHandler().addShop(updatedShop);
                                    updatedShop.setItemStack(item);
                                    updatedShop.updateSign();
                                }
                            } catch (Exception e){ } //do nothing
                        }
                    }
                }
            }
        }
    }

    private void formChestAndSign(Sign sign){
        org.bukkit.material.Sign matSign = (org.bukkit.material.Sign)sign.getData();
        byte chestDirection = 2;
        switch(matSign.getData()){
            case 8: //NORTH
                chestDirection = 2;
                break;
            case 12: //EAST
                chestDirection = 5;
                break;
            case 0: //SOUTH
                chestDirection = 3;
                break;
            case 4: //WEST
                chestDirection = 4;
                break;
            default:
                break;
        }

        Block toChest;
        if (matSign.isWallSign())
            toChest = sign.getBlock().getRelative(matSign.getAttachedFace());
        else
            toChest = sign.getBlock().getRelative(matSign.getFacing().getOppositeFace());

        if(toChest.getState() instanceof Chest){
            Chest chest = (Chest)toChest.getState();
            ItemStack[] contents = chest.getInventory().getContents().clone();
            toChest.setTypeIdAndData(Material.CHEST.getId(), chestDirection, true);
            chest.getInventory().setContents(contents);
        }
        else{
            toChest.setTypeIdAndData(Material.CHEST.getId(), chestDirection, true);
        }
        toChest.getRelative(BlockFace.UP).setType(Material.AIR);

        sign.getBlock().setTypeIdAndData(Material.WALL_SIGN.getId(), chestDirection, true);
        sign.update();
    }
}