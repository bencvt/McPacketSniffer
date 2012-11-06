package com.bencvt.minecraft.mcpacketsniffer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;

/**
 * Keep track of the number of packets of each type for a specific
 * connection, and periodically dump the stats to a text file.
 * 
 * Each StatRecorder is owned by a ConnectionLog.
 */
public class StatRecorder {
    public static final int PACKET_HEADER_SIZE = 1; // byte packetId

    private final long[] c2sPacketCounts = new long[256];
    private final long[] c2sPacketBytes = new long[256];
    private final long[] s2cPacketCounts = new long[256];
    private final long[] s2cPacketBytes = new long[256];
    private final Object packetCountsLock = new Object();
    private final String connectionAddress;
    private final long startTimestamp;
    private final File statsFile;
    private boolean stopWorkerThread;

    public StatRecorder(String connectionAddress, long startTimestamp, File statsFile) {
        this.connectionAddress = connectionAddress;
        this.startTimestamp = startTimestamp;
        this.statsFile = statsFile;
    }

    public void start() {
        new Thread(Controller.NAME + " stat recorder") {
            @Override
            public void run() {
                while (!stopWorkerThread) {
                    dumpStats();
                    try {
                        sleep(Controller.DUMP_STATS_INTERVAL);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }.start();
    }

    public void stop() {
        stopWorkerThread = true;
        dumpStats();
    }

    public void record(PacketDirection dir, int packetId, int packetPayloadSize) {
        synchronized (packetCountsLock) {
            if (dir == PacketDirection.C2S) {
                c2sPacketCounts[packetId] += 1;
                c2sPacketBytes[packetId] += PACKET_HEADER_SIZE + packetPayloadSize;
            } else {
                s2cPacketCounts[packetId] += 1;
                s2cPacketBytes[packetId] += PACKET_HEADER_SIZE + packetPayloadSize;
            }
        }
    }

    public synchronized void dumpStats() {
        long now = System.currentTimeMillis();
        long interval = now - startTimestamp;
        try {
            PrintWriter writer = new PrintWriter(statsFile);
            writer.printf("Packet stats over %.3f seconds", interval / 1000.0);
            writer.println();
            writer.print("from ");
            writer.println(PacketLoggersBase.timestampToString(startTimestamp));
            writer.print("  to ");
            writer.println(PacketLoggersBase.timestampToString(now));
            writer.print("Server: ");
            writer.println(connectionAddress);
            synchronized (packetCountsLock) {
                writer.println("Client to server:");
                writeStatsTable(writer, interval, c2sPacketCounts, c2sPacketBytes);
                writer.println("Server to client:");
                writeStatsTable(writer, interval, s2cPacketCounts, s2cPacketBytes);
            }
            writer.close();
        } catch (IOException e) {
            Controller.getEventLog().log(Level.SEVERE, "unable to save stats", e);
            stopWorkerThread = true;
            // don't stop the packet log manager
        }
    }

    private static void writeStatsTable(PrintWriter writer, long intervalMillis, long[] packetCounts, long[] byteCounts) {
        writer.println("  packet          name     count    approx bytes   average");
        writer.println("-------- ------------- --------- --------------- ---------");

        long packetTotal = 0L;
        long byteTotal = 0L;
        for (int i = 0; i < packetCounts.length; i++) {
            if (Controller.getOptions().statsAllPackets || packetCounts[i] > 0) {
                String packetName = PacketInfo.getPacketShortName(i);
                if (packetName == null) {
                    continue;
                }
                packetTotal += packetCounts[i];
                byteTotal += byteCounts[i];
                writer.printf(" %3d(%02X)  %-12s %9d %15d %9.2f",
                        i, i, packetName, packetCounts[i], byteCounts[i],
                        averagePacketSize(byteCounts[i], packetCounts[i]));
                writer.println();
            }
        }
        writer.printf("        * all          %9d %15d %9.2f",
                packetTotal, byteTotal,
                averagePacketSize(byteTotal, packetTotal));
        writer.println();

        // This isn't the true bandwidth as sent over the wire. We're up at the application level,
        // and many of the packet payload sizes are reported inaccurately by their classes.
        // It's a decent approximation, though.
        double bps = 0.0;
        if (intervalMillis != 0L) {
            // bits per second, not bytes per millisecond
            bps = byteTotal * 8.0 / (intervalMillis / 1000.0);
        }
        // kilo/mega, not kibi/mebi
        writer.printf("Approximate bandwidth: %.2f bps = %.2f kbps = %.2f Mbps",
                bps, bps / 1000.0, bps / 1000.0 / 1000.0);
        writer.println();
    }

    private static double averagePacketSize(long bytes, long packets) {
        if (packets == 0L) {
            return 0.0;
        }
        return (double) bytes / (double) packets;
    }
}
