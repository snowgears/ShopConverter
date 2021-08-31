package com.snowgears.shopconverter;


import com.Acrobot.Breeze.Utils.MaterialUtil;
import com.Acrobot.Breeze.Utils.PriceUtil;
import com.Acrobot.ChestShop.ChestShop;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.SellShop;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

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

                                    BlockFace signDirection = formChestAndSign(sign);
                                    if(signDirection != null) {
                                        SellShop updatedShop = new SellShop(blockState.getLocation(), uuid, price, amount, isAdmin, signDirection);
                                        Shop.getPlugin().getShopHandler().addShop(updatedShop);
                                        updatedShop.setItemStack(item);
                                        updatedShop.updateSign();

                                        shop.getShopHandler().addShop(updatedShop);
                                    }
                                }
                            } catch (Exception e){ } //do nothing
                        }
                    }
                }
            }
        }
        shop.getShopHandler().saveAllShops();
    }

    //creates the chest and sign for Shop and returns the direction of the sign
    private BlockFace formChestAndSign(Sign sign) {
        BlockFace signDirection = null;
        //if instanceof rotatable (regular sign post), get blockface from closest rotation
        if(sign.getBlockData() instanceof Rotatable){
            signDirection = ((Rotatable) sign.getBlockData()).getRotation();
            //adjust the sign direction to coordinal direction if its not already one
            if( signDirection.toString().indexOf('_') != -1) {
                String adjustedDirString = signDirection.toString().substring(0, signDirection.toString().indexOf('_'));
                signDirection = BlockFace.valueOf(adjustedDirString);
            }
        }
        //if instanceof wallsign, get blockface from facing
        else if (sign.getBlockData() instanceof WallSign) {
            WallSign wallSign = (WallSign) sign.getBlockData();
            signDirection = wallSign.getFacing();
        }

        if(signDirection == null)
            return null;

        Block toChest = sign.getBlock().getRelative(signDirection.getOppositeFace());

        //if theres no chest on the other side of the sign, make one
        if (!(toChest.getState() instanceof Chest)) {
            toChest.setType(Material.CHEST);
        }

        Chest chest = (Chest) toChest.getState();
        ItemStack[] contents = chest.getInventory().getContents().clone();

        //this should always be true because chests are directional, but check just in case
        if(toChest.getBlockData() instanceof Directional){
            Directional directional = (Directional) toChest.getBlockData();
            directional.setFacing(signDirection);
            toChest.setBlockData(directional);
            chest.getInventory().setContents(contents);
            toChest.getRelative(BlockFace.UP).setType(Material.AIR);
        }

        //make the sign a wall sign, and set the direction to match what it used to be
        sign.getBlock().setType(Material.OAK_WALL_SIGN);

        //also should always be true, since wall signs are directional, but check anyway
        if(sign.getBlockData() instanceof WallSign){
            WallSign wallSign = (WallSign) sign.getBlockData();
            wallSign.setFacing(signDirection);
            sign.setBlockData(wallSign);
        }
        sign.update();

        return signDirection;
    }
}