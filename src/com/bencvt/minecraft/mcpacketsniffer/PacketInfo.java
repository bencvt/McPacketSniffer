package com.bencvt.minecraft.mcpacketsniffer;

import java.util.HashMap;

import net.minecraft.src.*;

/**
 * Reference class listing all known packet types.
 * @see http://mc.kev009.com/Protocol
 * 
 * The names for the packets in the above page from the Minecraft Coalition wiki are generally
 * more accurate than the class names as deobfuscated by MCP. This isn't MCP's fault: it has a
 * (good!) policy in place that once an identifier has been deobfuscated, it is generally not
 * renamed, because that would break the build for every mod that references that identifier.
 * 
 * This reference class lists the two names for each packet side-by-side. It also provides a third,
 * shorthand name of each packet suitable for logging, usually based off the Minecraft Coalition
 * version.
 */
public class PacketInfo {
    public static final HashMap<Integer, PacketInfo> ALL_PACKETS = new HashMap();

    public int id;
    public String shortName;
    public String name;
    public Class packetClass;

    public PacketInfo(int id, String shortName, String name, Class packetClass) {
        this.id = id;
        this.shortName = shortName;
        if (shortName.length() > 12)
            throw new RuntimeException("invalid short name length: " + shortName);
        this.name = name;
        this.packetClass = packetClass;
        Class c = (Class) Packet.packetIdToClassMap.lookup(id);
        if (!packetClass.isAssignableFrom(c)) // not .isEqual(), because some mods (i.e. wecui) insert a proxy subclass
            throw new RuntimeException("packet class mismatch: " + id + ", " + packetClass + ", " + c);
    }

    public static String getPacketShortName(int id) {
        if (!ALL_PACKETS.containsKey(id)) {
            return null;
        }
        return ALL_PACKETS.get(id).shortName;
    }

    private static void add(int id, String shortName, String name, Class packetClass) {
        if (ALL_PACKETS.containsKey(id))
            throw new RuntimeException("duplicate definitions for id " + id);
        PacketInfo packetInfo = new PacketInfo(id, shortName.trim(), name, packetClass);
        ALL_PACKETS.put(id, packetInfo);
    }

    static {
        add(0x00, "keepalive   ", "Keep Alive", Packet0KeepAlive.class);
        add(0x01, "login req   ", "Login Request", Packet1Login.class);
        add(0x02, "handshake   ", "Handshake", Packet2ClientProtocol.class);
        add(0x03, "chat        ", "Chat Message", Packet3Chat.class);
        add(0x04, "time update ", "Time Update", Packet4UpdateTime.class);
        add(0x05, "ent equip   ", "Entity Equipment", Packet5PlayerInventory.class);
        add(0x06, "spawn pos   ", "Spawn Position", Packet6SpawnPosition.class);
        add(0x07, "use ent     ", "Use Entity", Packet7UseEntity.class);
        add(0x08, "health      ", "Update Health", Packet8UpdateHealth.class);
        add(0x09, "respawn     ", "Respawn", Packet9Respawn.class);
        add(0x0A, "ply         ", "Player", Packet10Flying.class);
        add(0x0B, "ply pos     ", "Player Position", Packet11PlayerPosition.class);
        add(0x0C, "ply look    ", "Player Look", Packet12PlayerLook.class);
        add(0x0D, "ply poslook ", "Player Position & Look", Packet13PlayerLookMove.class);
        add(0x0E, "ply dig     ", "Player Digging", Packet14BlockDig.class);
        add(0x0F, "ply block   ", "Player Block Placement", Packet15Place.class);
        add(0x10, "held item ch", "Held Item Change", Packet16BlockItemSwitch.class);
        add(0x11, "use bed     ", "Use Bed", Packet17Sleep.class);
        add(0x12, "ent animatn ", "Animation", Packet18Animation.class);
        add(0x13, "ent action  ", "Entity Action", Packet19EntityAction.class);
        add(0x14, "s named ent ", "Spawn Named Entity", Packet20NamedEntitySpawn.class);
        add(0x15, "s drop item ", "Spawn Dropped Item", Packet21PickupSpawn.class);
        add(0x16, "collect item", "Collect Item", Packet22Collect.class);
        add(0x17, "s obj veh   ", "Spawn Object/Vehicle", Packet23VehicleSpawn.class);
        add(0x18, "s mob       ", "Spawn Mob", Packet24MobSpawn.class);
        add(0x19, "s painting  ", "Spawn Painting", Packet25EntityPainting.class);
        add(0x1A, "s exp orb   ", "Spawn Experience Orb", Packet26EntityExpOrb.class);
        // (unassigned id 27)
        add(0x1C, "ent veloc   ", "Entity Velocity", Packet28EntityVelocity.class);
        add(0x1D, "d ent       ", "Destroy Entity", Packet29DestroyEntity.class);
        add(0x1E, "ent         ", "Entity", Packet30Entity.class);
        add(0x1F, "ent rel move", "Entity Relative Move", Packet31RelEntityMove.class);
        add(0x20, "ent look    ", "Entity Look", Packet32EntityLook.class);
        add(0x21, "ent lookmove", "Entity Look and Relative Move", Packet33RelEntityMoveLook.class);
        add(0x22, "ent tele    ", "Entity Teleport", Packet34EntityTeleport.class);
        add(0x23, "ent headlook", "Entity Head Look", Packet35EntityHeadRotation.class);
        // (unassigned ids 36-37)
        add(0x26, "ent status  ", "Entity Status", Packet38EntityStatus.class);
        add(0x27, "ent attach  ", "Attach Entity", Packet39AttachEntity.class);
        add(0x28, "ent metadata", "Entity Metadata", Packet40EntityMetadata.class);
        add(0x29, "ent a effect", "Entity Effect", Packet41EntityEffect.class);
        add(0x2A, "ent r effect", "Remove Entity Effect", Packet42RemoveEntityEffect.class);
        add(0x2B, "set exp     ", "Set Experience", Packet43Experience.class);
        // (unassigned ids 44-49)
        // (deprecated id 50, used to be "prechunk" packet)
        add(0x33, "chunk       ", "Map Chunks", Packet51MapChunk.class);
        add(0x34, "blk change m", "Multi Block Change", Packet52MultiBlockChange.class);
        add(0x35, "blk change 1", "Block Change", Packet53BlockChange.class);
        add(0x36, "blk action  ", "Block Action", Packet54PlayNoteBlock.class);
        add(0x37, "blk breaking", "Block Break Animation", Packet55BlockDestroy.class);
        add(0x38, "chunk bulk  ", "Map Chunk Bulk", Packet56MapChunks.class);
        // (unassigned ids 57-59)
        add(0x3C, "blk explode ", "Explosion", Packet60Explosion.class);
        add(0x3D, "blk soundfx ", "Sound/Particle Effect", Packet61DoorChange.class);
        add(0x3E, "blk namedfx", "Named Sound Effect", Packet62LevelSound.class);
        // (unassigned ids 63-69)
        add(0x46, "ch game st  ", "Change Game State", Packet70GameEvent.class);
        add(0x47, "s thunderb  ", "Thunderbolt", Packet71Weather.class);
        // (unassigned ids 72-99)
        add(0x64, "wnd open    ", "Open Window", Packet100OpenWindow.class);
        add(0x65, "wnd close   ", "Close Window", Packet101CloseWindow.class);
        add(0x66, "wnd click   ", "Click Window", Packet102WindowClick.class);
        add(0x67, "wnd slot    ", "Set Slot", Packet103SetSlot.class);
        add(0x68, "wnd items   ", "Set Window Items", Packet104WindowItems.class);
        add(0x69, "wnd prop    ", "Update Window Property", Packet105UpdateProgressbar.class);
        add(0x6A, "wnd confirm ", "Confirm Transaction", Packet106Transaction.class);
        add(0x6B, "crinv action", "Creative Inventory Action", Packet107CreativeSetSlot.class);
        add(0x6C, "wnd enchant ", "Enchant Item", Packet108EnchantItem.class);
        // (unassigned ids 109-129)
        add(0x82, "sign        ", "Update Sign", Packet130UpdateSign.class);
        add(0x83, "item data   ", "Item Data", Packet131MapData.class);
        add(0x84, "tile ent up ", "Update Tile Entity", Packet132TileEntityData.class);
        // (unassigned ids 133-199)
        add(0xC8, "stat inc    ", "Increment Statistic", Packet200Statistic.class);
        add(0xC9, "user list   ", "Player List Item", Packet201PlayerInfo.class);
        add(0xCA, "ply abils   ", "Player Abilities", Packet202PlayerAbilities.class);
        add(0xCB, "autocomplete", "Tab-complete", Packet203AutoComplete.class);
        add(0xCC, "client info ", "Locale and View Distance", Packet204ClientInfo.class);
        add(0xCD, "client spawn", "Client Statuses", Packet205ClientCommand.class);
        // (unassigned ids 206-249)
        add(0xFA, "plugin      ", "Plugin Message", Packet250CustomPayload.class);
        // (unassigned id 251)
        add(0xFC, "encrypt resp", "Encryption Key Response", Packet252SharedKey.class);
        add(0xFD, "encrypt req ", "Encryption Key Request", Packet253ServerAuthData.class);
        add(0xFE, "ping        ", "Server List Ping", Packet254ServerPing.class);
        add(0xFF, "kick        ", "Disconnect/Kick", Packet255KickDisconnect.class);
    }
}
