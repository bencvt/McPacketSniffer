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
public class Controller implements ClientPacketEventListener {
    public static final String NAME = "McPacketSniffer";
    public static final String VERSION = "3.0-SNAPSHOT [1.4.2]";
    public static final long OPTIONS_CHECK_RELOAD_INTERVAL = 20000;
    public static final long DUMP_STATS_INTERVAL = 30000;

    private static Controller instance;

    public static final PacketFilter packetFilter = new PacketFilter();
    public static final PacketLoggers packetLoggers = new PacketLoggers();
    private final Logger eventLog;
    private final Options options;
    private final File baseDir;
    private ConnectionLog activeConnectionLog;

    public static Controller getInstance() {
        if (instance == null) {
            new Controller();
        }
        return instance;
    }

    private Controller() {
        instance = this;

        baseDir = new File(Minecraft.getMinecraftDir(), "mods" + File.separator + NAME);
        baseDir.mkdirs();

        eventLog = new FileLogger(baseDir, NAME, true);
        eventLog.info("new Minecraft session, loaded " + NAME + " v" + VERSION);

        options = new Options();
        options.load();
        Options.watchFileForReload();

        PacketHooks.register(this);
    }

    public static Logger getEventLog() {
        return getInstance().eventLog;
    }
    public static Options getOptions() {
        return getInstance().options;
    }
    public static File getBaseDir() {
        return getInstance().baseDir;
    }

    @Override
    public void onNewConnection(INetworkManager connection) {
        Options.reloadOptionsFileIfModified();
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
