package net.minecraft.src;

import com.bencvt.minecraft.mcpacketsniffer.Controller;

/**
 * ModLoader mod definition, not really necessary because we're already
 * creating the Controller in {@link PacketHooksBootstrapMcPacketSniffer}.
 * But keep this definition around so it shows up in the user's list of mods.
 */
public class mod_McPacketSniffer extends BaseMod {
    @Override
    public void load() {
        Controller.getInstance(); // redundant but harmless
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
