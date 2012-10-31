package com.bencvt.minecraft.mcpacketsniffer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;

import net.minecraft.src.INetworkManager;
import net.minecraft.src.Packet;

/**
 * Logs the packet sent/received for a single connection to file.
 * 
 * Each ConnectionLog is owned by LogManager.
 */
public class ConnectionLog {
    private final INetworkManager connection;
    private PrintWriter logWriter;
    private Object logWriterLock = new Object();
    private StatRecorder stats;
    private Thread flusherThread;

    public ConnectionLog(INetworkManager connection) {
        this.connection = connection;
    }

    public boolean isRunning() {
        return logWriter != null;
    }

    public boolean start() {
        if (isRunning()) {
            throw new IllegalStateException();
        }
        long now = System.currentTimeMillis();

        String connectionAddress = PacketLoggersBase.connectionAddressToString(connection);
        LogManager.eventLog.info("starting log for " + connectionAddress);

        // determine log file name
        File outputDir = LogManager.baseDirectory;
        String suffix = "";
        if (LogManager.options.NEW_FILE_PER_CONNECTION) {
            // e.g., "_20120518_133948_mc.example.com"
            suffix = "_" + PacketLoggersBase.timestampToString(now)
                    .replace(' ', '_').replaceAll("[:\\-]", "").substring(0, 15);
            suffix += "_" + connectionAddress;
            // use a subdirectory
            outputDir = new File(LogManager.baseDirectory, "logs");
        }
        outputDir.mkdirs();
        File logFile = new File(outputDir, "packets" + suffix + ".txt");

        // open file
        boolean fileExists = logFile.exists();
        try {
            logWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
        } catch (IOException e) {
            LogManager.eventLog.log(Level.SEVERE, "unable to open packet log file for writing", e);
            logWriter = null;
            return false;
        }

        // start recording stats
        try {
            stats = new StatRecorder(connectionAddress, now, new File(outputDir, "stats" + suffix + ".txt"));
            stats.start();
        } catch (Exception e) {
            LogManager.eventLog.log(Level.SEVERE, "unable to start recording stats", e);
            logWriter.close();
            logWriter = null;
            return false;
        }

        if (fileExists) {
            // in case the last line ended abruptly due to a client crash
            logWriter.println();
            logWriter.println();
        }

        StringBuilder line = new StringBuilder();
        PacketLoggersBase.logTimestamp(line, System.currentTimeMillis());
        line.append(" new connection to ").append(connectionAddress);
        logWriter.println(line.toString());
        logWriter.flush();

        if (LogManager.options.FLUSH_INTERVAL > 0 && !LogManager.options.FLUSH_AFTER_EVERY_PACKET) {
            startFlusherThread();
        }

        return true;
    }

    private void startFlusherThread() {
        flusherThread = new Thread(LogManager.NAME + " packet log flusher") {
            @Override
            public void run() {
                while (isRunning()) {
                    synchronized (logWriterLock) {
                        if (logWriter != null) {
                            logWriter.flush();
                        }
                    }
                    try {
                        sleep(Math.max(1000, LogManager.options.FLUSH_INTERVAL));
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        };
        flusherThread.start();
    }

    public void stop() {
        if (isRunning()) {
            synchronized (logWriterLock) {
                logWriter.close();
                logWriter = null;
            }
            stats.stop();
        }
    }

    public void onPacket(PacketDirection dir, Packet packet) {
        if (!isRunning()) {
            return;
        }

        // increment stats
        stats.record(dir, packet.getPacketId(), packet.getPacketSize());

        // filter packets
        if (!LogManager.packetFilter.shouldLogPacket(dir, packet)) {
            return;
        }

        // assemble and write log line
        StringBuilder line = new StringBuilder(160);
        logPacket(line, dir, packet);
        String lineString = line.toString();
        if (flusherThread == null) {
            logLine(lineString);
        } else {
            synchronized (logWriterLock) {
                logLine(lineString);
            }
        }
    }

    private void logLine(String line) {
        logWriter.println(line.toString());
        if (LogManager.options.FLUSH_AFTER_EVERY_PACKET) {
            logWriter.flush();
        }
    }

    private static void logPacket(StringBuilder line, PacketDirection dir, Packet packet) {
        // start off the line with timestamp, direction, packet id, and packet name
        PacketLoggersBase.logTimestamp(line, System.currentTimeMillis());
        line.append(' ').append(dir);
        line.append(" 0x").append(String.format("%02X", packet.getPacketId()));
        String packetName = PacketInfo.getPacketShortName(packet.getPacketId());
        if (packetName == null) {
            packetName = "????????????";
            LogManager.eventLog.severe("packet missing from PacketInfo: " + packet.getPacketId());
        }
        line.append(' ').append(packetName);
        for (int pad = packetName.length(); pad < 13; pad++) {
            line.append(' ');
        }

        // packet-specific loggers handle the rest of the line's contents
        LogManager.packetLoggers.dispatch(line, dir, packet);
    }
}
