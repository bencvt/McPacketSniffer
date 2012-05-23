package bencvt.mcpacketsniffer;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.src.BaseMod;

/**
 * Plug-and-play class for getting java.utils.logging to work with a ModLoader mod.
 * 
 * Setup:
 * 1) Make sure this class is in a package specific to the mod
 * 2) In your mod's constructor or load(), call Log.initForMod(this);
 * 
 * That's it. The log file (and directory, if need be) will be automatically created, located at:
 * [user's minecraft directory]/mods/[YourModName]/[YourModName].log.
 * 
 * Example usage:
 * Log.inGameMessage(minecraft, "whatever");  // log a message to both the in-game chat window and to file
 * Log.getLogger().info("whatever");          // log to file
 * Log.getLogger().setLevel(Level.WARNING);   // filter out info-level and lower messages
 * Log.getLogger().log(Level.SEVERE, "whatever", theException);  // log stack trace to file
 * 
 * @author bencvt
 * @version 1
 */
public abstract class Log {
	public static class LineFormatter extends Formatter
	{
		@Override
		public String format(LogRecord record) {
			StringWriter w = new StringWriter();
			String ts = new Timestamp((new Date()).getTime()).toString();
			w.append(ts);
			for (int pad = ts.length(); pad < 23; pad++)
				w.append('0');
			w.append(' ').append(record.getLevel().getName());
			w.append(' ').append(formatMessage(record)).append('\n');
			if (record.getThrown() != null) {
				record.getThrown().printStackTrace(new PrintWriter(w));
			}
			return w.toString();
		}
	}

	public static String inGameMessagePrefix;

	private static Logger logger;

	public static Logger getLogger() {
		if (logger == null) {
			throw new RuntimeException("logging before initialization");
		}
		return logger;
	}

	public static void inGameMessage(Minecraft minecraft, String msg) {
		if (minecraft != null && minecraft.ingameGUI != null) {
			minecraft.ingameGUI.addChatMessage((inGameMessagePrefix == null ? "" : inGameMessagePrefix) + msg);
		}
		getLogger().info("inGameMessage " + msg.replaceAll("\247[A-Za-z0-9]", ""));
	}

	/**
	 * @return mod directory, created if it wasn't there before
	 */
	public static File initForMod(BaseMod mod) {
		return initForMod(mod, mod.getName());
	}

	/**
	 * In case you want the directory and log name to be something other than mod.getName().
	 * @return mod directory, created if it wasn't there before
	 */
	public static File initForMod(BaseMod mod, String modAlternateName) {
		inGameMessagePrefix = "\247c[" + modAlternateName + "]\247r ";
		File modDirectory = makeModDirectory(modAlternateName);
		init(modDirectory.getPath() + File.separatorChar + modAlternateName + ".log", modAlternateName);
		getLogger().info("loading " + mod.getName() + " version " + mod.getVersion());
		return modDirectory;
	}

	/**
	 * In case you want to specify your own log file path/pattern.
	 * @see http://docs.oracle.com/javase/6/docs/api/java/util/logging/FileHandler.html for pattern syntax.
	 */
	public static void init(String logFilePattern, String logName) {
		logger = Logger.getLogger(logName);
		logger.setLevel(Level.INFO);

		try {
			FileHandler handler = new FileHandler(logFilePattern);
	        handler.setFormatter(new LineFormatter());
	        logger.addHandler(handler);
		} catch (Exception e) {
			throw new RuntimeException("unable to init log " + logFilePattern, e);
		}
	}

	/**
	 * @return mod directory, created if it wasn't there before
	 */
	public static File makeModDirectory(String modName) {
		File modDirectory = new File(Minecraft.getMinecraftDir(), "mods" + File.separatorChar + modName);
		if (!modDirectory.exists())
			modDirectory.mkdirs();
		if (!modDirectory.isDirectory())
			throw new RuntimeException("unable to init mod directory " + modDirectory);
		return modDirectory;
	}
}
