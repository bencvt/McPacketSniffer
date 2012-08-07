package bencvt.minecraft.client.mcpacketsniffer;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Utility class to translate integer/byte identifiers to human-readable strings.
 */
public class CodeTable {

    public static final CodeTable dimension = new CodeTable(
            "-1 nether", "0 overworld", "1 end");

    public static final CodeTable facing2D = new CodeTable(
            "0 -z,north", "1 -x,west", "2 +z,south", "3 +x,east");

    public static final CodeTable facing3D = new CodeTable(
            "0 -y,down", "1 +y,up", "2 -z,north", "3 +z,south", "4 -x,west", "5 +x,east");

    public static final CodeTable blockDigStatus = new CodeTable(
            "0 started digging", "1 DEPRECATED:digging", "2 finished digging", "3 DEPRECATED:block broken", "4 drop item", "5 shoot arrow or finish eating");

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
            "65 small fireball",
            "65 ender pearl",
            "70 falling block",
            "72 eye of ender",
            "73 splash potion",
            "75 exp bottle",
            "90 fish hook");

    /** @see RenderGlobal.playAuxSFX */
    public static final CodeTable soundOrParticleEffect = new CodeTable(
            "1000 sound: random.click",
            "1001 sound: random.click",
            "1002 sound: random.bow",
            "1003 sound: random.door_open or random.door_close",
            "1004 sound: random.fizz",
            "1005 sound: music disc",
            "1007 sound: mob.ghast.charge",
            "1008 sound: mob.ghast.fireball",
            "1010 sound: mob.zombie.wood",
            "1011 sound: mob.zombie.metal",
            "1012 sound: mob.zombie.woodbreak",
            "2000 smoke particles",
            "2001 block break",
            "2002 splash potion",
            "2003 eye of ender",
            "2004 mob spawn");

    public static final CodeTable entityMetadataType = new CodeTable(
            "0 byte", "1 short", "2 int", "3 float", "4 string16", "5 itemslot", "6 vector3");

    private HashMap<Integer, String> table = new HashMap<Integer, String>();
    public CodeTable(String ... codeList) {
        for (String code : codeList) {
            String[] parts = code.split(" ", 2);
            table.put(Integer.parseInt(parts[0]), parts[1]);
        }
    }
    public void log(StringBuilder line, int code) {
        log(line, code, true);
    }
    public void log(StringBuilder line, int code, boolean fullCode) {
        String value = table.get(code);
        if (fullCode || value == null) {
            line.append(code).append('(').append(value == null ? '?' : value).append(')');
        } else {
            line.append(value);
        }
        if (value == null) {
            String tableName = "unknown table";
            try {
                for (Field field : CodeTable.class.getDeclaredFields()) {
                    if (field.isAccessible() && field.get(null) == this) {
                        tableName = field.getName();
                        break;
                    }
                }
            } catch (Exception e) {
                // do nothing
            }
            LogManager.eventLog.warning("unrecognized code " + code + " for " + tableName);
        }
    }
}
