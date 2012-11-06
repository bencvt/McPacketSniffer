package com.bencvt.minecraft.mcpacketsniffer;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Level;

import net.minecraft.client.Minecraft;

public class Options {
    public static final File OPTIONS_FILE = new File(Controller.getBaseDir(), "options.txt");
    private static long lastModified;

    // TODO: implement newFilePerServer and statsDump
    public final boolean newFilePerConnection;
    public final boolean newFilePerServer;
    public final boolean integratedServer;
    public final Collection<Integer> packetWhitelist;
    public final boolean flushAfterEveryPacket;
    public final long flushInterval;
    public final boolean coordsIncludeRegion;
    public final boolean coordsIncludeChunk;
    public final boolean summarizeBinaryData;
    public final String colorEscape;
    public final boolean logMissingCodes;
    public final boolean statsDump;
    public final boolean statsAllPackets;

    public Options() {
        Properties props = loadProperties();
        newFilePerConnection = Boolean.parseBoolean(props.getProperty("new-file-per-connection"));
        newFilePerServer = Boolean.parseBoolean(props.getProperty("new-file-per-server"));
        integratedServer = Boolean.parseBoolean(props.getProperty("integrated-server"));
        packetWhitelist = loadIntegerCollection(props.getProperty("packet-whitelist"), new HashSet<Integer>(), true);
        flushAfterEveryPacket = Boolean.parseBoolean(props.getProperty("flush-after-every-packet"));
        flushInterval = Long.parseLong(props.getProperty("flush-interval"));
        coordsIncludeRegion = Boolean.parseBoolean(props.getProperty("coords-include-region"));
        coordsIncludeChunk = Boolean.parseBoolean(props.getProperty("coords-include-chunk"));
        summarizeBinaryData = Boolean.parseBoolean(props.getProperty("summarize-binary-data"));
        colorEscape = props.getProperty("color-escape").trim();
        logMissingCodes = Boolean.parseBoolean(props.getProperty("log-missing-codes"));
        statsDump = Boolean.parseBoolean(props.getProperty("stats-dump"));
        statsAllPackets = Boolean.parseBoolean(props.getProperty("stats-all-packets"));
    }

    private static Properties loadProperties() {
        if (!OPTIONS_FILE.exists()) {
            copyDefaults();
        }
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(OPTIONS_FILE));
        } catch (Exception softFail) {
            try {
                File moveTo = OPTIONS_FILE;
                for (int i = 0; moveTo.exists(); i++) {
                    moveTo = new File(Controller.getBaseDir(), "options_" + i + "_invalid.txt");
                }
                OPTIONS_FILE.renameTo(moveTo);
                Controller.getEventLog().log(Level.SEVERE,
                        "Unable to load options. Renamed to " + moveTo, softFail);
                copyDefaults();
                properties.load(new FileInputStream(OPTIONS_FILE));
            } catch (Exception hardFail) {
                Controller.getEventLog().log(Level.SEVERE,
                        "unable to gracefully handle invalid options", hardFail);
                throw new RuntimeException(hardFail);
            }
        }
        lastModified = OPTIONS_FILE.lastModified();
        return properties;
    }

    private static void copyDefaults() {
        Util.copyResourceToFile("default-options.properties", OPTIONS_FILE);
        Controller.getEventLog().info("Restored " + OPTIONS_FILE);
    }

    private static Collection<Integer> loadIntegerCollection(String items, Collection<Integer> storage, boolean makeUnmodifiable) {
        storage.clear();
        for (String item : items.split(",")) {
            item = item.trim();
            if (item.isEmpty()) {
                continue;
            }
            if (item.startsWith("0x")) {
                storage.add(Integer.parseInt(item.substring(2), 16));
            } else {
                storage.add(Integer.parseInt(item));
            }
        }
        if (makeUnmodifiable) {
            return Collections.unmodifiableCollection(storage);
        }
        return storage;
    }

    public static void watchFileForReload() {
        new Thread(Controller.NAME + " options file watcher") {
            @Override
            public void run() {
                while (Minecraft.getMinecraft().running) {
                    try {
                        sleep(Controller.OPTIONS_CHECK_RELOAD_INTERVAL);
                    } catch (InterruptedException e) {
                        return;
                    }
                    reloadOptionsFileIfModified();
                }
            }
        }.start();
    }

    public static synchronized void reloadOptionsFileIfModified() {
        if (!OPTIONS_FILE.exists() || OPTIONS_FILE.lastModified() > lastModified) {
            Controller.getEventLog().info("Reloading " + OPTIONS_FILE +
                    ". Some changes won't take effect until a new connection is established.");
            Controller.reloadOptions();
        }
    }
}
