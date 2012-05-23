package bencvt.mcpacketsniffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

/**
 * Load/save user preferences from a Java properties file.
 * 
 * @author bencvt
 */
public class UserPreferences {
	public boolean modEnabled = true;

	/** If true, don't include a timestamp in the packet/stat filenames, and just overwrite them each time. */
	public boolean outputMultiple = true;

	/** Packet types to not parse; they'll still be included in stats though. */
	public SortedSet<Integer> packetBlacklist = new TreeSet();

	/** Whether to include the full uncompressed chunk data from packet 51(0x33) in the logs. */
	public boolean packet0x33LogAll = false;

	/** Whether to include the full item data (e.g., maps) from packet 131(0x83) in the logs. */
	public boolean packet0x83LogAll = false;

	/** If <= 0, do not write stats file. */
	public int statsWriteIntervalSeconds = 60;

	private File lastLoadedFile;
	private long lastLoadedFileTimestamp;
	public boolean reloadFileIfModified() {
		if (lastLoadedFile.lastModified() == lastLoadedFileTimestamp)
			return false;
		Log.getLogger().info("file change detected, reloading preferences");
		load(lastLoadedFile);
		return true;
	}

	/**
	 * Load user preferences from file. If there is no file present, create it.
	 * @return true on success
	 */
	public boolean load(File userPreferencesFile) {
		try {
			InputStream userPrefs;
			try {
				userPrefs = new FileInputStream(userPreferencesFile);
			} catch (FileNotFoundException e) {
				Log.getLogger().info("creating user preferences file: " + userPreferencesFile);
				if (!save(userPreferencesFile)) {
					return false;
				}
				// For sanity's sake, don't return early after a successful save. Reload what we just saved.
				userPrefs = new FileInputStream(userPreferencesFile);
				lastLoadedFile = userPreferencesFile;
				lastLoadedFileTimestamp = lastLoadedFile.lastModified();
			}
			loadStream(userPrefs);
		} catch (Exception e) {
			Log.getLogger().log(Level.SEVERE, "failed to load preferences", e);
			return false;
		}
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
				Log.getLogger().warning("skipped option: " + key + "=" + value);
				Log.getLogger().warning("reason skipped: " + e.getMessage());
				Log.getLogger().log(Level.FINE, "stack trace", e);
			}
		}
	}

	private void loadOne(String key, String value) {
		assert key != null && value != null;
		if (key.equals("modenabled")) {
			modEnabled = Util.parseBooleanStrict(value);
		} else if (key.equals("output.multiple")) {
			outputMultiple = Util.parseBooleanStrict(value);
		} else if (key.equals("packet.0x33.logall")) {
			packet0x33LogAll = Util.parseBooleanStrict(value);
		} else if (key.equals("packet.0x83.logall")) {
			packet0x83LogAll = Util.parseBooleanStrict(value);
		} else if (key.equals("packet.blacklist")) {
			packetBlacklist.clear();
			for (String num : value.split(",")) {
				num = num.trim();
				if (!num.isEmpty()) {
					packetBlacklist.add(Integer.decode(num));
				}
			}
		} else if (key.equals("stats.writeintervalseconds")) {
			statsWriteIntervalSeconds = Integer.parseInt(value);
		}
		// ==== deprecated or invalid keys:
		else {
			throw new IllegalArgumentException("key " + key + " not recognized");
		}
	}

	// =====
	// ===== Saving
	// =====

	/**
	 * @return true on success
	 */
	public boolean save(File userPreferencesFile) {
		try {
			saveStream(new FileOutputStream(userPreferencesFile));
			Log.getLogger().info("saved preferences to: " + userPreferencesFile);
			return true;
		} catch (IOException e) {
			Log.getLogger().log(Level.SEVERE, "failed to save preferences to: " + userPreferencesFile, e);
			return false;
		}
	}

	public void saveStream(OutputStream outStream) throws IOException {
		Properties props = new Properties();

		props.setProperty("modenabled", Boolean.toString(modEnabled));
		props.setProperty("output.multiple", Boolean.toString(outputMultiple));
		props.setProperty("packet.0x33.logall", Boolean.toString(packet0x33LogAll));
		props.setProperty("packet.0x83.logall", Boolean.toString(packet0x83LogAll));
		props.setProperty("packet.blacklist", Util.join(packetBlacklist));
		props.setProperty("stats.writeintervalseconds", Integer.toString(statsWriteIntervalSeconds));

		props.store(outStream, new StringBuilder()
				.append("Preferences for ").append(McPacketSnifferMod.instance.getName()).append(" version ").append(McPacketSnifferMod.instance.getVersion()).append('\n')
				.append("To restore all defaults, simply delete this file. It will be recreated the next time the mod loads.")
				.toString());
	}
}
