package com.snowgears.shopconverter;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;


public class ConvertListener implements Listener {

    public ShopConverter plugin = ShopConverter.getPlugin();

    public ConvertListener(ShopConverter instance) {
        plugin = instance;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (plugin.externalShopConverter.runAtChunkLoad()) {
            plugin.externalShopConverter.convertChunk(event.getChunk());
        }
    }
}
