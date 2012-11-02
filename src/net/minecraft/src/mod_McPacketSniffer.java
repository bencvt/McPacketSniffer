package net.minecraft.src;

import com.bencvt.minecraft.mcpacketsniffer.Controller;

public class mod_McPacketSniffer extends BaseMod {
    @Override
    public void load() {
        Controller.getInstance();
    }

    @Override
    public String getName() {
        return Controller.NAME;
    }

    @Override
    public String getVersion() {
        return Controller.VERSION;
    }
}
