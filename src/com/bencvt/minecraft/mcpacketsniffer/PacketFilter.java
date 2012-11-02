package com.bencvt.minecraft.mcpacketsniffer;

import net.minecraft.src.Packet;

public class PacketFilter {
    public boolean shouldLogPacket(PacketDirection dir, Object packet) {
        return Controller.getOptions().PACKET_WHITELIST == null ||
                Controller.getOptions().PACKET_WHITELIST.isEmpty() ||
                Controller.getOptions().PACKET_WHITELIST.contains(((Packet) packet).getPacketId());
    }
}
