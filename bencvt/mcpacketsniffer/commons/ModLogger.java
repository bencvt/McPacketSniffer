package bencvt.mcpacketsniffer.commons;

import java.io.File;
import java.io.IOException;
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
 * Simple plug-and-play file logging for a ModLoader mod.
 * 
 * Uses java.utils.logging, which isn't as robust as other logging packages, but has the
 * advantage of not requiring extra jars.
 * 
 * The log file (and directory, if need be) will be automatically created, located at:
 * [user's minecraft directory]/mods/[YourModName]/[YourModName].log,
 * where [YourModName] is BaseMod.getName(), minus the "mod_" prefix.
 * 
 * Basic usage:
 * 
 * // Declared somewhere in your mod:
 * public final ModLogger log = new ModLogger(this, new ModDirectory(this));
 * 
 * // In case you want to disable file logging entirely without having to comment out a bunch of code:
 * public final ModLogger log = new ModLogger(this, new ModDirectory(this), null);
 * 
 * // Log a message to both the in-game chat window and to file:
 * log.inGameMessage(minecraft, "whatever");
 * 
 * // Log to file:
 * log.info("whatever");
 * 
 * // Filter out info-level and lower messages:
 * log.setLevel(Level.WARNING);
 *                
 * // Log stack trace to file:
 * log.severe(theException, "whatever");
 * 
 * @author bencvt
 * @version 5
 */
public class ModLogger extends Logger {
	/**  Formats lines like: "2012-05-21 20:57:06.540 INFO example message" */
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

	public String prefixInGameMessage;

	public ModLogger(BaseMod mod, ModDirectory modDirectory) {
		this(mod, modDirectory, ModDirectory.getModShortName(mod) + ".log");
	}

	/**
	 * Use this constructor if you want more control over how the log file is created.
	 * @param mod
	 * @param modDirectory
	 * @param logFilePattern - the pattern for the log file in the subdirectory. If null, the log file
	 *                         will not be created and any logging calls will simply go to /dev/null.
	 * @see http://docs.oracle.com/javase/6/docs/api/java/util/logging/FileHandler.html for pattern syntax.
	 */
	public ModLogger(BaseMod mod, ModDirectory modDirectory, String logFilePattern) {
		super(modDirectory.getName(), null);
		prefixInGameMessage = "\247c[" + String.valueOf(modDirectory) + "]\247r ";
		if (logFilePattern == null) {
			setLevel(Level.OFF);
			return;
		}
		addFileHandler(modDirectory.getPath() + File.separatorChar + logFilePattern);
		setLevel(Level.INFO);
		info("loading " + mod.getName() + " version " + mod.getVersion());
	}

	public void addFileHandler(String logFilePattern) {
		try {
			FileHandler handler = new FileHandler(logFilePattern);
			handler.setFormatter(new LineFormatter());
			addHandler(handler);
		} catch (IOException e) {
			throw new RuntimeException("unable to add file handler for " + logFilePattern, e);
		}
	}

	/** Log a message to both the in-game chat window and to file. */
	public void inGameMessage(Minecraft minecraft, String msg) {
		if (minecraft != null && minecraft.ingameGUI != null) {
			minecraft.ingameGUI.addChatMessage(
					(prefixInGameMessage == null ? "" : prefixInGameMessage) + msg);
		}
		info("inGameMessage " + msg.replaceAll("\247[A-Za-z0-9]", ""));
	}

	/**
	 * Convenience method for logging stack traces for serious (level SEVERE) problems.
	 * @see Logger.throwing() for non-serious problems.
	 */
	public void severe(Throwable t) {
		severe(t, "stack trace");
	}
	/**
	 * Convenience method for logging stack traces for serious (level SEVERE) problems.
	 * @see Logger.throwing() for non-serious problems.
	 */
	public void severe(Throwable t, String msg) {
		log(Level.SEVERE, msg, t);
	}
}
