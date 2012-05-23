package bencvt.mcpacketsniffer;

import java.util.Collection;

/**
 * Miscellaneous utility methods.
 * 
 * @author bencvt
 */
public abstract class Util {

	public static boolean parseBooleanStrict(String value) {
		if (value.equalsIgnoreCase("true"))
			return true;
		if (value.equalsIgnoreCase("false"))
			return false;
		throw new IllegalArgumentException("expecting true or false, got: " + value);
	}

	public static String join(Collection items) {
		StringBuilder b = new StringBuilder();
		boolean first = true;
		for (Object item : items) {
			if (!first)
				b.append(',');
			b.append(item);
			first = false;
		}
		return b.toString();
	}
}
