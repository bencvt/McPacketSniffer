package bencvt.minecraft.client.mcpacketsniffer;

import net.minecraft.src.Packet;

public class PacketFilter {
    public boolean shouldLogPacket(PacketDirection dir, Object packet) {
        return LogManager.options.PACKET_WHITELIST == null ||
                LogManager.options.PACKET_WHITELIST.isEmpty() ||
                LogManager.options.PACKET_WHITELIST.contains(((Packet) packet).getPacketId());
    }
}
