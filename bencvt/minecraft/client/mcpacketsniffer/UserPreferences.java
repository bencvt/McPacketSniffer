package bencvt.minecraft.client.mcpacketsniffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import bencvt.minecraft.client.commons.UserPreferencesBase;

public class UserPreferences extends UserPreferencesBase {

	public boolean modEnabled;

	/** If true, escape color codes (e.g. "\247a") as ampersands ("&a"). */
	public boolean formatAmpersandColor;

	/** If true, don't include a timestamp in the packet/stat filenames, and just overwrite them each time. */
	public boolean outputMultiple;

	/**
	 * If non-empty, only these packet types will be parse/logged.
	 * Even with a whitelist, all packets will still be included in stats.
	 */
	public SortedSet<Integer> packetWhitelist;

	/** If true, flush the packet log after every packet. Probably a bad idea unless you're using a very small whitelist. */
	public boolean packetLogFlush;

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
		formatAmpersandColor = true;
		outputMultiple = true;
		packetWhitelist = new TreeSet();
		packetLogFlush = false;
		packet0x33LogAll = false;
		packet0x83LogAll = false;
		statsWriteIntervalSeconds = 60;
	}

	@Override
	protected void loadOne(String key, String value) {
		if (key.equals("modenabled")) {
			modEnabled = parseBooleanStrict(value);
		} else if (key.equals("format.ampersandcolor")) {
			formatAmpersandColor = parseBooleanStrict(value);
		} else if (key.equals("output.multiple")) {
			outputMultiple = parseBooleanStrict(value);
		} else if (key.equals("packet.0x33.logall")) {
			packet0x33LogAll = parseBooleanStrict(value);
		} else if (key.equals("packet.0x83.logall")) {
			packet0x83LogAll = parseBooleanStrict(value);
		} else if (key.equals("packet.logflush")) {
			packetLogFlush = parseBooleanStrict(value);
		} else if (key.equals("packet.whitelist")) {
			packetWhitelist.clear();
			for (String num : value.split(",")) {
				num = num.trim();
				if (!num.isEmpty()) {
					packetWhitelist.add(Integer.decode(num));
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
		props.setProperty("format.ampersandcolor", Boolean.toString(formatAmpersandColor));
		props.setProperty("output.multiple", Boolean.toString(outputMultiple));
		props.setProperty("packet.0x33.logall", Boolean.toString(packet0x33LogAll));
		props.setProperty("packet.0x83.logall", Boolean.toString(packet0x83LogAll));
		props.setProperty("packet.logflush", Boolean.toString(packetLogFlush));
		props.setProperty("packet.whitelist", join(packetWhitelist));
		props.setProperty("stats.writeintervalseconds", Integer.toString(statsWriteIntervalSeconds));
		return props;
	}
}
