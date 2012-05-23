package bencvt.mcpacketsniffer;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.src.*;

/**
 * Log packets.
 * 
 * This class is abstract, though it implements everything needed.
 * ModLoader works by looking for specifically named classes in the main package.
 * 
 * @see net.minecraft.src.mod_McPacketSniffer
 * @author bencvt
 */
public abstract class McPacketSnifferMod extends BaseMod {
	public static final int PACKET_HEADER_SIZE = 1; // byte packetId

	public static McPacketSnifferMod instance;
	private UserPreferences preferences = new UserPreferences();
	private boolean modEnabled = true;
	private Minecraft minecraft;
	private File modDirectory;
	private File statsFile;
	private PrintWriter packetLog;
	private boolean forceFlush;
	private long started;
	private long lastSaveStats;
	private long lastPreferencesCheck;
	private long[] sentPacketCounts = new long[256];
	private long[] sentPacketBytes = new long[256];
	private long[] receivePacketCounts = new long[256];
	private long[] receivePacketBytes = new long[256];

	public McPacketSnifferMod() {
		if (instance != null)
			throw new RuntimeException("multiple instances of singleton");
		instance = this;
		getPacketShortName(0); // force class loader to load PacketInfo so it can do its sanity checking right away
	}

	@Override
	public String getVersion() {
		return "1.0 [1.2.5]";
	}

	@Override
	public void load() {
		modDirectory = Log.initForMod(this, "mcpacketsniffer");
		preferences.load(new File(modDirectory, "options.txt"));
		modEnabled = preferences.modEnabled;
		ModLoader.setInGameHook(this, true, true);
	}

	@Override
	public boolean onTickInGame(float f, Minecraft minecraft) {
		if (!modEnabled)
			return false;
		this.minecraft = minecraft;
		// just use in-game ticks for timing; no need to add a thread if we don't have to
		long now = System.currentTimeMillis();
		if (preferences.statsWriteIntervalSeconds >= 0 &&
				lastSaveStats != 0L && now >= lastSaveStats + preferences.statsWriteIntervalSeconds*1000L) {
			lastSaveStats = now;
			saveStats();
		}
		if (now >= lastPreferencesCheck + 10000L) {
			lastPreferencesCheck = now;
			preferences.reloadFileIfModified();
		}
		return true;
	}

	private boolean startPacketLog() {
		long now = System.currentTimeMillis();
		String suffix = "";
		if (preferences.outputMultiple) {
			// e.g., "_20120518_133948"
			suffix = "_" + timestampToString(now).replace(' ', '_').replaceAll("[:\\-]", "").substring(0, 15);
		}

		File packetLogFile = new File(modDirectory, "packets" + suffix + ".txt");
		try {
			packetLog = new PrintWriter(new BufferedWriter(new FileWriter(packetLogFile)));
		} catch (IOException e) {
			Log.getLogger().log(Level.SEVERE, "unable to open packet log file for writing: " + packetLogFile, e);
			modEnabled = false;
			return false;
		}

		statsFile = new File(modDirectory, "stats" + suffix + ".txt");
		started = now;
		lastSaveStats = now;
		return true;
	}

	// called from GuiMultiplayer
	public synchronized void onPacketThreadSafe(Packet packet, boolean isSend) {
		onPacket(packet, isSend);
	}

	// called from NetworkManager
	public void onPacket(Packet packet, boolean isSend) {
		if (!modEnabled)
			return;

		if (isSend) {
			sentPacketCounts[packet.getPacketId()] += 1;
			sentPacketBytes[packet.getPacketId()] += PACKET_HEADER_SIZE + getPacketPayloadSize(packet);
		} else {
			receivePacketCounts[packet.getPacketId()] += 1;
			receivePacketBytes[packet.getPacketId()] += PACKET_HEADER_SIZE + getPacketPayloadSize(packet);
		}

		if (packetLog == null || !startPacketLog())
			return;

		if (preferences.packetBlacklist.contains(packet.getPacketId()))
			return;

		StringBuilder line = new StringBuilder(160);
		dumpTimestamp(line, System.currentTimeMillis());
		line.append(' ');
		dumpPacket(line, packet, isSend);
		packetLog.println(line.toString());
		if (forceFlush) {
			forceFlush = false;
			packetLog.flush();
		}
	}

	/**
	 * Output a timestamp, e.g., "2012-05-18 13:39:48.980"
	 */
	private static void dumpTimestamp(StringBuilder line, long timestamp) {
		String ts = new java.sql.Timestamp(timestamp).toString();
		line.append(ts);
		for (int pad = ts.length(); pad < 23; pad++)
			line.append('0');
	}
	private static String timestampToString(long timestamp) {
		StringBuilder b = new StringBuilder();
		dumpTimestamp(b, timestamp);
		return b.toString();
	}

	private void dumpPacket(StringBuilder line, Packet packet, boolean isSend) {
		line.append(isSend ? "C2S" : "S2C").append(' ');
		String packetName = getPacketShortName(packet.getPacketId());
		if (packetName == null)
			packetName = "unrecognized packet#" + packet.getPacketId();
		line.append(packetName);
		for (int pad = packetName.length(); pad < 13; pad++)
			line.append(' ');

		switch (packet.getPacketId()) {
		case 0x00: dumpPacket0x00KeepAlive(line, (Packet0KeepAlive) packet); break;
		case 0x01: dumpPacket0x01Login(line, (Packet1Login) packet, isSend); break;
		case 0x02: dumpPacket0x02Handshake(line, (Packet2Handshake) packet, isSend); break;
		case 0x03: dumpPacket0x03Chat(line, (Packet3Chat) packet); break;
		case 0x04: dumpPacket0x04UpdateTime(line, (Packet4UpdateTime) packet); break;
		case 0x05: dumpPacket0x05PlayerInventory(line, (Packet5PlayerInventory) packet); break;
		case 0x06: dumpPacket0x06SpawnPosition(line, (Packet6SpawnPosition) packet); break;
		case 0x07: dumpPacket0x07UseEntity(line, (Packet7UseEntity) packet); break;
		case 0x08: dumpPacket0x08UpdateHealth(line, (Packet8UpdateHealth) packet); break;
		case 0x09: dumpPacket0x09Respawn(line, (Packet9Respawn) packet); break;
		case 0x0A: dumpPacket0x0AFlying(line, (Packet10Flying) packet); break;
		case 0x0B: dumpPacket0x0BPlayerPosition(line, (Packet11PlayerPosition) packet); break;
		case 0x0C: dumpPacket0x0CPlayerLook(line, (Packet12PlayerLook) packet); break;
		case 0x0D: dumpPacket0x0DPlayerLookMove(line, (Packet13PlayerLookMove) packet, isSend); break;
		case 0x0E: dumpPacket0x0EBlockDig(line, (Packet14BlockDig) packet); break;
		case 0x0F: dumpPacket0x0FPlace(line, (Packet15Place) packet); break;
		case 0x10: dumpPacket0x10BlockItemSwitch(line, (Packet16BlockItemSwitch) packet); break;
		case 0x11: dumpPacket0x11Sleep(line, (Packet17Sleep) packet); break;
		case 0x12: dumpPacket0x12Animation(line, (Packet18Animation) packet); break;
		case 0x13: dumpPacket0x13EntityAction(line, (Packet19EntityAction) packet); break;
		case 0x14: dumpPacket0x14NamedEntitySpawn(line, (Packet20NamedEntitySpawn) packet); break;
		case 0x15: dumpPacket0x15PickupSpawn(line, (Packet21PickupSpawn) packet); break;
		case 0x16: dumpPacket0x16Collect(line, (Packet22Collect) packet); break;
		case 0x17: dumpPacket0x17VehicleSpawn(line, (Packet23VehicleSpawn) packet); break;
		case 0x18: dumpPacket0x18MobSpawn(line, (Packet24MobSpawn) packet); break;
		case 0x19: dumpPacket0x19EntityPainting(line, (Packet25EntityPainting) packet); break;
		case 0x1A: dumpPacket0x1AEntityExpOrb(line, (Packet26EntityExpOrb) packet); break;
		case 0x1C: dumpPacket0x1CEntityVelocity(line, (Packet28EntityVelocity) packet); break;
		case 0x1D: dumpPacket0x1DDestroyEntity(line, (Packet29DestroyEntity) packet); break;
		case 0x1E: dumpPacket0x1EEntity(line, (Packet30Entity) packet); break;
		case 0x1F: dumpPacket0x1FRelEntityMove(line, (Packet31RelEntityMove) packet); break;
		case 0x20: dumpPacket0x20EntityLook(line, (Packet32EntityLook) packet); break;
		case 0x21: dumpPacket0x21RelEntityMoveLook(line, (Packet33RelEntityMoveLook) packet); break;
		case 0x22: dumpPacket0x22EntityTeleport(line, (Packet34EntityTeleport) packet); break;
		case 0x23: dumpPacket0x23EntityHeadRotation(line, (Packet35EntityHeadRotation) packet); break;
		case 0x26: dumpPacket0x26EntityStatus(line, (Packet38EntityStatus) packet); break;
		case 0x27: dumpPacket0x27AttachEntity(line, (Packet39AttachEntity) packet); break;
		case 0x28: dumpPacket0x28EntityMetadata(line, (Packet40EntityMetadata) packet); break;
		case 0x29: dumpPacket0x29EntityEffect(line, (Packet41EntityEffect) packet); break;
		case 0x2A: dumpPacket0x2ARemoveEntityEffect(line, (Packet42RemoveEntityEffect) packet); break;
		case 0x2B: dumpPacket0x2BExperience(line, (Packet43Experience) packet); break;
		case 0x32: dumpPacket0x32PreChunk(line, (Packet50PreChunk) packet); break;
		case 0x33: dumpPacket0x33MapChunk(line, (Packet51MapChunk) packet); break;
		case 0x34: dumpPacket0x34MultiBlockChange(line, (Packet52MultiBlockChange) packet); break;
		case 0x35: dumpPacket0x35BlockChange(line, (Packet53BlockChange) packet); break;
		case 0x36: dumpPacket0x36PlayNoteBlock(line, (Packet54PlayNoteBlock) packet); break;
		case 0x3C: dumpPacket0x3CExplosion(line, (Packet60Explosion) packet); break;
		case 0x3D: dumpPacket0x3DDoorChange(line, (Packet61DoorChange) packet); break;
		case 0x46: dumpPacket0x46Bed(line, (Packet70Bed) packet); break;
		case 0x47: dumpPacket0x47Weather(line, (Packet71Weather) packet); break;
		case 0x64: dumpPacket0x64OpenWindow(line, (Packet100OpenWindow) packet); break;
		case 0x65: dumpPacket0x65CloseWindow(line, (Packet101CloseWindow) packet); break;
		case 0x66: dumpPacket0x66WindowClick(line, (Packet102WindowClick) packet); break;
		case 0x67: dumpPacket0x67SetSlot(line, (Packet103SetSlot) packet); break;
		case 0x68: dumpPacket0x68WindowItems(line, (Packet104WindowItems) packet); break;
		case 0x69: dumpPacket0x69UpdateProgressbar(line, (Packet105UpdateProgressbar) packet); break;
		case 0x6A: dumpPacket0x6ATransaction(line, (Packet106Transaction) packet); break;
		case 0x6B: dumpPacket0x6BCreativeSetSlot(line, (Packet107CreativeSetSlot) packet); break;
		case 0x6C: dumpPacket0x6CEnchantItem(line, (Packet108EnchantItem) packet); break;
		case 0x82: dumpPacket0x82UpdateSign(line, (Packet130UpdateSign) packet); break;
		case 0x83: dumpPacket0x83MapData(line, (Packet131MapData) packet); break;
		case 0x84: dumpPacket0x84TileEntityData(line, (Packet132TileEntityData) packet); break;
		case 0xC8: dumpPacket0xC8Statistic(line, (Packet200Statistic) packet); break;
		case 0xC9: dumpPacket0xC9PlayerInfo(line, (Packet201PlayerInfo) packet); break;
		case 0xCA: dumpPacket0xCAPlayerAbilities(line, (Packet202PlayerAbilities) packet); break;
		case 0xFA: dumpPacket0xFACustomPayload(line, (Packet250CustomPayload) packet); break;
		case 0xFE: dumpPacket0xFEServerPing(line, (Packet254ServerPing) packet); break;
		case 0xFF: dumpPacket0xFFKickDisconnect(line, (Packet255KickDisconnect) packet); break;
		default:
			Log.getLogger().severe("unhandled packet " + packetName + ", class=" + packet.getClass());
			line.append(" size~=").append(getPacketPayloadSize(packet));
			// we don't have access to the actual payload
			break;
		}
	}

	private void dumpPacket0x00KeepAlive(StringBuilder line, Packet0KeepAlive packet) {
		line.append("id=").append(packet.randomId);
	}

	private void dumpPacket0x01Login(StringBuilder line, Packet1Login packet, boolean isSend) {
		if (isSend) {
			line.append("protocol=").append(packet.protocolVersion);
			line.append(" username=").append(packet.username);
		} else {
			line.append("eid=").append(packet.protocolVersion);
			line.append(" leveltype=");
			dumpWorldType(line, packet.terrainType);
			line.append(" creative=").append(packet.serverMode);
			line.append(" dimension=").append(packet.field_48170_e);
			line.append(" difficulty=").append(packet.difficultySetting);
			line.append(" worldheight=").append(packet.worldHeight); // always 0; ignored by client
			line.append(" maxplayers=").append(packet.maxPlayers);
		}
	}

	private void dumpPacket0x02Handshake(StringBuilder line, Packet2Handshake packet, boolean isSend) {
		if (isSend) {
			line.append("usernamehost=").append(packet.username);
		} else {
			line.append("connectionhash=").append(packet.username);
		}
	}

	private void dumpPacket0x03Chat(StringBuilder line, Packet3Chat packet) {
		dumpPrintable(line, packet.message);
	}

	private void dumpPacket0x04UpdateTime(StringBuilder line, Packet4UpdateTime packet) {
		line.append("time=").append(packet.time);
	}

	private void dumpPacket0x05PlayerInventory(StringBuilder line, Packet5PlayerInventory packet) {
		dumpExistingEntity(line, packet.entityID);
		line.append(" slot=").append(packet.slot);
		line.append(" item=");
		dumpItemType(line, packet.itemID);
		line.append(" damage=").append(packet.itemDamage);
	}

	private void dumpPacket0x06SpawnPosition(StringBuilder line, Packet6SpawnPosition packet) {
		dumpCoordsBlockXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
	}

	private void dumpPacket0x07UseEntity(StringBuilder line, Packet7UseEntity packet) {
		line.append("player=").append(packet.playerEntityId); // don't dump the entire entity
		line.append(" target=");
		dumpExistingEntity(line, packet.targetEntity);
		line.append(" leftclick=").append(packet.isLeftClick);
	}

	private void dumpPacket0x08UpdateHealth(StringBuilder line, Packet8UpdateHealth packet) {
		line.append("health=").append(packet.healthMP);
		line.append(" food=").append(packet.food);
		line.append(" foodsaturation=").append(packet.foodSaturation);
	}

	private void dumpPacket0x09Respawn(StringBuilder line, Packet9Respawn packet) {
		line.append("dimension=").append(packet.respawnDimension);
		line.append(" difficulty=").append(packet.difficulty);
		line.append(" creative=").append(packet.creativeMode);
		line.append(" worldheight=").append(packet.worldHeight);
		line.append(" leveltype=");
		dumpWorldType(line, packet.terrainType);
	}

	private void dumpPacket0x0AFlying(StringBuilder line, Packet10Flying packet) {
		line.append("onground=").append(packet.onGround);
	}

	private void dumpPacket0x0BPlayerPosition(StringBuilder line, Packet11PlayerPosition packet) {
		dumpCoordsBlockXYZ(line, (int) packet.xPosition, (int) packet.yPosition, (int) packet.zPosition);
		line.append(" onground=").append(packet.onGround);
		line.append(" stance=").append(String.format("%.1f", packet.stance));
	}

	private void dumpPacket0x0CPlayerLook(StringBuilder line, Packet12PlayerLook packet) {
		line.append("onground=").append(packet.onGround);
		line.append(" yawpitch=");
		dumpAngleFloat(line, packet.yaw, packet.pitch);
	}

	private void dumpPacket0x0DPlayerLookMove(StringBuilder line, Packet13PlayerLookMove packet, boolean isSend) {
		double realY = packet.yPosition;
		double realStance = packet.stance;
		if (!isSend) {
			// You done goofed, Notch.
			double tmp = realY;
			realY = realStance;
			realStance = tmp;
		}
		dumpCoordsBlockXYZ(line, (int) packet.xPosition, (int) realY, (int) packet.zPosition);
		line.append(" onground=").append(packet.onGround);
		line.append(" stance=").append(String.format("%.1f", realStance));
		line.append(" yawpitch=");
		dumpAngleFloat(line, packet.yaw, packet.pitch);
	}

	private void dumpPacket0x0EBlockDig(StringBuilder line, Packet14BlockDig packet) {
		// TODO: Status code 4 (drop item) is a special case. In-game, when you use the Drop Item
		// command (keypress 'q'), a dig packet with a status of 4, and all other values set to 0,
		// is sent from client to server.
		dumpCoordsBlockXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
		line.append(" status=").append(packet.status);
		line.append(" face=");
		dumpFacingDirection3D(line, packet.face);
	}

	private void dumpPacket0x0FPlace(StringBuilder line, Packet15Place packet) {
		// TODO: This packet has a special case where X, Y, Z, and Direction are all -1.
		// This special packet indicates that the currently held item for the player
		// should have its state updated such as eating food, shooting bows, using buckets, etc.
		dumpCoordsBlockXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
		line.append(" direction=");
		dumpFacingDirection3D(line, packet.direction);
		line.append(" helditem=");
		dumpItemStack(line, packet.itemStack);
	}

	private void dumpPacket0x10BlockItemSwitch(StringBuilder line, Packet16BlockItemSwitch packet) {
		line.append("slotid=").append(packet.id);
	}

	private void dumpPacket0x11Sleep(StringBuilder line, Packet17Sleep packet) {
		dumpCoordsBlockXYZ(line, packet.bedX, packet.bedY, packet.bedZ);
		line.append(" zero=").append(packet.field_22046_e); // must be 0
		line.append(" entity=");
		dumpExistingEntity(line, packet.entityID);
	}

	private void dumpPacket0x12Animation(StringBuilder line, Packet18Animation packet) {
		dumpExistingEntity(line, packet.entityId);
		line.append(" animation=").append(packet.animate);
	}

	private void dumpPacket0x13EntityAction(StringBuilder line, Packet19EntityAction packet) {
		dumpExistingEntity(line, packet.entityId);
		line.append(" actionid=").append(packet.state);
	}

	private void dumpPacket0x14NamedEntitySpawn(StringBuilder line, Packet20NamedEntitySpawn packet) {
		dumpCoordsAbsoluteIntegerXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
		line.append(" eid=").append(packet.entityId);
		line.append(" name=").append(packet.name);
		line.append(" yawpitch=");
		dumpAngleByte(line, packet.rotation, packet.pitch);
		line.append(" helditem=");
		dumpItemType(line, packet.currentItem);
	}

	private void dumpPacket0x15PickupSpawn(StringBuilder line, Packet21PickupSpawn packet) {
		dumpCoordsAbsoluteIntegerXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
		line.append(" eid=").append(packet.entityId);
		line.append(" item=");
		dumpItemType(line, packet.itemID);
		line.append(" count=").append(packet.count);
		line.append(" damage=").append(packet.itemDamage);
		line.append(" yawpitch=");
		dumpAngleByte(line, packet.rotation, packet.pitch);
		line.append(" roll=");
		dumpAngleByte(line, packet.roll);
	}

	private void dumpPacket0x16Collect(StringBuilder line, Packet22Collect packet) {
		dumpExistingEntity(line, packet.collectedEntityId);
		line.append(" collectedby=");
		dumpExistingEntity(line, packet.collectorEntityId);
	}

	private void dumpPacket0x17VehicleSpawn(StringBuilder line, Packet23VehicleSpawn packet) {
		dumpCoordsAbsoluteIntegerXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
		line.append(" eid=").append(packet.entityId);
		line.append(" type=").append(packet.type); // ids are hard-coded in NetClientHandler
		line.append(" thrower=");
		if (packet.throwerEntityId <= 0) {
			line.append(packet.throwerEntityId);
		} else {
			dumpExistingEntity(line, packet.throwerEntityId);
			line.append(" speedx=").append(packet.speedX);
			line.append(" speedy=").append(packet.speedY);
			line.append(" speedz=").append(packet.speedZ);
		}
	}

	private void dumpPacket0x18MobSpawn(StringBuilder line, Packet24MobSpawn packet) {
		dumpCoordsAbsoluteIntegerXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
		line.append(" eid=").append(packet.entityId);
		line.append(" type=");
		dumpEntityType(line, packet.type);
		line.append(" yawpitch=");
		dumpAngleByte(line, packet.yaw, packet.pitch);
		line.append(" headyaw=");
		dumpAngleByte(line, packet.field_48169_h);
		line.append(" metadata=");
		dumpEntityMetadata(line, packet.getMetadata());
	}

	private void dumpPacket0x19EntityPainting(StringBuilder line, Packet25EntityPainting packet) {
		dumpCoordsBlockXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
		line.append(" eid=").append(packet.entityId);
		line.append(" dir=");
		dumpFacingDirection2D(line, packet.direction);
		line.append(" title=").append(packet.title);
	}

	private void dumpPacket0x1AEntityExpOrb(StringBuilder line, Packet26EntityExpOrb packet) {
		dumpCoordsAbsoluteIntegerXYZ(line, packet.posX, packet.posY, packet.posZ);
		line.append(" eid=").append(packet.entityId);
		line.append(" xpvalue=").append(packet.xpValue);
	}

	private void dumpPacket0x1CEntityVelocity(StringBuilder line, Packet28EntityVelocity packet) {
		dumpExistingEntity(line, packet.entityId);
		line.append(" motionx=").append(packet.motionX);
		line.append(" motiony=").append(packet.motionY);
		line.append(" motionz=").append(packet.motionZ);
	}

	private void dumpPacket0x1DDestroyEntity(StringBuilder line, Packet29DestroyEntity packet) {
		dumpExistingEntity(line, packet.entityId);
	}

	private void dumpPacket0x1EEntity(StringBuilder line, Packet30Entity packet) {
		dumpExistingEntity(line, packet.entityId);
	}

	private void dumpPacket0x1FRelEntityMove(StringBuilder line, Packet31RelEntityMove packet) {
		dumpExistingEntity(line, packet.entityId);
		line.append(" relx=").append(packet.xPosition);
		line.append('(').append(String.format("%.2f", (float)packet.xPosition / 32.0)).append(')');
		line.append(" rely=").append(packet.yPosition);
		line.append('(').append(String.format("%.2f", (float)packet.yPosition / 32.0)).append(')');
		line.append(" relz=").append(packet.zPosition);
		line.append('(').append(String.format("%.2f", (float)packet.zPosition / 32.0)).append(')');
	}

	private void dumpPacket0x20EntityLook(StringBuilder line, Packet32EntityLook packet) {
		dumpExistingEntity(line, packet.entityId);
		line.append(" yawpitch=");
		dumpAngleByte(line, packet.yaw, packet.pitch);
	}

	private void dumpPacket0x21RelEntityMoveLook(StringBuilder line, Packet33RelEntityMoveLook packet) {
		dumpExistingEntity(line, packet.entityId);
		// the number in parentheses is the number of blocks
		line.append(" relx=").append(String.format("%.5f", (double)packet.xPosition / 32.0));
		line.append(" rely=").append(String.format("%.5f", (double)packet.yPosition / 32.0));
		line.append(" relz=").append(String.format("%.5f", (double)packet.zPosition / 32.0));
		line.append(" yawpitch=");
		dumpAngleByte(line, packet.yaw, packet.pitch);
	}

	private void dumpPacket0x22EntityTeleport(StringBuilder line, Packet34EntityTeleport packet) {
		dumpExistingEntity(line, packet.entityId);
		line.append(" teleport=");
		dumpCoordsAbsoluteIntegerXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
		line.append(" yawpitch=");
		dumpAngleByte(line, packet.yaw, packet.pitch);
	}

	private void dumpPacket0x23EntityHeadRotation(StringBuilder line, Packet35EntityHeadRotation packet) {
		dumpExistingEntity(line, packet.entityId);
		line.append(" headyaw=");
		dumpAngleByte(line, packet.headRotationYaw);
	}

	private void dumpPacket0x26EntityStatus(StringBuilder line, Packet38EntityStatus packet) {
		dumpExistingEntity(line, packet.entityId);
		line.append(" status=").append(packet.entityStatus);
	}

	private void dumpPacket0x27AttachEntity(StringBuilder line, Packet39AttachEntity packet) {
		dumpExistingEntity(line, packet.entityId);
		line.append(" vehicle=");
		dumpExistingEntity(line, packet.vehicleEntityId);
	}

	private void dumpPacket0x28EntityMetadata(StringBuilder line, Packet40EntityMetadata packet) {
		dumpExistingEntity(line, packet.entityId);
		line.append(" metadata=");
		dumpEntityMetadata(line, packet.getMetadata());
	}

	private void dumpPacket0x29EntityEffect(StringBuilder line, Packet41EntityEffect packet) {
		dumpExistingEntity(line, packet.entityId);
		line.append(" effectid=").append(packet.effectId);
		line.append(" amplifier=").append(packet.effectAmplifier);
		line.append(" duration=").append(packet.duration);
	}

	private void dumpPacket0x2ARemoveEntityEffect(StringBuilder line, Packet42RemoveEntityEffect packet) {
		dumpExistingEntity(line, packet.entityId);
		line.append(" effectid=").append(packet.effectId);
	}

	private void dumpPacket0x2BExperience(StringBuilder line, Packet43Experience packet) {
		line.append("expbar=").append(String.format("%.3f", packet.experience));
		line.append(" level=").append(packet.experienceLevel);
		line.append(" total=").append(packet.experienceTotal);
	}

	private void dumpPacket0x32PreChunk(StringBuilder line, Packet50PreChunk packet) {
		dumpCoordsChunkXZ(line, packet.xPosition, packet.yPosition);
		line.append(" init=").append(packet.mode);
	}

	private void dumpPacket0x33MapChunk(StringBuilder line, Packet51MapChunk packet) {
		dumpCoordsChunkXZ(line, packet.xCh, packet.zCh);
		line.append(" init=").append(packet.includeInitialize);
		line.append(" ymin=").append(packet.yChMin);
		line.append(" ymax=").append(packet.yChMax);
		line.append(" compressedsize=").append(packet.getPacketSize() - 17); // HACK: because tempLength is private
		// there's another private int field as well, which the client ignores
		line.append(" chunkdata=");
		if (preferences.packet0x33LogAll) {
			dumpHex(line, packet.chunkData);
		} else {
			line.append(packet.chunkData.length).append(" bytes");
		}
	}

	private void dumpPacket0x34MultiBlockChange(StringBuilder line, Packet52MultiBlockChange packet) {
		dumpCoordsChunkXZ(line, packet.xPosition, packet.zPosition);
		line.append(" numblocks=").append(packet.size);
		line.append(" blocks=[");
		boolean first = true;
		// the following code is adapted from NetClientHandler
		int xBase = packet.xPosition << 4;
		int zBase = packet.zPosition << 4;
		DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(packet.metadataArray));
		try {
	        for (int k = 0; k < packet.size; k++)
	        {
	            short wordPos = dataStream.readShort();
	            short wordBlock = dataStream.readShort();
	            int xOff = (wordPos >> 12) & 0xf;
	            int zOff = (wordPos >> 8) & 0xf;
	            int yAbs = wordPos & 0xff;
	            int blockId = (wordBlock & 0xfff) >> 4;
	            int blockMetadata = wordBlock & 0xf;

	    		if (!first)
	    			line.append(' ');
	    		first = false;
	            line.append('(').append(xBase + xOff);
	            line.append(',').append(yAbs);
	            line.append(',').append(zBase + zOff);
	            line.append("):");
	            line.append(blockId);
	            line.append('d');
	            line.append(blockMetadata);
	        }
		} catch (IOException e) {
			// do nothing
		}
		line.append(']');
	}

	private void dumpPacket0x35BlockChange(StringBuilder line, Packet53BlockChange packet) {
		dumpCoordsBlockXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
		line.append(" block=");
        line.append(packet.type);
        line.append('d');
        line.append(packet.metadata);
	}

	private void dumpPacket0x36PlayNoteBlock(StringBuilder line, Packet54PlayNoteBlock packet) {
		dumpCoordsBlockXYZ(line, packet.xLocation, packet.yLocation, packet.zLocation);
		// @see http://mc.kev009.com/Block_Actions
		line.append(" byte1=").append(packet.instrumentType);
		line.append(" byte2=").append(packet.pitch);
	}

	private void dumpPacket0x3CExplosion(StringBuilder line, Packet60Explosion packet) {
		dumpCoordsBlockXYZ(line, (int) packet.explosionX, (int) packet.explosionY, (int) packet.explosionZ);
		line.append(" radius=").append(String.format("%.1f", packet.explosionSize));
		line.append(" destroyed=[");
		boolean first = true;
		// the original order is lost due to the use of HashSet
		for (ChunkPosition pos : (Collection<ChunkPosition>) packet.destroyedBlockPositions) {
    		if (!first)
    			line.append(' ');
    		first = false;
            line.append('(').append(pos.x);
            line.append(',').append(pos.y);
            line.append(',').append(pos.z);
            line.append(')');
		}
		line.append(']');
	}

	private void dumpPacket0x3DDoorChange(StringBuilder line, Packet61DoorChange packet) {
		dumpCoordsBlockXYZ(line, packet.posX, packet.posY, packet.posZ);
		// ids are hard-coded in RenderGlobal.playAuxSFX
		line.append(" sfxid=").append(packet.sfxID);
		line.append(" extradata=").append(packet.auxData);
	}

	private void dumpPacket0x46Bed(StringBuilder line, Packet70Bed packet) {
		line.append("reason=").append(packet.bedState);
		line.append(" gamemode=").append(packet.gameMode);
	}

	private void dumpPacket0x47Weather(StringBuilder line, Packet71Weather packet) {
		dumpCoordsAbsoluteIntegerXYZ(line, packet.posX, packet.posY, packet.posZ);
		line.append(" eid=").append(packet.entityID);
		line.append(" islightningbolt=").append(packet.isLightningBolt); // always true
	}

	private void dumpPacket0x64OpenWindow(StringBuilder line, Packet100OpenWindow packet) {
		line.append("windowid=").append(packet.windowId);
		line.append(" inventorytype=").append(packet.inventoryType);
		line.append(" windowtitle=");
		dumpPrintable(line, packet.windowTitle);
		line.append(" numslots=").append(packet.slotsCount);
	}

	private void dumpPacket0x65CloseWindow(StringBuilder line, Packet101CloseWindow packet) {
		line.append("windowid=").append(packet.windowId);
	}

	private void dumpPacket0x66WindowClick(StringBuilder line, Packet102WindowClick packet) {
		line.append("windowid=").append(packet.window_Id);
		line.append(" slot=").append(packet.inventorySlot);
		line.append(" rightclick=").append(packet.mouseClick);
		line.append(" actionid=").append(packet.action);
		line.append(" shift=").append(packet.holdingShift);
		line.append(" clickeditem=");
		dumpItemStack(line, packet.itemStack);
	}

	private void dumpPacket0x67SetSlot(StringBuilder line, Packet103SetSlot packet) {
		line.append("windowid=").append(packet.windowId);
		line.append(" slot=").append(packet.itemSlot);
		line.append(" setitem=");
		dumpItemStack(line, packet.myItemStack);
	}

	private void dumpPacket0x68WindowItems(StringBuilder line, Packet104WindowItems packet) {
		line.append("windowid=").append(packet.windowId);
		line.append(" windowitems=[");
		boolean first = true;
		for (ItemStack itemStack : packet.itemStack) {
			if (!first)
				line.append(',');
			first = false;
			dumpItemStack(line, itemStack);
		}
		line.append(']');
	}

	private void dumpPacket0x69UpdateProgressbar(StringBuilder line, Packet105UpdateProgressbar packet) {
		line.append("windowid=").append(packet.windowId);
		line.append(" property=").append(packet.progressBar);
		line.append(" value=").append(packet.progressBarValue);
	}

	private void dumpPacket0x6ATransaction(StringBuilder line, Packet106Transaction packet) {
		line.append("windowid=").append(packet.windowId);
		line.append(" actionid=").append(packet.shortWindowId);
		line.append(" accepted=").append(packet.accepted);
	}

	private void dumpPacket0x6BCreativeSetSlot(StringBuilder line, Packet107CreativeSetSlot packet) {
		line.append("slot=").append(packet.slot);
		line.append(" item=");
		dumpItemStack(line, packet.itemStack);
	}

	private void dumpPacket0x6CEnchantItem(StringBuilder line, Packet108EnchantItem packet) {
		line.append("windowid=").append(packet.windowId);
		line.append(" enchantment=").append(packet.enchantment);
	}

	private void dumpPacket0x82UpdateSign(StringBuilder line, Packet130UpdateSign packet) {
		dumpCoordsBlockXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
		line.append(' ');
		for (String signLine : packet.signLines) {
			dumpPrintable(line, signLine);
		}
	}

	private void dumpPacket0x83MapData(StringBuilder line, Packet131MapData packet) {
		line.append("item=");
		dumpItemType(line, packet.itemID);
		line.append(" uniqueid=").append(packet.uniqueID); // stored as item damage
		line.append(" data=");
		if (preferences.packet0x83LogAll) {
			dumpHex(line, packet.itemData);
		} else {
			line.append(packet.itemData.length).append(" bytes");
		}
	}

	private void dumpPacket0x84TileEntityData(StringBuilder line, Packet132TileEntityData packet) {
		dumpCoordsBlockXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
		line.append(" action=").append(packet.actionType);
		line.append(" param1=").append(packet.customParam1);
		line.append(" param2=").append(packet.customParam2);
		line.append(" param3=").append(packet.customParam3);
	}

	private void dumpPacket0xC8Statistic(StringBuilder line, Packet200Statistic packet) {
		line.append("statid=").append(packet.statisticId);
		line.append(" amount=").append(packet.amount);
	}

	private void dumpPacket0xC9PlayerInfo(StringBuilder line, Packet201PlayerInfo packet) {
		line.append("username=").append(packet.playerName);
		line.append(" online=").append(packet.isConnected);
		line.append(" ping=").append(packet.ping);
	}

	private void dumpPacket0xCAPlayerAbilities(StringBuilder line, Packet202PlayerAbilities packet) {
		line.append("invulnerable=").append(packet.disableDamage);
		line.append(" isflying=").append(packet.isFlying);
		line.append(" canfly=").append(packet.allowFlying);
		line.append(" iscreative=").append(packet.isCreativeMode);
	}

	private void dumpPacket0xFACustomPayload(StringBuilder line, Packet250CustomPayload packet) {
		line.append("channel=");
		dumpPrintable(line, packet.channel);
		line.append(" length=").append(packet.data.length);
		line.append(" data=");
		dumpPrintable(line, packet.data);
		line.append(" hexdata=");
		dumpHex(line, packet.data);
	}

	private void dumpPacket0xFEServerPing(StringBuilder line, Packet254ServerPing packet) {
		// no payload
		forceFlush = true;
	}

	private void dumpPacket0xFFKickDisconnect(StringBuilder line, Packet255KickDisconnect packet) {
		dumpPrintable(line, packet.reason);
		forceFlush = true;
	}

	// ====
	// ==== String escaping
	// ====

	private static Pattern printable = Pattern.compile("(\\\\|[^\\p{Print}])");
	private static void dumpPrintable(StringBuilder line, String s) {
		Matcher m = printable.matcher(s);
		StringBuffer b = new StringBuffer(s.length()); // derp http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5066679
		while (m.find()) {
			if (m.group(1).equals("\\")) {
				m.appendReplacement(b, "\\\\\\\\");
			} else if (m.group(1).equals("\n")) {
				m.appendReplacement(b, "\\\\n");
			} else if (m.group(1).equals("\t")) {
				m.appendReplacement(b, "\\\\t");
			} else if (m.group(1).equals("\r")) {
				m.appendReplacement(b, "\\\\r");
			} else {
				m.appendReplacement(b, String.format("\\\\0x%2x", (int) m.group(1).charAt(0)));
			}
		}
		m.appendTail(b);
		line.append('[').append(b.toString()).append(']');
	}
	private static void dumpPrintable(StringBuilder line, byte[] data) {
		dumpPrintable(line, new String(data));
	}

	private static void dumpHex(StringBuilder line, byte[] data) {
		line.append('[');
		for (byte x : data) {
			line.append(Integer.toHexString(x)).append(' ');
		}
		line.append(']');
	}

	// ====
	// ==== Game dimensions
	// ====

	/**
	 * @return "(X,Z;x,y,z)", where X,Z are chunk coordinates and x,y,z are block coordinates
	 */
	private static void dumpCoords(StringBuilder line, int chunkX, int chunkZ, int blockX, int blockY, int blockZ) {
		line.append(String.format("(%d,%d;%d,%d,%d)",
				chunkX, chunkZ, blockX, blockY, blockZ));
	}
	private static void dumpCoordsBlockXYZ(StringBuilder line, int blockX, int blockY, int blockZ) {
		dumpCoords(line, blockX >> 4, blockZ >> 4, blockX, blockY, blockZ);
	}
	private static void dumpCoordsAbsoluteIntegerXYZ(StringBuilder line, int absX, int absY, int absZ) {
		dumpCoordsBlockXYZ(line, absX / 32, absY / 32, absZ / 32);
	}
	private static void dumpCoordsChunkXZ(StringBuilder line, int chunkX, int chunkZ) {
		dumpCoords(line, chunkX, chunkZ, chunkX << 4, 0, chunkZ << 4);
	}

	private static void dumpAngleFloat(StringBuilder line, float angle) {
		line.append(String.format("%.1f", angle));
	}
	private static void dumpAngleFloat(StringBuilder line, float yaw, float pitch) {
		line.append(String.format("(%.1f,%.1f)", yaw, pitch));
	}
	private static void dumpAngleByte(StringBuilder line, byte angle) {
		dumpAngleFloat(line,
				(float)angle * 360F / 256F);
	}
	private static void dumpAngleByte(StringBuilder line, byte yaw, byte pitch) {
		dumpAngleFloat(line,
				(float)yaw * 360F / 256F,
				(float)pitch * 360F / 256F);
	}

	private static void dumpFacingDirection2D(StringBuilder line, int facing) {
		line.append(facing).append('(');
		switch (facing) {
		case 0: line.append("-z,north"); break;
		case 1: line.append("-x,west"); break;
		case 2: line.append("+z,south"); break;
		case 3: line.append("+x,east"); break;
		default: line.append("?"); break;
		}
		line.append(')');
	}
	private static void dumpFacingDirection3D(StringBuilder line, int facing) {
		line.append(facing).append('(');
		switch (facing) {
		case 0: line.append("-y,down"); break;
		case 1: line.append("+y,up"); break;
		case 2: line.append("-z,north"); break;
		case 3: line.append("+z,south"); break;
		case 4: line.append("-x,west"); break;
		case 5: line.append("+x,east"); break;
		default: line.append("?"); break;
		}
		line.append(')');
	}

	// ====
	// ==== Game objects
	// ====

	private static void dumpWorldType(StringBuilder line, WorldType worldType) {
		if (worldType == null)
			line.append("<null>");
		else
			line.append(worldType.func_48628_a());
	}

	private static void dumpItemType(StringBuilder line, int itemId) {
		line.append(itemId);
		if (itemId == 0)
			return;
		line.append('(');
		Item item = null;
		if (itemId >= 0 && itemId < Item.itemsList.length)
			item = Item.itemsList[itemId];
		if (item == null)
			line.append('?');
		else
			line.append(item.getItemName());
		line.append(')');
	}

	private static void dumpItemStack(StringBuilder line, ItemStack itemStack) {
		if (itemStack == null) {
			line.append("<null>");
			return;
		}
		dumpItemType(line, itemStack.itemID);
		line.append('x').append(itemStack.stackSize);
		line.append('d').append(itemStack.getItemDamage());
	}

	private static void dumpEntityType(StringBuilder line, int entityType) {
		line.append(entityType).append('(');
		String name = EntityList.getStringFromID(entityType);
		if (name == null)
			line.append('?');
		else
			line.append(name);
		line.append(')');
	}
	private static void dumpEntityType(StringBuilder line, Entity entity) {
		if (entity == null) {
			line.append("<null>");
			return;
		}
		String name = EntityList.getEntityString(entity);
		if (name == null && entity instanceof EntityOtherPlayerMP)
			line.append("<other player>(").append(((EntityOtherPlayerMP) entity).username).append(')');
		else if (name == null)
			line.append("<unlisted>(?)");
		else
			line.append(EntityList.getEntityID(entity)).append('(').append(name).append(')');
	}

	private void dumpExistingEntity(StringBuilder line, int entityId) {
		Entity entity = null;
		if (minecraft != null && minecraft.theWorld != null) {
			WorldClient worldClient = (WorldClient) minecraft.theWorld;
			entity = worldClient.getEntityByID(entityId);
		}
		line.append("[eid=").append(entityId);
		line.append(" type=");
		dumpEntityType(line, entity);
		if (entity != null) {
			line.append(" pos=");
			dumpCoordsBlockXYZ(line, (int) entity.posX, (int) entity.posY, (int) entity.posZ);
		}
		line.append(']');
	}

	/**
	 * @see http://mc.kev009.com/Entities
	 */
	private static void dumpEntityMetadata(StringBuilder line, List<WatchableObject> metadata) {
		line.append('[');
		boolean first = true;
		for (WatchableObject obj : metadata) {
			if (!first)
				line.append(' ');
			first = false;
			switch (obj.getDataValueId()) {
			case 0:
				assertOrLogError(obj.getObjectType() == 0); // byte
				line.append("flags=");
				line.append(obj.getObject());
				break;
			case 1:
				assertOrLogError(obj.getObjectType() == 1); // short
				line.append("drown=");
				line.append(obj.getObject());
				break;
			case 8:
				assertOrLogError(obj.getObjectType() == 2); // int
				line.append("potion=");
				line.append(obj.getObject());
				break;
			case 12:
				assertOrLogError(obj.getObjectType() == 2); // int
				line.append("animal=");
				line.append(obj.getObject());
				break;
			default:
				line.append(obj.getDataValueId());
				//line.append('(').append(obj.getObjectType()).append(')');
				line.append('=');
				dumpPrintable(line, obj.getObject().toString());
			}
		}
		line.append(']');
	}

	// ====
	// ==== Stats
	// ====

	public void saveStats() {
		if (statsFile == null)
			return;
		long now = System.currentTimeMillis();
		long interval = now - started;			
		try {
			PrintWriter writer = new PrintWriter(statsFile);
			writer.printf("Packet stats over %.3f seconds", (double)interval / 1000.0);
			writer.println();
			writer.print("from ");
			writer.println(timestampToString(started));
			writer.print("  to ");
			writer.println(timestampToString(now));
			writer.println("Received:");
			writeStatsTable(writer, interval, receivePacketCounts, receivePacketBytes);
			writer.println("Sent:");
			writeStatsTable(writer, interval, sentPacketCounts, sentPacketBytes);
			writer.close();
		} catch (IOException e) {
			Log.getLogger().log(Level.SEVERE, "unable to dump stats", e);
		}
	}

	private static void writeStatsTable(PrintWriter writer, long intervalMillis, long[] packetCounts, long[] byteCounts) {
		writer.println("  packet          name     count    approx bytes   average");
		writer.println("-------- ------------- --------- --------------- ---------");

		long packetTotal = 0L;
		long byteTotal = 0L;
		for (int i = 0; i < packetCounts.length; i++) {
			if (packetCounts[i] > 0) {
				packetTotal += packetCounts[i];
				byteTotal += byteCounts[i];
				writer.printf(" %3d(%02X)  %-12s %9d %15d %9.2f",
						i, i, getPacketShortName(i), packetCounts[i], byteCounts[i],
						(double)byteCounts[i] / (double)packetCounts[i]);
				writer.println();
			}
		}
		writer.printf("        * all          %9d %15d %9.2f",
				packetTotal, byteTotal,
				(double)byteTotal / (double)packetTotal);
		writer.println();

		// This isn't the true bandwidth as sent over the wire. We're up at the application level,
		// and many of the packet payload sizes are reported inaccurately by their classes.
		// It's a decent approximation, though.
		double bps = 0.0;
		if (intervalMillis != 0L) {
			// bits per second, not bytes per millisecond
			bps = (double)byteTotal * 8.0 / ((double)intervalMillis / 1000.0);
		}
		// kilo/mega, not kibi/mebi
		writer.printf("Approximate bandwidth: %.2f bps = %.2f kbps = %.2f Mbps", bps, bps / 1000.0, bps / 1000.0 / 1000.0);
		writer.println();
	}

	// ====
	// ====
	// ====

	private static String getPacketShortName(int id) {
		return PacketInfo.ALL_PACKETS.get(id).shortName;
	}

	/**
	 * Get the packet's self-reported payload size. This is known to be inaccurate for some packets,
	 * e.g. Packet202PlayerAbilities and anything with metadata.
	 */
	private static int getPacketPayloadSize(Packet packet) {
		return packet.getPacketSize();
	}

	private static void assertOrLogError(boolean condition) {
		if (!condition) {
			// log stack trace but don't actually throw the exception
			Log.getLogger().log(Level.SEVERE, "assertion failed", new Exception());
		}
	}
}
