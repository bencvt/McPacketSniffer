package com.bencvt.minecraft.mcpacketsniffer;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Level;

public class Options {
    public boolean NEW_FILE_PER_CONNECTION;
    public boolean INTEGRATED_SERVER;
    public HashSet<Integer> PACKET_WHITELIST = new HashSet<Integer>();
    public boolean FLUSH_AFTER_EVERY_PACKET;
    public long FLUSH_INTERVAL;
    public boolean COORDS_INCLUDE_REGION;
    public boolean COORDS_INCLUDE_CHUNK;
    public boolean SUMMARIZE_BINARY_DATA;
    public String COLOR_ESCAPE;
    public boolean STATS_ALL_PACKETS;

    public void load() {
        File optionsFile = getOptionsFile();
        if (!optionsFile.exists()) {
            copyDefaults(optionsFile);
        }
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(optionsFile));
            loadWork(properties);
        } catch (Exception softFail) {
            try {
                File moveTo = optionsFile;
                for (int i = 0; moveTo.exists(); i++) {
                    moveTo = new File(LogManager.baseDirectory, "options_" + i + "_invalid.txt");
                }
                optionsFile.renameTo(moveTo);
                LogManager.eventLog.log(Level.SEVERE,
                        "Unable to load options. Renamed to " + moveTo, softFail);
                copyDefaults(optionsFile);
                properties.load(new FileInputStream(optionsFile));
                loadWork(properties);
            } catch (Exception hardFail) {
                LogManager.eventLog.log(Level.SEVERE,
                        "unable to gracefully handle invalid options", hardFail);
                throw new RuntimeException(hardFail);
            }
        }
    }

    private static void copyDefaults(File optionsFile) {
        Util.copyResourceToFile("/com/bencvt/minecraft/mcpacketsniffer/default-options.properties", optionsFile);
        LogManager.eventLog.info("Restored " + optionsFile);
    }

    private void loadWork(Properties properties) {
        NEW_FILE_PER_CONNECTION = Boolean.parseBoolean(properties.getProperty("new-file-per-connection"));
        INTEGRATED_SERVER = Boolean.parseBoolean(properties.getProperty("integrated-server"));
        loadIntegerList(PACKET_WHITELIST, properties.getProperty("packet-whitelist"));
        FLUSH_AFTER_EVERY_PACKET = Boolean.parseBoolean(properties.getProperty("flush-after-every-packet"));
        FLUSH_INTERVAL = Long.parseLong(properties.getProperty("flush-interval"));
        COORDS_INCLUDE_REGION = Boolean.parseBoolean(properties.getProperty("coords-include-region"));
        COORDS_INCLUDE_CHUNK = Boolean.parseBoolean(properties.getProperty("coords-include-chunk"));
        SUMMARIZE_BINARY_DATA = Boolean.parseBoolean(properties.getProperty("summarize-binary-data"));
        COLOR_ESCAPE = properties.getProperty("color-escape").trim();
        STATS_ALL_PACKETS = Boolean.parseBoolean(properties.getProperty("stats-all-packets"));
    }

    private static void loadIntegerList(Collection<Integer> list, String items) {
        list.clear();
        for (String item : items.split(",")) {
            item = item.trim();
            if (item.isEmpty()) {
                continue;
            }
            if (item.startsWith("0x")) {
                list.add(Integer.parseInt(item.substring(2), 16));
            } else {
                list.add(Integer.parseInt(item));
            }
        }
    }

    public static File getOptionsFile() {
        return new File(LogManager.baseDirectory, "options.txt");
    }

    public static void watchFileForReload() {
        new Thread(LogManager.NAME + " options file watcher") {
            @Override
            public void run() {
                File optionsFile = getOptionsFile();
                long lastModified = optionsFile.lastModified();
                while (LogManager.minecraft.running) {
                    if (optionsFile.lastModified() > lastModified) {
                        LogManager.eventLog.info("Reloading " + optionsFile +
                                ". Some changes won't take effect until a new connection is established.");
                        Options newOptions = new Options();
                        newOptions.load();
                        LogManager.options = newOptions;
                        lastModified = optionsFile.lastModified();
                    }
                    try {
                        sleep(LogManager.OPTIONS_CHECK_RELOAD_INTERVAL);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }.start();
    }
}
