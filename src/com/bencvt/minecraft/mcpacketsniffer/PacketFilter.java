package com.bencvt.minecraft.mcpacketsniffer;

import net.minecraft.src.Packet;

public class PacketFilter {
    public boolean shouldLogPacket(PacketDirection dir, Object packet) {
        return Controller.getOptions().packetWhitelist == null ||
                Controller.getOptions().packetWhitelist.isEmpty() ||
                Controller.getOptions().packetWhitelist.contains(((Packet) packet).getPacketId());
    }
}
