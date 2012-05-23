package bencvt.mcpacketsniffer.commons;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.src.BaseMod;

/**
 * Get the mod-specific subdirectory: [user's minecraft directory]/mods/[YourModName]/
 * 
 * [YourModName] is the result of BaseMod.getName(), minus the "mod_" prefix.
 * The directory is created if it wasn't there before.
 * 
 * @author bencvt
 * @version 5
 */
public class ModDirectory extends File {
	public ModDirectory(BaseMod mod) {
		this(getModShortName(mod), true);
	}

	public ModDirectory(String modName, boolean createIfNotExists) {
		super(Minecraft.getMinecraftDir().getPath() + File.separator + "mods" + File.separator + modName);
		if (createIfNotExists)
			createIfNotExists();
	}

	public boolean createIfNotExists() {
		if (exists())
			return false;
		mkdirs();
		if (!isDirectory())
			throw new RuntimeException("unable to init mod directory " + getAbsolutePath());
		return true;
	}

	public static String getModShortName(BaseMod mod) {
		String name = mod.getName();
		if (name.startsWith("mod_"))
			name = name.substring(4);
		return name;
	}
}
