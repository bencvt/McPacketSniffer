package bencvt.mcpacketsniffer.commons;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;

import net.minecraft.src.BaseMod;

/**
 * Load/save user preferences from a Java properties file.
 * 
 * Basic usage:
 * 
 * // Extend this class as UserPreferences (or whatever), implementing the required methods to reset/load/save.
 * 
 * // Declared somewhere in your mod (probably better to refactor this into multiple lines):
 * public final UserPreferences preferences = new UserPreferences(this, new ModLogger(this, new ModDirectory()));
 * 
 * // Then in load():
 * preferences.load(new File(new ModDirectory(), "options.txt"));
 * 
 * // Then wherever in your code:
 * if (preferences.isSomePreferenceEnabled) doWhatever();
 * 
 * @author bencvt
 * @version 5
 */
public abstract class UserPreferencesBase {
	protected BaseMod parent;
	protected ModLogger log;
	protected File lastLoadedFile;
	protected long lastLoadedFileTimestamp;

	public UserPreferencesBase(BaseMod mod, ModLogger log) {
		this.parent = mod;
		this.log = log;
		resetToDefaults();
	}

	public abstract void resetToDefaults();

	/**
	 * Load user preferences from file. If there is no file present, create it.
	 * @return true on success
	 */
	public boolean load(File userPreferencesFile) {
		resetToDefaults();
		try {
			InputStream userPrefs;
			try {
				userPrefs = new FileInputStream(userPreferencesFile);
			} catch (FileNotFoundException e) {
				log.info("creating user preferences file: " + userPreferencesFile);
				if (!save(userPreferencesFile)) {
					return false;
				}
				// For sanity's sake, don't return early after a successful save. Reload what we just saved.
				userPrefs = new FileInputStream(userPreferencesFile);
			}
			loadStream(userPrefs);
		} catch (Exception e) {
			log.severe(e, "failed to load preferences");
			return false;
		}
		lastLoadedFile = userPreferencesFile;
		lastLoadedFileTimestamp = lastLoadedFile.lastModified();
		return true;
	}

	public void loadStream(InputStream inStream) throws IOException {
		Properties props = new Properties();
		props.load(inStream);

		for (Enumeration m = props.keys(); m.hasMoreElements(); ) {
			String key = (String) m.nextElement();
			String value = props.getProperty(key);
			try {
				loadOne((String) key, value);
			} catch (Exception e) {
				log.warning("skipped option: " + key + "=" + value);
				log.warning("reason skipped: " + e.getMessage());
				log.throwing("UserPreferences", "loadStream", e);
			}
		}
	}

	protected abstract void loadOne(String key, String value);

	/**
	 * @return true if the file was modified externally and was reloaded
	 * @throws RuntimeException if there was no file specified by an earlier load() call
	 */
	public boolean reloadFileIfModified() {
		if (lastLoadedFile == null)
			throw new RuntimeException("attempting to reload file before it has been loaded");
		if (lastLoadedFile.lastModified() == lastLoadedFileTimestamp)
			return false;
		log.info("file change detected, reloading preferences");
		load(lastLoadedFile);
		return true;
	}

	/**
	 * @return true on success
	 * @throws RuntimeException if there was no file specified by an earlier load() call
	 */
	public boolean save() {
		if (lastLoadedFile == null)
			throw new RuntimeException("saving file before it has been loaded");
		return save(lastLoadedFile);
	}

	/** @return true on success */
	public boolean save(File userPreferencesFile) {
		try {
			saveStream(new FileOutputStream(userPreferencesFile));
			lastLoadedFile = userPreferencesFile;
			lastLoadedFileTimestamp = lastLoadedFile.lastModified();
			log.info("saved preferences to: " + userPreferencesFile);
			return true;
		} catch (IOException e) {
			log.severe(e, "failed to save preferences to: ");
			return false;
		}
	}

	public void saveStream(OutputStream outStream) throws IOException {
		Properties props = getPropertiesToSave();
		StringBuilder comments = new StringBuilder();
		addSaveComments(comments);
		props.store(outStream, comments.toString());
	}

	public abstract Properties getPropertiesToSave();

	public void addSaveComments(StringBuilder comments) {
		comments.append("Preferences for ").append(parent.getName()).append(" version ").append(parent.getVersion()).append('\n');
		comments.append("To restore all defaults, simply delete this file. It will be recreated the next time the mod loads.");
	}

	// ====
	// ==== Utility methods
	// ====

	public static boolean parseBooleanStrict(String value) {
		if (value.equalsIgnoreCase("true"))
			return true;
		if (value.equalsIgnoreCase("false"))
			return false;
		throw new IllegalArgumentException("expecting true or false, got: " + value);
	}

	public static String join(Collection items) {
		return join(items, ",");
	}
	public static String join(Collection items, String separator) {
		StringBuilder b = new StringBuilder();
		boolean first = true;
		for (Object item : items) {
			if (!first)
				b.append(separator);
			b.append(item);
			first = false;
		}
		return b.toString();
	}
}
