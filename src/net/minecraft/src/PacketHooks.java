package net.minecraft.src;

import java.util.LinkedHashSet;

/**
 * Provide an API for inspecting packets sent between the Minecraft client
 * and a Minecraft server. This can include the integrated server.
 * <p>
 * This does *not* capture every piece of data sent and received by the
 * Minecraft client. Known exceptions:<ul>
 * <li>Server pings (client <-> server):
 *     {@link GuiMultiplayer}</li>
 * <li>Server login verification (client <-> session.minecraft.net):
 *     {@link NetClientHandler} and {@link ThreadLoginVerifier}</li>
 * <li>Account login (client launcher <-> login.minecraft.net)</li>
 * <li>Resource downloads (client <-> s3.amazonaws.com):
 *     {@link ThreadDownloadResources}</li>
 * <li>Skins and capes (client <-> skins.minecraft.net):
 *     {@link EntityPlayerSP} and {@link EntityOtherPlayerMP}</li>
 * <li>Usage reporting (client <-> snoop.minecraft.net):
 *     {@link PlayerUsageSnooper}</li>
 * <li>Mod-specific connections (client <-> wherever, e.g. the mod's website to
 *     automatically check for updates)</li>
 * </ul>
 * @author bencvt
 */
public class PacketHooks {
    public static final int VERSION = 4;

    public interface ClientPacketEventListener {
        /**
         * This event occurs whenever the client creates a new TcpConnection or
         * MemoryConnection.
         */
        public void onNewConnection(INetworkManager connection);

        /**
         * This event occurs right before Packet.writePacketData() (when sending)
         * or right before Packet.processPacket() (when receiving).
         * 
         * @param connection
         * @param packet
         * @param send true if the client originated this packet, false if the
         *             server did
         * @param highPriority normally packets read from the input stream sit
         *                     in a queue for later processing. However certain
         *                     packets (flagged "isWritePacket") are processed
         *                     immediately.
         * @see Packet#isWritePacket
         */
        public void onPacket(INetworkManager connection, Packet packet, boolean send, boolean highPriority);

        /**
         * This event occurs whenever a TcpConnection or MemoryConnection
         * closes.
         * 
         * @param connection
         * @param voluntarily if true, the player chose to disconnect in the
         *                    in-game GUI. If false, the server booted the
         *                    player or the connection failed otherwise.
         * @param reason the reason the connection was closed, e.g.
         *               "disconnect.timeout".
         * @param reasonArgs if this array is non-empty then reason's localized
         *                   string is intended for use in String.format with
         *                   these arguments. Only present for certain error
         *                   types.
         */
        public void onCloseConnection(INetworkManager connection, boolean voluntarily, String reason, Object[] reasonArgs);
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

    protected void dispatchNewConnectionEvent(INetworkManager connection) {
        for (ClientPacketEventListener listener : listeners) {
            listener.onNewConnection(connection);
        }
    }

    protected void dispatchPacketEvent(INetworkManager connection, Packet packet, boolean send, boolean highPriority) {
        for (ClientPacketEventListener listener : listeners) {
            listener.onPacket(connection, packet, send, highPriority);
        }
    }

    protected void dispatchCloseConnectionEvent(INetworkManager connection, boolean voluntarily, String reason, Object[] reasonArgs) {
        for (ClientPacketEventListener listener : listeners) {
            listener.onCloseConnection(connection, voluntarily, reason, reasonArgs);
        }
    }

    /**
     * Forge compatibility
     */
    protected void dispatchForgeRemoteCloseConnectionEvent(INetworkManager connection, NetHandler netHandler) {
        try {
            EntityPlayer player = (EntityPlayer) NetHandler.class.getMethod("getPlayer").invoke(netHandler);
            getClass()
            .getClassLoader()
            .loadClass("cpw.mods.fml.common.network.FMLNetworkHandler")
            .getMethod("onConnectionClosed", INetworkManager.class, EntityPlayer.class)
            .invoke(null, connection, player);
        } catch (Throwable t) {
            // Do nothing. Forge must not be installed.
        }
    }

    protected PacketHooks() {
        // Do nothing. Just marking the constructor as protected.
    }
}
