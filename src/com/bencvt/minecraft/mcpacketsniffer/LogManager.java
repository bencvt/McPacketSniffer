package com.bencvt.minecraft.mcpacketsniffer;

import java.io.File;
import java.util.logging.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.src.MemoryConnection;
import net.minecraft.src.INetworkManager;
import net.minecraft.src.Packet;
import net.minecraft.src.PacketHooks;
import net.minecraft.src.PacketHooks.ClientPacketEventListener;
import com.bencvt.minecraft.mcpacketsniffer.Util.FileLogger;

/**
 * Controller class that sets up the environment and creates ConnectionLog instances
 * whenever a new connection is established.
 */
public class LogManager implements ClientPacketEventListener {
    public static final String NAME = "McPacketSniffer";
    public static final String VERSION = "3.0-SNAPSHOT [1.4.2]";
    public static final long OPTIONS_CHECK_RELOAD_INTERVAL = 20000;
    public static final long DUMP_STATS_INTERVAL = 30000;

    public static LogManager instance;
    public static Minecraft minecraft;
    public static Logger eventLog;
    public static Options options;
    public static File baseDirectory;
    public static PacketFilter packetFilter = new PacketFilter();
    public static PacketLoggers packetLoggers = new PacketLoggers();
    private ConnectionLog activeConnectionLog;

    public LogManager(Minecraft minecraft) {
        if (instance != null) {
            throw new IllegalStateException("multiple instances of singleton");
        }
        instance = this;
        LogManager.minecraft = minecraft;
    }

    public void init() {
        baseDirectory = new File(Minecraft.getMinecraftDir(), "mods" + File.separator + NAME);
        baseDirectory.mkdirs();

        eventLog = new FileLogger(baseDirectory, NAME, true);

        options = new Options();
        options.load();
        Options.watchFileForReload();

        PacketHooks.register(this);
    }

    @Override
    public void onNewConnection(INetworkManager connection) {
        if (activeConnectionLog != null) {
            activeConnectionLog.stop("replaced"); // shouldn't happen
            activeConnectionLog = null;
        }
        if (options.INTEGRATED_SERVER || !(connection instanceof MemoryConnection)) {
            activeConnectionLog = new ConnectionLog(connection);
            activeConnectionLog.start();
        }
    }

    @Override
    public void onPacket(INetworkManager connection, Packet packet, boolean send, boolean highPriority) {
        if (activeConnectionLog != null) {
            activeConnectionLog.onPacket(send ? PacketDirection.C2S : PacketDirection.S2C, packet);
        }
    }

    @Override
    public void onCloseConnection(INetworkManager connection, boolean voluntarily, String reason, Object[] reasonArgs) {
        if (activeConnectionLog != null) {
            if (reasonArgs.length > 0) {
                reason += ": ";
                for (int i = 0; i < reasonArgs.length; i++) {
                    if (i > 0) {
                        reason += ", ";
                    }
                    reason += String.valueOf(reasonArgs[i]);
                }
            }
            activeConnectionLog.stop(reason);
            activeConnectionLog = null;
        }
    }
}
