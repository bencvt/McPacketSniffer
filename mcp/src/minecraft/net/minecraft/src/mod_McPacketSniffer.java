package net.minecraft.src;

import bencvt.minecraft.client.mcpacketsniffer.LogManager;

public class mod_McPacketLogger extends BaseMod {
    @Override
    public void load() {
        new LogManager(ModLoader.getMinecraftInstance()).init();
    }

    @Override
    public String getName() {
        return LogManager.NAME;
    }

    @Override
    public String getVersion() {
        return LogManager.VERSION;
    }
}
