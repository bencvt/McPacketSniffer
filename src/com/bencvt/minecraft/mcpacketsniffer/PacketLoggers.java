package com.bencvt.minecraft.mcpacketsniffer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

import net.minecraft.src.*;

/**
 * Worker class that converts each Packet type into a log line.
 */
public class PacketLoggers extends PacketLoggersBase {
    // Annotation just for documentation. No reflection magic for dispatching for now.
    public @interface PacketLogger {
        int id();
        int hex();
    }

    public void dispatch(StringBuilder line, PacketDirection dir, Packet packet) {
        switch (packet.getPacketId()) {
        case 0x00: logPacketKeepAlive(line, dir, (Packet0KeepAlive) packet); break;
        case 0x01: logPacketLogin(line, dir, (Packet1Login) packet); break;
        case 0x02: logPacketClientProtocol(line, dir, (Packet2ClientProtocol) packet); break;
        case 0x03: logPacketChat(line, dir, (Packet3Chat) packet); break;
        case 0x04: logPacketUpdateTime(line, dir, (Packet4UpdateTime) packet); break;
        case 0x05: logPacketPlayerInventory(line, dir, (Packet5PlayerInventory) packet); break;
        case 0x06: logPacketSpawnPosition(line, dir, (Packet6SpawnPosition) packet); break;
        case 0x07: logPacketUseEntity(line, dir, (Packet7UseEntity) packet); break;
        case 0x08: logPacketUpdateHealth(line, dir, (Packet8UpdateHealth) packet); break;
        case 0x09: logPacketRespawn(line, dir, (Packet9Respawn) packet); break;
        case 0x0A: logPacketFlying(line, dir, (Packet10Flying) packet); break;
        case 0x0B: logPacketPlayerPosition(line, dir, (Packet11PlayerPosition) packet); break;
        case 0x0C: logPacketPlayerLook(line, dir, (Packet12PlayerLook) packet); break;
        case 0x0D: logPacketPlayerLookMove(line, dir, (Packet13PlayerLookMove) packet); break;
        case 0x0E: logPacketBlockDig(line, dir, (Packet14BlockDig) packet); break;
        case 0x0F: logPacketPlace(line, dir, (Packet15Place) packet); break;
        case 0x10: logPacketBlockItemSwitch(line, dir, (Packet16BlockItemSwitch) packet); break;
        case 0x11: logPacketSleep(line, dir, (Packet17Sleep) packet); break;
        case 0x12: logPacketAnimation(line, dir, (Packet18Animation) packet); break;
        case 0x13: logPacketEntityAction(line, dir, (Packet19EntityAction) packet); break;
        case 0x14: logPacketNamedEntitySpawn(line, dir, (Packet20NamedEntitySpawn) packet); break;
        case 0x15: logPacketPickupSpawn(line, dir, (Packet21PickupSpawn) packet); break;
        case 0x16: logPacketCollect(line, dir, (Packet22Collect) packet); break;
        case 0x17: logPacketVehicleSpawn(line, dir, (Packet23VehicleSpawn) packet); break;
        case 0x18: logPacketMobSpawn(line, dir, (Packet24MobSpawn) packet); break;
        case 0x19: logPacketEntityPainting(line, dir, (Packet25EntityPainting) packet); break;
        case 0x1A: logPacketEntityExpOrb(line, dir, (Packet26EntityExpOrb) packet); break;
        case 0x1C: logPacketEntityVelocity(line, dir, (Packet28EntityVelocity) packet); break;
        case 0x1D: logPacketDestroyEntity(line, dir, (Packet29DestroyEntity) packet); break;
        case 0x1E: logPacketEntity(line, dir, (Packet30Entity) packet); break;
        case 0x1F: logPacketRelEntityMove(line, dir, (Packet31RelEntityMove) packet); break;
        case 0x20: logPacketEntityLook(line, dir, (Packet32EntityLook) packet); break;
        case 0x21: logPacketRelEntityMoveLook(line, dir, (Packet33RelEntityMoveLook) packet); break;
        case 0x22: logPacketEntityTeleport(line, dir, (Packet34EntityTeleport) packet); break;
        case 0x23: logPacketEntityHeadRotation(line, dir, (Packet35EntityHeadRotation) packet); break;
        case 0x26: logPacketEntityStatus(line, dir, (Packet38EntityStatus) packet); break;
        case 0x27: logPacketAttachEntity(line, dir, (Packet39AttachEntity) packet); break;
        case 0x28: logPacketEntityMetadata(line, dir, (Packet40EntityMetadata) packet); break;
        case 0x29: logPacketEntityEffect(line, dir, (Packet41EntityEffect) packet); break;
        case 0x2A: logPacketRemoveEntityEffect(line, dir, (Packet42RemoveEntityEffect) packet); break;
        case 0x2B: logPacketExperience(line, dir, (Packet43Experience) packet); break;
        case 0x33: logPacketMapChunk(line, dir, (Packet51MapChunk) packet); break;
        case 0x34: logPacketMultiBlockChange(line, dir, (Packet52MultiBlockChange) packet); break;
        case 0x35: logPacketBlockChange(line, dir, (Packet53BlockChange) packet); break;
        case 0x36: logPacketPlayNoteBlock(line, dir, (Packet54PlayNoteBlock) packet); break;
        case 0x37: logPacketBlockDestroy(line, dir, (Packet55BlockDestroy) packet); break;
        case 0x38: logPacketMapChunks(line, dir, (Packet56MapChunks) packet); break;
        case 0x3C: logPacketExplosion(line, dir, (Packet60Explosion) packet); break;
        case 0x3D: logPacketDoorChange(line, dir, (Packet61DoorChange) packet); break;
        case 0x3E: logPacketLevelSound(line, dir, (Packet62LevelSound) packet); break;
        case 0x46: logPacketGameEvent(line, dir, (Packet70GameEvent) packet); break;
        case 0x47: logPacketWeather(line, dir, (Packet71Weather) packet); break;
        case 0x64: logPacketOpenWindow(line, dir, (Packet100OpenWindow) packet); break;
        case 0x65: logPacketCloseWindow(line, dir, (Packet101CloseWindow) packet); break;
        case 0x66: logPacketWindowClick(line, dir, (Packet102WindowClick) packet); break;
        case 0x67: logPacketSetSlot(line, dir, (Packet103SetSlot) packet); break;
        case 0x68: logPacketWindowItems(line, dir, (Packet104WindowItems) packet); break;
        case 0x69: logPacketUpdateProgressbar(line, dir, (Packet105UpdateProgressbar) packet); break;
        case 0x6A: logPacketTransaction(line, dir, (Packet106Transaction) packet); break;
        case 0x6B: logPacketCreativeSetSlot(line, dir, (Packet107CreativeSetSlot) packet); break;
        case 0x6C: logPacketEnchantItem(line, dir, (Packet108EnchantItem) packet); break;
        case 0x82: logPacketUpdateSign(line, dir, (Packet130UpdateSign) packet); break;
        case 0x83: logPacketMapData(line, dir, (Packet131MapData) packet); break;
        case 0x84: logPacketTileEntityData(line, dir, (Packet132TileEntityData) packet); break;
        case 0xC8: logPacketStatistic(line, dir, (Packet200Statistic) packet); break;
        case 0xC9: logPacketPlayerInfo(line, dir, (Packet201PlayerInfo) packet); break;
        case 0xCA: logPacketPlayerAbilities(line, dir, (Packet202PlayerAbilities) packet); break;
        case 0xCB: logPacketAutoComplete(line, dir, (Packet203AutoComplete) packet); break;
        case 0xCC: logPacketClientInfo(line, dir, (Packet204ClientInfo) packet); break;
        case 0xCD: logPacketClientCommand(line, dir, (Packet205ClientCommand) packet); break;
        case 0xFA: logPacketCustomPayload(line, dir, (Packet250CustomPayload) packet); break;
        case 0xFC: logPacketSharedKey(line, dir, (Packet252SharedKey) packet); break;
        case 0xFD: logPacketServerAuthData(line, dir, (Packet253ServerAuthData) packet); break;
        case 0xFE: logPacketServerPing(line, dir, (Packet254ServerPing) packet); break;
        case 0xFF: logPacketKickDisconnect(line, dir, (Packet255KickDisconnect) packet); break;
        default:
            LogManager.eventLog.severe("unhandled packet id=" + packet.getPacketId() + " class=" + packet.getClass());
            logApproximatePacketPayloadSize(line, packet);
            // we don't have access to the actual payload
        }
    }

    //
    // Individual packet loggers
    //

    @PacketLogger(id=0, hex=0x00)
    public void logPacketKeepAlive(StringBuilder line, PacketDirection dir, Packet0KeepAlive packet) {
        line.append("id=").append(packet.randomId);
    }

    @PacketLogger(id=1, hex=0x01)
    public void logPacketLogin(StringBuilder line, PacketDirection dir, Packet1Login packet) {
        line.append("eid=").append(packet.clientEntityId);
        line.append(" worldtype=");
        logWorldType(line, packet.terrainType);
        line.append(" gamemode=");
        logGameMode(line, packet.gameType);
        line.append(" hardcore=").append(packet.field_73560_c);
        line.append(" dimension=");
        CodeTable.dimension.log(line, packet.dimension);
        line.append(" difficulty=");
        CodeTable.difficulty.log(line, packet.difficultySetting);
        line.append(" unused=").append(packet.worldHeight); // always 0; ignored by client
        line.append(" maxplayers=").append(packet.maxPlayers);
    }

    @PacketLogger(id=2, hex=0x02)
    public void logPacketClientProtocol(StringBuilder line, PacketDirection dir, Packet2ClientProtocol packet) {
        line.append("protocolversion=").append(packet.getProtocolVersion());
        line.append(" username=");
        logString(line, packet.getUsername());
        line.append(" host=");
        line.append(Util.getFieldByType(packet, String.class, 1));
        line.append(" port=");
        line.append(Util.getFieldByType(packet, int.class, 1));
    }

    @PacketLogger(id=3, hex=0x03)
    public void logPacketChat(StringBuilder line, PacketDirection dir, Packet3Chat packet) {
        logString(line, packet.message);
    }

    @PacketLogger(id=4, hex=0x04)
    public void logPacketUpdateTime(StringBuilder line, PacketDirection dir, Packet4UpdateTime packet) {
        line.append("time=").append(packet.time);
    }

    @PacketLogger(id=5, hex=0x05)
    public void logPacketPlayerInventory(StringBuilder line, PacketDirection dir, Packet5PlayerInventory packet) {
        logExistingEntity(line, packet.entityID);
        line.append(" slot=").append(packet.slot);
        line.append(" item=");
        logItemStack(line, packet.getItemSlot());
    }

    @PacketLogger(id=6, hex=0x06)
    public void logPacketSpawnPosition(StringBuilder line, PacketDirection dir, Packet6SpawnPosition packet) {
        logCoordsBlockXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
    }

    @PacketLogger(id=7, hex=0x07)
    public void logPacketUseEntity(StringBuilder line, PacketDirection dir, Packet7UseEntity packet) {
        line.append("player=").append(packet.playerEntityId); // don't log the entire entity
        line.append(" target=");
        logExistingEntity(line, packet.targetEntity);
        line.append(" leftclick=").append(packet.isLeftClick);
    }

    @PacketLogger(id=8, hex=0x08)
    public void logPacketUpdateHealth(StringBuilder line, PacketDirection dir, Packet8UpdateHealth packet) {
        line.append("health=").append(packet.healthMP);
        line.append(" food=").append(packet.food);
        line.append(" foodsaturation=").append(String.format("%.1f", packet.foodSaturation));
    }

    @PacketLogger(id=9, hex=0x09)
    public void logPacketRespawn(StringBuilder line, PacketDirection dir, Packet9Respawn packet) {
        line.append("dimension=");
        CodeTable.dimension.log(line, packet.respawnDimension);
        line.append(" difficulty=");
        CodeTable.difficulty.log(line, packet.difficulty);
        line.append(" gamemode=");
        logGameMode(line, packet.gameType);
        line.append(" unused=").append(packet.worldHeight);
        line.append(" worldtype=");
        logWorldType(line, packet.terrainType);
    }

    @PacketLogger(id=10, hex=0x0A)
    public void logPacketFlying(StringBuilder line, PacketDirection dir, Packet10Flying packet) {
        line.append("onground=").append(packet.onGround);
    }

    @PacketLogger(id=11, hex=0x0B)
    public void logPacketPlayerPosition(StringBuilder line, PacketDirection dir, Packet11PlayerPosition packet) {
        logCoordsBlockXYZ(line, (int) packet.xPosition, (int) packet.yPosition, (int) packet.zPosition);
        line.append(" onground=").append(packet.onGround);
        line.append(" stance=").append(String.format("%.1f", packet.stance));
    }

    @PacketLogger(id=12, hex=0x0C)
    public void logPacketPlayerLook(StringBuilder line, PacketDirection dir, Packet12PlayerLook packet) {
        line.append("onground=").append(packet.onGround);
        line.append(" yawpitch=");
        logAngleFloat(line, packet.yaw, packet.pitch);
    }

    @PacketLogger(id=13, hex=0x0D)
    public void logPacketPlayerLookMove(StringBuilder line, PacketDirection dir, Packet13PlayerLookMove packet) {
        double realY = packet.yPosition;
        double realStance = packet.stance;
        if (dir == PacketDirection.S2C) {
            // You done goofed, Notch.
            double tmp = realY;
            realY = realStance;
            realStance = tmp;
        }
        logCoordsBlockXYZ(line, (int) packet.xPosition, (int) realY, (int) packet.zPosition);
        line.append(" onground=").append(packet.onGround);
        line.append(" stance=").append(String.format("%.1f", realStance));
        line.append(" yawpitch=");
        logAngleFloat(line, packet.yaw, packet.pitch);
    }

    @PacketLogger(id=14, hex=0x0E)
    public void logPacketBlockDig(StringBuilder line, PacketDirection dir, Packet14BlockDig packet) {
        logCoordsBlockXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
        line.append(" status=");
        CodeTable.blockDigStatus.log(line, packet.status);
        line.append(" face=");
        CodeTable.facing3D.log(line, packet.face);
    }

    @PacketLogger(id=15, hex=0x0F)
    public void logPacketPlace(StringBuilder line, PacketDirection dir, Packet15Place packet) {
        logCoordsBlockXYZ(line, packet.getXPosition(), packet.getYPosition(), packet.getZPosition());
        line.append(" direction=");
        CodeTable.facing3D.log(line, packet.getDirection());
        line.append(" helditem=");
        logItemStack(line, packet.getItemStack());
        line.append(" cursorx=").append(String.format("%.1f", packet.getXOffset()));
        line.append(" cursory=").append(String.format("%.1f", packet.getYOffset()));
        line.append(" cursorz=").append(String.format("%.1f", packet.getZOffset()));
    }

    @PacketLogger(id=16, hex=0x10)
    public void logPacketBlockItemSwitch(StringBuilder line, PacketDirection dir, Packet16BlockItemSwitch packet) {
        line.append("slotid=").append(packet.id);
    }

    @PacketLogger(id=17, hex=0x11)
    public void logPacketSleep(StringBuilder line, PacketDirection dir, Packet17Sleep packet) {
        logCoordsBlockXYZ(line, packet.bedX, packet.bedY, packet.bedZ);
        line.append(" unused=").append(packet.field_73622_e); // always 0
        line.append(" entity=");
        logExistingEntity(line, packet.entityID);
    }

    @PacketLogger(id=18, hex=0x12)
    public void logPacketAnimation(StringBuilder line, PacketDirection dir, Packet18Animation packet) {
        logExistingEntity(line, packet.entityId);
        line.append(" animation=");
        CodeTable.entityAnimation.log(line, packet.animate);
    }

    @PacketLogger(id=19, hex=0x13)
    public void logPacketEntityAction(StringBuilder line, PacketDirection dir, Packet19EntityAction packet) {
        logExistingEntity(line, packet.entityId);
        line.append(" action=");
        CodeTable.entityAction.log(line, packet.state);
    }

    @PacketLogger(id=20, hex=0x14)
    public void logPacketNamedEntitySpawn(StringBuilder line, PacketDirection dir, Packet20NamedEntitySpawn packet) {
        logCoordsAbsoluteIntegerXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
        line.append(" eid=").append(packet.entityId);
        line.append(" name=");
        logString(line, packet.name);
        line.append(" yawpitch=");
        logAngleByte(line, packet.rotation, packet.pitch);
        line.append(" helditem=");
        logItemType(line, packet.currentItem);
        line.append(" metadata=");
        logEntityMetadata(line, packet.func_73509_c());
    }

    @PacketLogger(id=21, hex=0x15)
    public void logPacketPickupSpawn(StringBuilder line, PacketDirection dir, Packet21PickupSpawn packet) {
        logCoordsAbsoluteIntegerXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
        line.append(" eid=").append(packet.entityId);
        line.append(" item=");
        logItemStack(line, packet.itemID);
        line.append(" yawpitch=");
        logAngleByte(line, packet.rotation, packet.pitch);
        line.append(" roll=");
        logAngleByte(line, packet.roll);
    }

    @PacketLogger(id=22, hex=0x16)
    public void logPacketCollect(StringBuilder line, PacketDirection dir, Packet22Collect packet) {
        logExistingEntity(line, packet.collectedEntityId);
        line.append(" collectedby=");
        logExistingEntity(line, packet.collectorEntityId);
    }

    @PacketLogger(id=23, hex=0x17)
    public void logPacketVehicleSpawn(StringBuilder line, PacketDirection dir, Packet23VehicleSpawn packet) {
        logCoordsAbsoluteIntegerXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
        line.append(" eid=").append(packet.entityId);
        line.append(" type=");
        CodeTable.entitySpawnObject.log(line, packet.type);
        line.append(" thrower=");
        if (packet.throwerEntityId <= 0) {
            line.append(packet.throwerEntityId);
        } else {
            logExistingEntity(line, packet.throwerEntityId);
            line.append(" speedx=").append(packet.speedX);
            line.append(" speedy=").append(packet.speedY);
            line.append(" speedz=").append(packet.speedZ);
        }
    }

    @PacketLogger(id=24, hex=0x18)
    public void logPacketMobSpawn(StringBuilder line, PacketDirection dir, Packet24MobSpawn packet) {
        logCoordsAbsoluteIntegerXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
        line.append(" eid=").append(packet.entityId);
        line.append(" type=");
        logEntityType(line, packet.type);
        line.append(" yawpitch=");
        logAngleByte(line, packet.yaw, packet.pitch);
        line.append(" headyaw=");
        logAngleByte(line, packet.headYaw);
        line.append(" velocity=");
        logVelocityShort(line, packet.velocityX, packet.velocityY, packet.velocityZ);
        line.append(" metadata=");
        logEntityMetadata(line, packet.getMetadata());
    }

    @PacketLogger(id=25, hex=0x19)
    public void logPacketEntityPainting(StringBuilder line, PacketDirection dir, Packet25EntityPainting packet) {
        logCoordsBlockXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
        line.append(" eid=").append(packet.entityId);
        line.append(" dir=");
        CodeTable.facing2D.log(line, packet.direction);
        line.append(" title=").append(packet.title);
    }

    @PacketLogger(id=26, hex=0x1A)
    public void logPacketEntityExpOrb(StringBuilder line, PacketDirection dir, Packet26EntityExpOrb packet) {
        logCoordsAbsoluteIntegerXYZ(line, packet.posX, packet.posY, packet.posZ);
        line.append(" eid=").append(packet.entityId);
        line.append(" expvalue=").append(packet.xpValue);
    }

    @PacketLogger(id=28, hex=0x1C)
    public void logPacketEntityVelocity(StringBuilder line, PacketDirection dir, Packet28EntityVelocity packet) {
        logExistingEntity(line, packet.entityId);
        line.append(" velocity=");
        logVelocityShort(line, packet.motionX, packet.motionY, packet.motionZ);
    }

    @PacketLogger(id=29, hex=0x1D)
    public void logPacketDestroyEntity(StringBuilder line, PacketDirection dir, Packet29DestroyEntity packet) {
        line.append("count=").append(packet.entityId.length);
        line.append(" entities=[");
        for (int i = 0; i < packet.entityId.length; i++) {
            if (i > 0) {
                line.append(", ");
            }
            logExistingEntity(line, packet.entityId[i]);
        }
        line.append(']');
    }

    @PacketLogger(id=30, hex=0x1E)
    public void logPacketEntity(StringBuilder line, PacketDirection dir, Packet30Entity packet) {
        logExistingEntity(line, packet.entityId);
    }

    @PacketLogger(id=31, hex=0x1F)
    public void logPacketRelEntityMove(StringBuilder line, PacketDirection dir, Packet31RelEntityMove packet) {
        logExistingEntity(line, packet.entityId);
        line.append(" relmove=");
        logRelativeMove(line, packet.xPosition, packet.yPosition, packet.zPosition);
    }

    @PacketLogger(id=32, hex=0x20)
    public void logPacketEntityLook(StringBuilder line, PacketDirection dir, Packet32EntityLook packet) {
        logExistingEntity(line, packet.entityId);
        line.append(" yawpitch=");
        logAngleByte(line, packet.yaw, packet.pitch);
    }

    @PacketLogger(id=33, hex=0x21)
    public void logPacketRelEntityMoveLook(StringBuilder line, PacketDirection dir, Packet33RelEntityMoveLook packet) {
        logExistingEntity(line, packet.entityId);
        line.append(" relmove=");
        logRelativeMove(line, packet.xPosition, packet.yPosition, packet.zPosition);
        line.append(" yawpitch=");
        logAngleByte(line, packet.yaw, packet.pitch);
    }

    @PacketLogger(id=34, hex=0x22)
    public void logPacketEntityTeleport(StringBuilder line, PacketDirection dir, Packet34EntityTeleport packet) {
        logExistingEntity(line, packet.entityId);
        line.append(" teleport=");
        logCoordsAbsoluteIntegerXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
        line.append(" yawpitch=");
        logAngleByte(line, packet.yaw, packet.pitch);
    }

    @PacketLogger(id=35, hex=0x23)
    public void logPacketEntityHeadRotation(StringBuilder line, PacketDirection dir, Packet35EntityHeadRotation packet) {
        logExistingEntity(line, packet.entityId);
        line.append(" headyaw=");
        logAngleByte(line, packet.headRotationYaw);
    }

    @PacketLogger(id=38, hex=0x26)
    public void logPacketEntityStatus(StringBuilder line, PacketDirection dir, Packet38EntityStatus packet) {
        logExistingEntity(line, packet.entityId);
        line.append(" status=");
        CodeTable.entityStatus.log(line, packet.entityStatus);
    }

    @PacketLogger(id=39, hex=0x27)
    public void logPacketAttachEntity(StringBuilder line, PacketDirection dir, Packet39AttachEntity packet) {
        logExistingEntity(line, packet.entityId);
        line.append(" vehicle=");
        logExistingEntity(line, packet.vehicleEntityId);
    }

    @PacketLogger(id=40, hex=0x28)
    public void logPacketEntityMetadata(StringBuilder line, PacketDirection dir, Packet40EntityMetadata packet) {
        logExistingEntity(line, packet.entityId);
        line.append(" metadata=");
        logEntityMetadata(line, packet.getMetadata());
    }

    @PacketLogger(id=41, hex=0x29)
    public void logPacketEntityEffect(StringBuilder line, PacketDirection dir, Packet41EntityEffect packet) {
        logExistingEntity(line, packet.entityId);
        line.append(" effectid=").append(packet.effectId);
        line.append(" amplifier=").append(packet.effectAmplifier);
        line.append(" duration=").append(packet.duration);
    }

    @PacketLogger(id=42, hex=0x2A)
    public void logPacketRemoveEntityEffect(StringBuilder line, PacketDirection dir, Packet42RemoveEntityEffect packet) {
        logExistingEntity(line, packet.entityId);
        line.append(" effectid=").append(packet.effectId);
    }

    @PacketLogger(id=43, hex=0x2B)
    public void logPacketExperience(StringBuilder line, PacketDirection dir, Packet43Experience packet) {
        line.append("expbar=").append(String.format("%.3f", packet.experience));
        line.append(" level=").append(packet.experienceLevel);
        line.append(" total=").append(packet.experienceTotal);
    }

    @PacketLogger(id=51, hex=0x33)
    public void logPacketMapChunk(StringBuilder line, PacketDirection dir, Packet51MapChunk packet) {
        logCoordsChunkXZ(line, packet.xCh, packet.zCh);
        // these MCP-specified field names are incorrect as of 1.3.1, which drastically changed packet 51.
        line.append(" continuous=").append(packet.includeInitialize);
        line.append(" sections=0x").append(Integer.toHexString(packet.yChMin));
        line.append(" addsections=0x").append(Integer.toHexString(packet.yChMax));
        // HACK: because tempLength is private
        line.append(" compressedsize=").append(packet.getPacketSize() - 17);
        line.append(" chunkdata=");
        if (LogManager.options.SUMMARIZE_BINARY_DATA) {
            line.append(packet.func_73593_d().length).append(" bytes");
        } else {
            logByteArrayHexDump(line, packet.func_73593_d());
        }
    }

    @PacketLogger(id=52, hex=0x34)
    public void logPacketMultiBlockChange(StringBuilder line, PacketDirection dir, Packet52MultiBlockChange packet) {
        logCoordsChunkXZ(line, packet.xPosition, packet.zPosition);
        line.append(" numblocks=").append(packet.size);
        line.append(" blocks=[");
        // the following code is adapted from NetClientHandler
        int xBase = packet.xPosition << 4;
        int zBase = packet.zPosition << 4;
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(packet.metadataArray));
        try {
            for (int k = 0; k < packet.size; k++) {
                short wordPos = dataStream.readShort();
                short wordBlock = dataStream.readShort();
                int xOff = (wordPos >> 12) & 0xf;
                int zOff = (wordPos >> 8) & 0xf;
                int yAbs = wordPos & 0xff;
                int blockId = ((wordBlock & 0xfff) >> 4);
                int blockMetadata = wordBlock & 0xf;
                if (k > 0) {
                    line.append(' ');
                }
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

    @PacketLogger(id=53, hex=0x35)
    public void logPacketBlockChange(StringBuilder line, PacketDirection dir, Packet53BlockChange packet) {
        logCoordsBlockXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
        line.append(" block=");
        line.append(packet.type);
        line.append('d');
        line.append(packet.metadata);
    }

    @PacketLogger(id=54, hex=0x36)
    public void logPacketPlayNoteBlock(StringBuilder line, PacketDirection dir, Packet54PlayNoteBlock packet) {
        logCoordsBlockXYZ(line, packet.xLocation, packet.yLocation, packet.zLocation);
        // @see http://mc.kev009.com/Block_Actions
        line.append(" blockactionid=").append(packet.instrumentType);
        line.append(" blockactionparam=").append(packet.pitch);
        line.append(" blockid=").append(packet.blockId);
    }

    @PacketLogger(id=55, hex=0x37)
    public void logPacketBlockDestroy(StringBuilder line, PacketDirection dir, Packet55BlockDestroy packet) {
        logCoordsBlockXYZ(line, packet.getPosX(), packet.getPosY(), packet.getPosZ());
        line.append(" destroyer=");
        logExistingEntity(line, packet.getEntityId());
        line.append(" stage=").append(packet.getDestroyedStage());
    }

    @PacketLogger(id=56, hex=0x38)
    public void logPacketMapChunks(StringBuilder line, PacketDirection dir, Packet56MapChunks packet) {
        int numChunks = packet.func_73581_d();
        line.append("numchunks=").append(numChunks);
        // HACK: because field_73585_g is private
        line.append(" compressedsize=").append(packet.getPacketSize() - 6 - numChunks*12);
        line.append(" chunks=[");
        for (int i = 0; i < numChunks; i++) {
            if (i > 0) {
                line.append(", ");
            }
            line.append("{pos=");
            logCoordsChunkXZ(line, packet.func_73582_a(i), packet.func_73580_b(i));
            line.append(" sections=0x").append(Integer.toHexString(packet.field_73590_a[i]));
            line.append(" addsections=0x").append(Integer.toHexString(packet.field_73588_b[i]));
            line.append(" chunkdata=");
            if (LogManager.options.SUMMARIZE_BINARY_DATA) {
                line.append(packet.func_73583_c(i).length).append(" bytes");
            } else {
                logByteArrayHexDump(line, packet.func_73583_c(i));
            }
            line.append('}');
        }
        line.append(']');
    }

    @PacketLogger(id=60, hex=0x3C)
    public void logPacketExplosion(StringBuilder line, PacketDirection dir, Packet60Explosion packet) {
        logCoordsBlockXYZ(line, (int) packet.explosionX, (int) packet.explosionY, (int) packet.explosionZ);
        line.append(" radius=").append(String.format("%.1f", packet.explosionSize));
        line.append(" destroyed=[");
        for (int i = 0; i < packet.chunkPositionRecords.size(); i++) {
            if (i > 0) {
                line.append(", ");
            }
            ChunkPosition pos = (ChunkPosition) packet.chunkPositionRecords.get(i);
            line.append('(').append(pos.x);
            line.append(',').append(pos.y);
            line.append(',').append(pos.z);
            line.append(')');
        }
        line.append(']');
        line.append(" knockback=");
        logVelocityFloat(line, packet.func_73607_d(), packet.func_73609_f(), packet.func_73608_g());
    }

    @PacketLogger(id=61, hex=0x3D)
    public void logPacketDoorChange(StringBuilder line, PacketDirection dir, Packet61DoorChange packet) {
        logCoordsBlockXYZ(line, packet.posX, packet.posY, packet.posZ);
        line.append(" effect=");
        CodeTable.soundOrParticleEffect.log(line, packet.sfxID);
        line.append(" extradata=").append(packet.auxData);
    }

    @PacketLogger(id=62, hex=0x3E)
    public void logPacketLevelSound(StringBuilder line, PacketDirection dir, Packet62LevelSound packet) {
        logCoordsBlockXYZ(line,
                (int) packet.getEffectX(),
                (int) packet.getEffectY(),
                (int) packet.getEffectZ());
        line.append(" soundname=").append(packet.getSoundName());
        line.append(" volume=").append(String.format("%.1f", packet.getVolume()*100)).append('%');
        line.append(" pitch=").append(String.format("%.1f", packet.getPitch()*100)).append('%');
    }

    @PacketLogger(id=70, hex=0x46)
    public void logPacketGameEvent(StringBuilder line, PacketDirection dir, Packet70GameEvent packet) {
        line.append("reason=");
        CodeTable.changeGameStateReason.log(line, packet.bedState);
        if (packet.bedState == 3) {
            line.append(" gamemode=").append(packet.gameMode);
        }
    }

    @PacketLogger(id=71, hex=0x47)
    public void logPacketWeather(StringBuilder line, PacketDirection dir, Packet71Weather packet) {
        logCoordsAbsoluteIntegerXYZ(line, packet.posX, packet.posY, packet.posZ);
        line.append(" eid=").append(packet.entityID);
        line.append(" islightningbolt=").append(packet.isLightningBolt); // always true
    }

    @PacketLogger(id=100, hex=0x64)
    public void logPacketOpenWindow(StringBuilder line, PacketDirection dir, Packet100OpenWindow packet) {
        line.append("windowid=").append(packet.windowId);
        line.append(" inventorytype=").append(packet.inventoryType);
        line.append(" windowtitle=");
        logString(line, packet.windowTitle);
        line.append(" numslots=").append(packet.slotsCount);
    }

    @PacketLogger(id=101, hex=0x65)
    public void logPacketCloseWindow(StringBuilder line, PacketDirection dir, Packet101CloseWindow packet) {
        line.append("windowid=").append(packet.windowId);
    }

    @PacketLogger(id=102, hex=0x66)
    public void logPacketWindowClick(StringBuilder line, PacketDirection dir, Packet102WindowClick packet) {
        line.append("windowid=").append(packet.window_Id);
        line.append(" slot=").append(packet.inventorySlot);
        line.append(" rightclick=").append(packet.mouseClick);
        line.append(" actionid=").append(packet.action);
        line.append(" shift=").append(packet.holdingShift);
        line.append(" clickeditem=");
        logItemStack(line, packet.itemStack);
    }

    @PacketLogger(id=103, hex=0x67)
    public void logPacketSetSlot(StringBuilder line, PacketDirection dir, Packet103SetSlot packet) {
        line.append("windowid=").append(packet.windowId);
        line.append(" slot=").append(packet.itemSlot);
        line.append(" setitem=");
        logItemStack(line, packet.myItemStack);
    }

    @PacketLogger(id=104, hex=0x68)
    public void logPacketWindowItems(StringBuilder line, PacketDirection dir, Packet104WindowItems packet) {
        line.append("windowid=").append(packet.windowId);
        line.append(" numwindowitems=").append(packet.itemStack.length);
        line.append(" windowitems=[");
        for (int i = 0; i < packet.itemStack.length; i++) {
            if (i > 0) {
                line.append(", ");
            }
            logItemStack(line, packet.itemStack[i]);
        }
        line.append(']');
    }

    @PacketLogger(id=105, hex=0x69)
    public void logPacketUpdateProgressbar(StringBuilder line, PacketDirection dir, Packet105UpdateProgressbar packet) {
        line.append("windowid=").append(packet.windowId);
        line.append(" property=").append(packet.progressBar);
        line.append(" value=").append(packet.progressBarValue);
    }

    @PacketLogger(id=106, hex=0x6A)
    public void logPacketTransaction(StringBuilder line, PacketDirection dir, Packet106Transaction packet) {
        line.append("windowid=").append(packet.windowId);
        line.append(" actionid=").append(packet.shortWindowId);
        line.append(" accepted=").append(packet.accepted);
    }

    @PacketLogger(id=107, hex=0x6B)
    public void logPacketCreativeSetSlot(StringBuilder line, PacketDirection dir, Packet107CreativeSetSlot packet) {
        line.append("slot=").append(packet.slot);
        line.append(" item=");
        logItemStack(line, packet.itemStack);
    }

    @PacketLogger(id=108, hex=0x6C)
    public void logPacketEnchantItem(StringBuilder line, PacketDirection dir, Packet108EnchantItem packet) {
        line.append("windowid=").append(packet.windowId);
        line.append(" enchantment=").append(packet.enchantment);
    }

    @PacketLogger(id=130, hex=0x82)
    public void logPacketUpdateSign(StringBuilder line, PacketDirection dir, Packet130UpdateSign packet) {
        logCoordsBlockXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
        line.append(' ');
        for (String signLine : packet.signLines) {
            logString(line, signLine);
        }
    }

    @PacketLogger(id=131, hex=0x83)
    public void logPacketMapData(StringBuilder line, PacketDirection dir, Packet131MapData packet) {
        line.append("item=");
        logItemType(line, packet.itemID);
        line.append(" uniqueid=").append(packet.uniqueID); // stored as item damage
        line.append(" data=");
        if (LogManager.options.SUMMARIZE_BINARY_DATA) {
            line.append(packet.itemData.length).append(" bytes");
        } else {
            logByteArrayHexDump(line, packet.itemData);
        }
    }

    @PacketLogger(id=132, hex=0x84)
    public void logPacketTileEntityData(StringBuilder line, PacketDirection dir, Packet132TileEntityData packet) {
        logCoordsBlockXYZ(line, packet.xPosition, packet.yPosition, packet.zPosition);
        line.append(" action=").append(packet.actionType);
        line.append(" data=");
        logNBTTagCompound(line, packet.customParam1);
        if (packet.actionType == 1) {
            line.append(" spawnertype=");
            line.append(packet.customParam1.getString("EntityId"));
        }
    }

    @PacketLogger(id=200, hex=0xC8)
    public void logPacketStatistic(StringBuilder line, PacketDirection dir, Packet200Statistic packet) {
        line.append("statid=").append(packet.statisticId);
        line.append(" amount=").append(packet.amount);
    }

    @PacketLogger(id=201, hex=0xC9)
    public void logPacketPlayerInfo(StringBuilder line, PacketDirection dir, Packet201PlayerInfo packet) {
        line.append("username=");
        logString(line, packet.playerName);
        line.append(" online=").append(packet.isConnected);
        line.append(" ping=").append(packet.ping);
    }

    @PacketLogger(id=202, hex=0xCA)
    public void logPacketPlayerAbilities(StringBuilder line, PacketDirection dir, Packet202PlayerAbilities packet) {
        line.append("invulnerable=").append(packet.getDisableDamage());
        line.append(" isflying=").append(packet.getFlying());
        line.append(" canfly=").append(packet.getAllowFlying());
        line.append(" iscreative=").append(packet.isCreativeMode());
        line.append(" flyspeed=").append(packet.getFlySpeed());
        // walk speed appears to be sent by the client but ignored if received from server
        line.append(" walkspeed=").append(Util.getFieldByType(packet, float.class, 1));
    }

    @PacketLogger(id=203, hex=0xCB)
    public void logPacketAutoComplete(StringBuilder line, PacketDirection dir, Packet203AutoComplete packet) {
        logString(line, packet.getText());
    }

    @PacketLogger(id=204, hex=0xCC)
    public void logPacketClientInfo(StringBuilder line, PacketDirection dir, Packet204ClientInfo packet) {
        line.append("locale=");
        logString(line, packet.getLanguage());
        line.append(" viewdistance=");
        CodeTable.viewDistance.log(line, packet.getRenderDistance());
        line.append(" chat=");
        CodeTable.chatMode.log(line, packet.getChatVisibility());
        line.append(" color=").append(packet.getChatColours());
        line.append(" difficulty=");
        CodeTable.difficulty.log(line, packet.getDifficulty());
    }

    @PacketLogger(id=205, hex=0xCD)
    public void logPacketClientCommand(StringBuilder line, PacketDirection dir, Packet205ClientCommand packet) {
        CodeTable.clientStatus.log(line, packet.forceRespawn);
    }

    @PacketLogger(id=250, hex=0xFA)
    public void logPacketCustomPayload(StringBuilder line, PacketDirection dir, Packet250CustomPayload packet) {
        line.append("channel=");
        logString(line, packet.channel);
        line.append(" length=").append(packet.data.length);
        line.append(" data=");
        logByteArrayAsString(line, packet.data);
        line.append(" hexdata=");
        logByteArrayHexDump(line, packet.data);
    }

    @PacketLogger(id=252, hex=0xFC)
    public void logPacketSharedKey(StringBuilder line, PacketDirection dir, Packet252SharedKey packet) {
        // This packet's data fields are private, obfuscated, and lack accessors... reflection time
        line.append("sharedsecret=");
        logByteArrayHexDump(line, (byte[]) Util.getFieldByType(packet, byte[].class, 0));
        line.append(" verifytoken=");
        logByteArrayHexDump(line, (byte[]) Util.getFieldByType(packet, byte[].class, 1));
    }

    @PacketLogger(id=253, hex=0xFD)
    public void logPacketServerAuthData(StringBuilder line, PacketDirection dir, Packet253ServerAuthData packet) {
        line.append("serverid=");
        logString(line, packet.getServerId());
        line.append(" publickey=");
        line.append(packet.getPublicKey().toString());
        line.append(" verifytoken=");
        logByteArrayHexDump(line, packet.getVerifyToken());
    }

    @PacketLogger(id=254, hex=0xFE)
    public void logPacketServerPing(StringBuilder line, PacketDirection dir, Packet254ServerPing packet) {
        // no payload
    }

    @PacketLogger(id=255, hex=0xFF)
    public void logPacketKickDisconnect(StringBuilder line, PacketDirection dir, Packet255KickDisconnect packet) {
        logString(line, packet.reason);
    }
}
