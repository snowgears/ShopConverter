package com.snowgears.shopconverter.converter;

import org.bukkit.Chunk;

public interface Converter {

    int convertChunk(Chunk chunk);

    int convertAll();

    String getShopPluginName();

    boolean runAtChunkLoad();
}
