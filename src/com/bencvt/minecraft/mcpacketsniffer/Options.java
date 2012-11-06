package com.bencvt.minecraft.mcpacketsniffer;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Level;

import net.minecraft.client.Minecraft;

public class Options {
    // TODO: refactor these to public final
    // TODO: implement newFilePerServer and statsDump
    public boolean NEW_FILE_PER_CONNECTION;
    public boolean NEW_FILE_PER_SERVER;
    public boolean INTEGRATED_SERVER;
    public HashSet<Integer> PACKET_WHITELIST = new HashSet<Integer>();
    public boolean FLUSH_AFTER_EVERY_PACKET;
    public long FLUSH_INTERVAL;
    public boolean COORDS_INCLUDE_REGION;
    public boolean COORDS_INCLUDE_CHUNK;
    public boolean SUMMARIZE_BINARY_DATA;
    public String COLOR_ESCAPE;
    public boolean LOG_MISSING_CODES;
    public boolean STATS_DUMP;
    public boolean STATS_ALL_PACKETS;

    private static long lastModified;

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
                    moveTo = new File(Controller.getBaseDir(), "options_" + i + "_invalid.txt");
                }
                optionsFile.renameTo(moveTo);
                Controller.getEventLog().log(Level.SEVERE,
                        "Unable to load options. Renamed to " + moveTo, softFail);
                copyDefaults(optionsFile);
                properties.load(new FileInputStream(optionsFile));
                loadWork(properties);
            } catch (Exception hardFail) {
                Controller.getEventLog().log(Level.SEVERE,
                        "unable to gracefully handle invalid options", hardFail);
                throw new RuntimeException(hardFail);
            }
        }
        lastModified = optionsFile.lastModified();
    }

    private static void copyDefaults(File optionsFile) {
        Util.copyResourceToFile("/com/bencvt/minecraft/mcpacketsniffer/default-options.properties", optionsFile);
        Controller.getEventLog().info("Restored " + optionsFile);
    }

    private void loadWork(Properties properties) {
        NEW_FILE_PER_CONNECTION = Boolean.parseBoolean(properties.getProperty("new-file-per-connection"));
        NEW_FILE_PER_SERVER = Boolean.parseBoolean(properties.getProperty("new-file-per-server"));
        INTEGRATED_SERVER = Boolean.parseBoolean(properties.getProperty("integrated-server"));
        loadIntegerList(PACKET_WHITELIST, properties.getProperty("packet-whitelist"));
        FLUSH_AFTER_EVERY_PACKET = Boolean.parseBoolean(properties.getProperty("flush-after-every-packet"));
        FLUSH_INTERVAL = Long.parseLong(properties.getProperty("flush-interval"));
        COORDS_INCLUDE_REGION = Boolean.parseBoolean(properties.getProperty("coords-include-region"));
        COORDS_INCLUDE_CHUNK = Boolean.parseBoolean(properties.getProperty("coords-include-chunk"));
        SUMMARIZE_BINARY_DATA = Boolean.parseBoolean(properties.getProperty("summarize-binary-data"));
        COLOR_ESCAPE = properties.getProperty("color-escape").trim();
        LOG_MISSING_CODES = Boolean.parseBoolean(properties.getProperty("log-missing-codes"));
        STATS_DUMP = Boolean.parseBoolean(properties.getProperty("stats-dump"));
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
        return new File(Controller.getBaseDir(), "options.txt");
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
        File optionsFile = getOptionsFile();
        if (!optionsFile.exists() || optionsFile.lastModified() > lastModified) {
            Controller.getEventLog().info("Reloading " + optionsFile +
                    ". Some changes won't take effect until a new connection is established.");
            Controller.getOptions().load();
        }
    }
}
