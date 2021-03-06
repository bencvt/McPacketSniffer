package net.minecraft.src;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Provides a basic API for inspecting packets sent between the Minecraft
 * client and a Minecraft server. This can include the integrated server.
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
 * 
 * Includes a Forge compatibility layer so this API and Forge's can co-exist
 * without either breaking the other's.
 * <p>
 * Also provides a bare-bones mod loading system: search the jar/package where
 * this class is located for "PacketHooksBootstrap*.class", instantiating each.
 * This makes Forge/ModLoader completely optional for mods using this API.
 * 
 * @author bencvt
 */
public class PacketHooks {
    public static final int VERSION = 9;

    public interface ClientPacketEventListener {
        /**
         * This event occurs whenever the client creates a new TcpConnection or
         * MemoryConnection.
         */
        public void onNewConnection(INetworkManager connection);

        /**
         * This event occurs whenever a TcpConnection or MemoryConnection
         * closes. This event will not be fired if the client is closed without
         * disconnecting first (or if the client crashes, of course.)
         * 
         * @param connection
         * @param reason the reason the connection was closed, e.g.
         *               "disconnect.timeout", "disconnect.closed". Will be
         *               "Quitting" if the client is actively disconnecting
         *               (i.e., they clicked the disconnect button rather than
         *               being kicked or experiencing a network error).
         * @param reasonArgs if this array is non-empty then reason's localized
         *                   string is intended for use in String.format with
         *                   these arguments. Only present for certain error
         *                   types.
         */
        public void onCloseConnection(INetworkManager connection, String reason, Object[] reasonArgs);

        /**
         * This event occurs right before {@link Packet#writePacketData} (when
         * sending) or right before {@link Packet#processPacket} (when
         * receiving).
         * 
         * @param connection
         * @param packet
         * @param send true if the client originated this packet, false if the
         *             server did
         * @param cancelled true if this event was cancelled by another
         *                  ClientPacketEventListener, false normally
         * @return whether to allow the packet to be sent/processed.
         *         <p>
         *         Normally, just return the value of the <b>cancelled</b>
         *         parameter. Alternately, returning <b>true</b> will
         *         explicitly cancel the event, while <b>false</b> will
         *         explicitly allow the event.
         *         <p>
         *         Warning: canceling packets may cause game state
         *         inconsistencies. It's safe for some packet types (e.g.,
         *         chat), but definitely not safe for others (e.g., respawn).
         *         Only return true if you know what you're doing!
         */
        public boolean onPacket(INetworkManager connection, Packet packet, boolean send, boolean cancelled);
    }

    private static boolean modsLoaded;
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

    // ====
    // Dispatch functions, should only be called from modified vanilla classes
    // ====

    protected void dispatchNewConnectionEvent(INetworkManager connection) {
        for (ClientPacketEventListener listener : listeners) {
            listener.onNewConnection(connection);
        }
    }

    /** @return true if the packet should not be sent/processed */
    protected boolean dispatchPacketEvent(INetworkManager connection, Packet packet, boolean send) {
        boolean cancelled = false;
        for (ClientPacketEventListener listener : listeners) {
            cancelled = listener.onPacket(connection, packet, send, cancelled);
        }
        return cancelled;
    }

    protected void dispatchCloseConnectionEvent(INetworkManager connection, String reason, Object[] reasonArgs) {
        for (ClientPacketEventListener listener : listeners) {
            listener.onCloseConnection(connection, reason, reasonArgs);
        }
    }

    /**
     * Forge compatibility layer, verified working as of Forge v6.3.0.
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

    // ====
    // Mod bootstrapper, makes ModLoader/Forge unnecessary.
    // ====

    protected void load() {
        if (modsLoaded) {
            return;
        }
        modsLoaded = true;
        try {
            // Search the jar/zip where this class resides for specifically
            // named classes, and create a new instance for each.
            final String packagePrefix = PacketHooks.class.getPackage() == null ? "" : PacketHooks.class.getPackage().getName() + ".";
            final File src = new File(PacketHooks.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!src.isFile()) {
                // This class doesn't live in a jar, probably because we're in
                // a dev environment (e.g. using MCP's startclient script, or
                // launching in Eclipse).
                // 
                // Do nothing.
                return;
            }
            final FileInputStream fileStream = new FileInputStream(src);
            final ZipInputStream zipStream = new ZipInputStream(fileStream);
            while (true) {
                ZipEntry zipEntry = zipStream.getNextEntry();
                if (zipEntry == null) {
                    zipStream.close();
                    fileStream.close();
                    return;
                }
                String className = zipEntry.getName();
                if (!zipEntry.isDirectory() && className.startsWith("PacketHooksBootstrap") && className.endsWith(".class")) {
                    className = packagePrefix + className.replaceAll(".class$", "");
                    PacketHooks.class.getClassLoader().loadClass(className).newInstance();
                }
            }
        } catch (Exception e) {
            System.err.println("PacketHooks unable to bootstrap mods:");
            e.printStackTrace();
        }
    }
}
