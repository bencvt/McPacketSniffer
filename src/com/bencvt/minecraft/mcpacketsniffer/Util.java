package com.bencvt.minecraft.mcpacketsniffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * General purpose Java utility methods.
 */
public abstract class Util {
    public static class FileLogger extends Logger {
        /** Formats lines like: "2012-05-21 20:57:06.540 [INFO] example message" */
        public static class LineFormatter extends Formatter
        {
            @Override
            public String format(LogRecord record) {
                StringWriter w = new StringWriter();
                String ts = new Timestamp((new Date()).getTime()).toString();
                w.append(ts);
                for (int pad = ts.length(); pad < 23; pad++) {
                    w.append('0');
                }
                w.append(" [").append(record.getLevel().getName());
                w.append("] ").append(formatMessage(record)).append('\n');
                if (record.getThrown() != null) {
                    record.getThrown().printStackTrace(new PrintWriter(w));
                }
                return w.toString();
            }
        }

        public FileLogger(File baseDirectory, String name, boolean append) {
            super(name, null);
            baseDirectory.mkdirs();
            String logFilePattern = baseDirectory.getPath() + File.separatorChar + name + ".log";
            try {
                FileHandler handler = new FileHandler(logFilePattern, append);
                handler.setFormatter(new LineFormatter());
                addHandler(handler);
            } catch (IOException e) {
                throw new RuntimeException("unable to add file handler for " + logFilePattern, e);
            }
        }
    }

    /**
     * For getting at those pesky private and obfuscated fields.
     * @return the nth (usually 0) field of the specified type declared by
     *         obj's class, or null if there was a reflection error.
     *         <p>
     *         Caveat: this is not guaranteed to work correctly if there are
     *         more than one declared fields of the same type on obj.
     *         <p>
     *         From {@link Class#getDeclaredFields}: "The elements in the
     *         array returned are not sorted and are not in any particular
     *         order."
     *         <p>
     *         In practice things work as expected even if there are multiple
     *         fields of the same type, but this is dependent on the JVM
     *         implementation.
     */
    @SuppressWarnings("rawtypes")
    public static Object getFieldByType(Object obj, Class type, int n) {
        try {
            int index = 0;
            for (Field field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if (field.getType().equals(type)) {
                    if (index == n) {
                        return field.get(obj);
                    }
                    index++;
                }
            }
            throw new RuntimeException("field not found");
        } catch (Exception e) {
            Controller.getEventLog().log(Level.SEVERE,
                    "unable to reflect field type " + type + "#" + n + " for " + obj, e);
            return null;
        }
    }

    public static Object getFieldByName(Object obj, String name) {
        try {
            Field field = obj.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            Controller.getEventLog().log(Level.SEVERE,
                    "unable to reflect field named " + name + " for " + obj, e);
            return null;
        }
    }

    public static void copyResourceToFile(String resourcePath, File destFile) {
        try {
            InputStream in = Controller.class.getResourceAsStream(resourcePath);
            FileOutputStream out = new FileOutputStream(destFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (Exception e) {
            Controller.getEventLog().log(Level.SEVERE,
                    "unable to copy resource " + resourcePath + " to " + destFile, e);
        }
    }
}
