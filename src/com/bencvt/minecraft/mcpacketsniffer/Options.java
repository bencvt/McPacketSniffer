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

    public final Collection<Integer> packetWhitelist;
    public final boolean flushAfterEveryPacket;
    public final long flushInterval;
    public final boolean coordsIncludeRegion;
    public final boolean coordsIncludeChunk;
    public final boolean summarizeBinaryData;
    public final String colorEscape;
    public final boolean logMissingCodes;

    public final boolean newFilePerConnection;
    public final boolean newFilePerServer;
    public final boolean integratedServer;
    public final boolean statsDump;
    public final boolean statsAllPackets;

    public Options() {
        this(loadProperties());
    }

    private Options(Properties props) {
        packetWhitelist = loadIntegerCollection(notNull(props, "packet-whitelist"), new HashSet<Integer>(), true);
        flushAfterEveryPacket = Boolean.parseBoolean(notNull(props, "flush-after-every-packet"));
        flushInterval = Long.parseLong(notNull(props, "flush-interval"));
        coordsIncludeRegion = Boolean.parseBoolean(notNull(props, "coords-include-region"));
        coordsIncludeChunk = Boolean.parseBoolean(notNull(props, "coords-include-chunk"));
        summarizeBinaryData = Boolean.parseBoolean(notNull(props, "summarize-binary-data"));
        colorEscape = notNull(props, "color-escape").trim();
        logMissingCodes = Boolean.parseBoolean(notNull(props, "log-missing-codes"));

        newFilePerConnection = Boolean.parseBoolean(notNull(props, "new-file-per-connection"));
        newFilePerServer = Boolean.parseBoolean(notNull(props, "new-file-per-server"));
        integratedServer = Boolean.parseBoolean(notNull(props, "integrated-server"));
        statsDump = Boolean.parseBoolean(notNull(props, "stats-dump"));
        statsAllPackets = Boolean.parseBoolean(notNull(props, "stats-all-packets"));
    }

    private static Properties loadProperties() {
        if (!OPTIONS_FILE.exists()) {
            copyDefaults();
        }
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(OPTIONS_FILE));
            new Options(props); // create a throwaway to validate
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
                props.load(new FileInputStream(OPTIONS_FILE));
            } catch (Exception hardFail) {
                Controller.getEventLog().log(Level.SEVERE,
                        "unable to gracefully handle invalid options", hardFail);
                throw new RuntimeException(hardFail);
            }
        }
        lastModified = OPTIONS_FILE.lastModified();
        return props;
    }

    private static void copyDefaults() {
        Util.copyResourceToFile("default-options.properties", OPTIONS_FILE);
        Controller.getEventLog().info("Restored " + OPTIONS_FILE);
    }

    private static String notNull(Properties props, String name) {
        String result = props.getProperty(name);
        if (result == null) {
            throw new IllegalArgumentException("missing required property: " + String.valueOf(name));
        }
        return result;
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
