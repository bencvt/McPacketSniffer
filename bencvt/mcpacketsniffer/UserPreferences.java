package bencvt.mcpacketsniffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import bencvt.mcpacketsniffer.commons.UserPreferencesBase;

public class UserPreferences extends UserPreferencesBase {

	public boolean modEnabled;

	/** If true, don't include a timestamp in the packet/stat filenames, and just overwrite them each time. */
	public boolean outputMultiple;

	/** Packet types to not parse; they'll still be included in stats though. */
	public SortedSet<Integer> packetBlacklist;

	/** Whether to include the full uncompressed chunk data from packet 51(0x33) in the logs. */
	public boolean packet0x33LogAll;

	/** Whether to include the full item data (e.g., maps) from packet 131(0x83) in the logs. */
	public boolean packet0x83LogAll;

	/** If <= 0, do not write stats file. */
	public int statsWriteIntervalSeconds;

	public UserPreferences(McPacketSnifferMod mod) {
		super(mod, mod.log);
	}

	@Override
	public void resetToDefaults() {
		modEnabled = true;
		outputMultiple = true;
		packetBlacklist = new TreeSet();
		packet0x33LogAll = false;
		packet0x83LogAll = false;
		statsWriteIntervalSeconds = 60;
	}

	@Override
	protected void loadOne(String key, String value) {
		if (key.equals("modenabled")) {
			modEnabled = parseBooleanStrict(value);
		} else if (key.equals("output.multiple")) {
			outputMultiple = parseBooleanStrict(value);
		} else if (key.equals("packet.0x33.logall")) {
			packet0x33LogAll = parseBooleanStrict(value);
		} else if (key.equals("packet.0x83.logall")) {
			packet0x83LogAll = parseBooleanStrict(value);
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

	@Override
	public Properties getPropertiesToSave() {
		Properties props = new Properties();
		props.setProperty("modenabled", Boolean.toString(modEnabled));
		props.setProperty("output.multiple", Boolean.toString(outputMultiple));
		props.setProperty("packet.0x33.logall", Boolean.toString(packet0x33LogAll));
		props.setProperty("packet.0x83.logall", Boolean.toString(packet0x83LogAll));
		props.setProperty("packet.blacklist", join(packetBlacklist));
		props.setProperty("stats.writeintervalseconds", Integer.toString(statsWriteIntervalSeconds));
		return props;
	}
}
