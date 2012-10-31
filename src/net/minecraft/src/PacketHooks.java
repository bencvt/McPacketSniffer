package net.minecraft.src;

import java.util.LinkedHashSet;

/**
 * Provide an API for inspecting packets sent between the Minecraft client
 * and a Minecraft server. This can include the integrated server.
 * 
 * This does NOT capture every piece of data sent and received by the
 * Minecraft client. Known exceptions:
 *  - Server pings (client <-> server) @see GuiMultiplayer
 *  - Server login verification (client <-> session.minecraft.net) @see NetClientHandler, ThreadLoginVerifier
 *  - Account login (client launcher <-> login.minecraft.net)
 *  - Resource downloads (client <-> s3.amazonaws.com) @see ThreadDownloadResources
 *  - Skins and cloaks (client <-> skins.minecraft.net) @see EntityPlayerSP, EntityOtherPlayerMP
 *  - Usage reporting (client <-> snoop.minecraft.net) @see PlayerUsageSnooper
 * 
 * @author bencvt
 */
public class PacketHooks {
    public static final int VERSION = 2;

    public interface ClientPacketEventListener {
        /**
         * This event occurs whenever the client creates a new TcpConnection or
         * MemoryConnection.
         */
        public void onNewConnection(NetworkManager connection);

        /**
         * This event occurs right before Packet.writePacketData() (when sending)
         * or right before Packet.processPacket() (when receiving).
         * 
         * @param connection
         * @param packet
         * @param send true if the client originated this packet, false if the server did
         * @param highPriority normally packets read from the input stream sit in a queue
         *     for later processing. However certain packets are processed immediately.
         *     @see Packet.isWritePacket().
         */
        public void onPacket(NetworkManager connection, Packet packet, boolean send, boolean highPriority);
    }

    private static LinkedHashSet<ClientPacketEventListener> listeners = new LinkedHashSet<ClientPacketEventListener>();

    public static boolean register(ClientPacketEventListener listener) {
        return register(listener, true);
    }

    public static boolean register(ClientPacketEventListener listener, boolean verifyPatch) {
        if (verifyPatch && (!isTcpConnectionPatched() || !isMemoryConnectionPatched())) {
            throw new RuntimeException("Unable to register packet events." +
                    " This is most likely due to a mod overwriting " +
                    MemoryConnection.class.getSimpleName() +
                    ".class (MemoryConnection) or " +
                    TcpConnection.class.getSimpleName() +
                    ".class (TcpConnection).");
        }
        return listeners.add(listener);
    }

    public static boolean isTcpConnectionPatched() {
        try {
            return TcpConnection.packetHooksClient.getClass().equals(PacketHooks.class);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isMemoryConnectionPatched() {
        try {
            return MemoryConnection.packetHooksClient.getClass().equals(PacketHooks.class);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean unregister(ClientPacketEventListener listener) {
        return listeners.remove(listener);
    }

    //
    // Dispatch functions, should only be called from modified vanilla classes
    //

    protected void dispatchNewConnectionEvent(NetworkManager connection) {
        for (ClientPacketEventListener listener : listeners) {
            listener.onNewConnection(connection);
        }
    }

    protected void dispatchPacketEvent(NetworkManager connection, Packet packet, boolean send, boolean highPriority) {
        for (ClientPacketEventListener listener : listeners) {
            listener.onPacket(connection, packet, send, highPriority);
        }
    }
}
