package com.bencvt.minecraft.mcpacketsniffer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;

/**
 * Utility class to translate integer/byte identifiers to human-readable strings.
 */
public class CodeTable {

    public static final CodeTable dimension = new CodeTable(
            "-1 nether", "0 overworld", "1 end");

    public static final CodeTable facing2D = new CodeTable(
            "0 -z,north", "1 -x,west", "2 +z,south", "3 +x,east");

    public static final CodeTable facing3D = new CodeTable(
            "-1 n/a", "0 -y,down", "1 +y,up", "2 -z,north", "3 +z,south", "4 -x,west", "5 +x,east", "255 n/a");

    public static final CodeTable blockDigStatus = new CodeTable(
            "0 started digging", "1 cancelled digging", "2 finished digging", "3 check block", "4 drop item", "5 shoot arrow or finish eating");

    public static final CodeTable changeGameStateReason = new CodeTable(
            "0 invalid bed", "1 begin raining", "2 stop raining", "3 change game mode", "4 enter credits");

    public static final CodeTable clientStatus = new CodeTable(
            "0 initial spawn", "1 respawn after death");

    public static final CodeTable viewDistance = new CodeTable(
            "0 far", "1 normal", "2 short", "3 tiny");

    public static final CodeTable chatMode = new CodeTable(
            "0 enabled", "1 commands only", "2 hidden");

    public static final CodeTable difficulty = new CodeTable(
            "0 peaceful", "1 easy", "2 normal", "3 hard");

    public static final CodeTable entityAnimation = new CodeTable(
            "1 swing arm", "2 hurt", "3 leave bed", "5 DEPRECATED:eat food", "6 critical hit", "7 magic critical hit");

    public static final CodeTable entityAction = new CodeTable(
            "1 crouch", "2 uncrouch", "3 leave bed", "4 start sprinting", "5 stop sprinting");

    public static final CodeTable entityStatus = new CodeTable(
            "2 hurt",
            "3 dead",
            "4 iron golem attacking",
            "6 taming",
            "7 tamed",
            "8 wolf shaking off water",
            "9 player done eating",
            "10 sheep eating grass",
            "11 iron golem offering rose",
            "12 villager looking for mate");

    /** @see net.minecraft.src.NetClientHandler#handleVehicleSpawn */
    public static final CodeTable entitySpawnObject = new CodeTable(
            "1 boat",
            "10 minecart",
            "11 storage minecart",
            "12 powered minecart",
            "50 primed tnt",
            "51 ender crystal",
            "60 arrow",
            "61 snowball",
            "62 chicken egg",
            "63 fireball",
            "64 small fireball",
            "65 ender pearl",
            "66 wither skull",
            "70 falling block",
            "72 eye of ender",
            "73 splash potion",
            "75 exp bottle",
            "90 fish hook");

    /** @see net.minecraft.src.RenderGlobal#playAuxSFX */
    public static final CodeTable soundOrParticleEffect = new CodeTable(
            "1000 sound: random.click",
            "1001 sound: random.click",
            "1002 sound: random.bow",
            "1003 sound: random.door_open or random.door_close",
            "1004 sound: random.fizz",
            "1005 sound: music disc",
            // no 1006
            "1007 sound: mob.ghast.charge",
            "1008 sound: mob.ghast.fireball",
            "1009 sound: mob.ghast.fireball",
            "1010 sound: mob.zombie.wood",
            "1011 sound: mob.zombie.metal",
            "1012 sound: mob.zombie.woodbreak",
            // no 1013
            "1014 sound: mob.wither.shoot",
            "1015 sound: mob.bat.takeoff",
            "1016 sound: mob.zombie.infect",
            "1017 sound: mob.zombie.unfect",
            // no 1018 or 1019
            "1020 sound: random.anvil_break",
            "1021 sound: random.anvil_use",
            "1022 sound: random.anvil_land",
            "2000 smoke particles",
            "2001 block break",
            "2002 splash potion",
            "2003 eye of ender",
            "2004 mob spawn");

    public static final CodeTable entityMetadataType = new CodeTable(
            "0 byte", "1 short", "2 int", "3 float", "4 string16", "5 itemslot", "6 vector3");

    // Populate the name field for every table.
    static {
        try {
            for (Field field : CodeTable.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    ((CodeTable) field.get(null)).tableName = field.getName();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("internal reflection exception", e);
        }
    }

    private final HashMap<Integer, String> table = new HashMap<Integer, String>();
    private final HashSet<Integer> missingCodes = new HashSet<Integer>();
    private String tableName;

    private CodeTable(String ... codeList) {
        for (String code : codeList) {
            String[] parts = code.split(" ", 2);
            table.put(Integer.parseInt(parts[0]), parts[1]);
        }
    }

    public void log(StringBuilder line, int code) {
        log(line, code, true);
    }

    /**
     * @param line
     * @param code
     * @param fullCode if true output, e.g., "61(snowball)".
     *                 If false, just "snowball".
     */
    public void log(StringBuilder line, int code, boolean fullCode) {
        String value = table.get(code);
        if (fullCode || value == null) {
            line.append(code).append('(').append(value == null ? '?' : value).append(')');
        } else {
            line.append(value);
        }
        if (value == null && Controller.getOptions().logMissingCodes && !missingCodes.contains(code)) {
            missingCodes.add(code);
            Controller.getEventLog().log(Level.WARNING,
                    "unrecognized code " + code + " for " + tableName,
                    new Exception());
        }
    }
}
