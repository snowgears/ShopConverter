package com.snowgears.shopconverter;


import com.snowgears.shop.Shop;
import com.snowgears.shopconverter.converter.ChestShopConverter;
import com.snowgears.shopconverter.converter.Converter;
import com.snowgears.shopconverter.converter.EssentialsShopConverter;
import com.snowgears.shopconverter.converter.QuickShopConverter;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class ShopConverter extends JavaPlugin {

    public static ShopConverter plugin;
    public Converter externalShopConverter;
    private final ConvertListener convertListener = new ConvertListener(this);

    public static Shop shopPlugin;
    public static ShopConverter getPlugin() {
        return plugin;
    }

    public void onEnable() {
        plugin = this;
        this.shopPlugin = (Shop) this.getServer().getPluginManager().getPlugin("Shop");
        if(this.shopPlugin == null){
            System.out.println("[ShopConverter] Shop needs to be installed for this utility to function! Disabling converter...");
            getServer().getPluginManager().disablePlugin(this);
        }

        if(getServer().getPluginManager().getPlugin("ChestShop") != null) {
            System.out.println("[ShopConverter] Found ChestShop on server. This utility will convert all ChestShop shops to Shop shops!");
            externalShopConverter = new ChestShopConverter();
        }
        else if(getServer().getPluginManager().getPlugin("QuickShop") != null) {
            System.out.println("[ShopConverter] Found QuickShop on server. This utility will convert all QuickShop shops to Shop shops!");
            externalShopConverter = new QuickShopConverter();
        }
        else if(getServer().getPluginManager().getPlugin("Essentials") != null){
            System.out.println("[ShopConverter] Found Essentials on server. This utility will convert all Essentials shops to Shop shops!");
            externalShopConverter = new EssentialsShopConverter();
        }
        else{
            System.out.println("[ShopConverter] No supported shop plugins were found on the server!");
            System.out.println("[ShopConverter] One is required to be installed for this utility to function! Disabling converter...");
            getServer().getPluginManager().disablePlugin(this);
        }

        getServer().getPluginManager().registerEvents(convertListener, this);
    }

    public void onDisable() {
        plugin = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(cmd.getName().equalsIgnoreCase("shopconvert") && args.length == 0){
            if(externalShopConverter.runAtChunkLoad()) {
                if(sender instanceof Player) {
                    Player player = (Player) sender;
                    sender.sendMessage(ChatColor.GRAY+externalShopConverter.getShopPluginName()+" conversions happen at chunk load automatically.");
                    player.sendMessage(ChatColor.GRAY+"Forcing shop conversions in current chunk...");
                    int numConverted = externalShopConverter.convertChunk(player.getLocation().getChunk());
                    sender.sendMessage(ChatColor.GRAY+"Converted "+ChatColor.GREEN+numConverted+ChatColor.GRAY+" "+externalShopConverter.getShopPluginName()+" shops to Shop shops.");
                }
                else{
                    sender.sendMessage("[ShopConverter] "+externalShopConverter.getShopPluginName()+" conversions happen at chunk load automatically.");
                }
            }
            else{
                int numConverted = externalShopConverter.convertAll();
                if(sender instanceof Player) {
                    Player player = (Player) sender;
                    player.sendMessage(ChatColor.GRAY+"Converted "+ChatColor.GREEN+numConverted+ChatColor.GRAY+" "+externalShopConverter.getShopPluginName()+" shops to Shop shops.");
                }
                else{
                    sender.sendMessage("[ShopConverter] Converted "+numConverted+" "+externalShopConverter.getShopPluginName()+" shops to Shop shops.");
                }
            }
            return true;
        }
        return true;
    }

    //creates the chest and sign for Shop and returns the direction of the sign
    public static BlockFace formBlocksFromSign(Sign sign) {
        BlockFace signDirection = null;
        Material signMaterial = null;
        //if instanceof rotatable (regular sign post), get blockface from closest rotation
        if(sign.getBlockData() instanceof Rotatable){
            signDirection = ((Rotatable) sign.getBlockData()).getRotation();
            //adjust the sign direction to coordinal direction if its not already one
            if( signDirection.toString().indexOf('_') != -1) {
                String adjustedDirString = signDirection.toString().substring(0, signDirection.toString().indexOf('_'));
                signDirection = BlockFace.valueOf(adjustedDirString);
            }
            String signMat = sign.getBlockData().getMaterial().toString();
            int index = signMat.indexOf("_SIGN");
            if(index != -1){
                signMaterial = Material.valueOf(signMat.substring(0, index)+"_WALL_SIGN");
            }else{
                return null;
            }

            //make the sign a wall sign, and set the direction to match what it used to be
            sign.setType(signMaterial);
            sign.update(true);
        }
        //if instanceof wallsign, get blockface from facing
        else if (sign.getBlockData() instanceof WallSign) {
            WallSign wallSign = (WallSign) sign.getBlockData();
            signDirection = wallSign.getFacing();
            signMaterial = wallSign.getMaterial();
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

        final BlockFace fSignDirection = signDirection;
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                //when in doubt do a delayed task
                WallSign wallSign = (WallSign) sign.getBlockData();
                wallSign.setFacing(fSignDirection);
                sign.setBlockData(wallSign);
                sign.update();
            }
        }, 1L);

        return signDirection;
    }

    public static Block formBlocksFromChest(Block chestBlock) {
        if(!(chestBlock.getBlockData() instanceof org.bukkit.block.data.type.Chest)){
            return null;
        }
        org.bukkit.block.data.type.Chest chest = (org.bukkit.block.data.type.Chest)chestBlock.getBlockData();

        Block signBlock = chestBlock.getRelative(chest.getFacing());
        signBlock.setType(Material.OAK_WALL_SIGN);

        if(signBlock.getBlockData() instanceof WallSign){
            WallSign wallSign = (WallSign) signBlock.getBlockData();
            wallSign.setFacing(chest.getFacing());
            signBlock.setBlockData(wallSign);
            return signBlock;
        }

        return null;
    }

    public static String getCleanLocation(Location loc, boolean includeWorld){
        String text = "";
        if(includeWorld)
            text = loc.getWorld().getName() + " - ";
        text = text + "("+ loc.getBlockX() + ", "+loc.getBlockY() + ", "+loc.getBlockZ() + ")";
        return text;
    }
}