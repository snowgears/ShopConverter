package com.snowgears.shopconverter;

import org.bukkit.event.Listener;


public class ConvertListener implements Listener {

    public ShopConverter plugin = ShopConverter.getPlugin();

    public ConvertListener(ShopConverter instance) {
        plugin = instance;
    }
}
