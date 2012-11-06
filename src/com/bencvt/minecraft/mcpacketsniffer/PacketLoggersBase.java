package com.bencvt.minecraft.mcpacketsniffer;

import java.net.Socket;
import java.util.List;
import java.util.logging.Level;

import net.minecraft.client.Minecraft;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityList;
import net.minecraft.src.EntityOtherPlayerMP;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EnumGameType;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MemoryConnection;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.INetworkManager;
import net.minecraft.src.Packet;
import net.minecraft.src.TcpConnection;
import net.minecraft.src.WatchableObject;
import net.minecraft.src.WorldType;

/**
 * Various helper methods for converting stuff to strings.
 */
public abstract class PacketLoggersBase {
    /**
     * Get the packet's self-reported payload size. This is known to be inaccurate for some packets,
     * e.g. Packet202PlayerAbilities and anything with metadata.
     */
    public static void logApproximatePacketPayloadSize(StringBuilder line, Packet packet) {
        line.append("size~=").append(packet.getPacketSize());
    }

    /**
     * Output a timestamp, e.g., "2012-05-18 13:39:48.980"
     */
    public static void logTimestamp(StringBuilder line, long timestamp) {
        String ts = new java.sql.Timestamp(timestamp).toString();
        line.append(ts);
        for (int pad = ts.length(); pad < 23; pad++) {
            line.append('0');
        }
    }
    public static String timestampToString(long timestamp) {
        StringBuilder b = new StringBuilder();
        logTimestamp(b, timestamp);
        return b.toString();
    }

    public static void logConnectionAddress(StringBuilder line, INetworkManager connection) {
        if (connection instanceof MemoryConnection) {
            line.append("singleplayer");
            return;
        }
        Socket socket = ((TcpConnection) connection).getSocket();
        // Use toString() and parse the output so we don't force a reverse DNS lookup.
        // InetSocketAddress.getHostString() would eliminate the need for the extra
        // parsing, but that's a Java 7 thing. Minecraft uses 6.
        String[] parts = socket.getInetAddress().toString().split("/", 2); // "hostname/ip" or "/ip"
        line.append(parts[0].isEmpty() ? parts[1] : parts[0]);
        // Only include the port number if it's non-standard.
        if (socket.getPort() != 25565) {
            line.append(':').append(socket.getPort());
        }
    }

    public static String connectionAddressToString(INetworkManager connection) {
        StringBuilder b = new StringBuilder();
        logConnectionAddress(b, connection);
        return b.toString().replace(' ', '_').replace(':', '_').replaceAll("[^A-Za-z0-9\\-_\\.]+", "");
    }

    //
    // String escaping
    //

    public static void logString(StringBuilder line, String s) {
        line.append('"');
        for (int i = 0; i < s.length(); i++) {
            // unprintable or unicode
            char ch = s.charAt(i);
            if (ch > 0x7f || ch < 0x20) {
                if (ch == '\u00A7' && !Controller.getOptions().colorEscape.isEmpty()) {
                    line.append(Controller.getOptions().colorEscape);
                } else if (ch == '\n') {
                    line.append("\\n");
                } else if (ch == '\r') {
                    line.append("\\r");
                } else if (ch == '\t') {
                    line.append("\\t");
                } else if (ch == '\f') {
                    line.append("\\f");
                } else if (ch == '\b') {
                    line.append("\\b");
                } else {
                    line.append(String.format("\\u%04X", (int) ch));
                }
            } else if (ch == '"') {
                line.append("\\\"");
            } else if (ch == '\\') {
                line.append("\\\\");
            } else {
                line.append(ch);
            }
        }
        line.append('"');
    }

    //
    // Raw data
    //

    public static void logByteArrayAsString(StringBuilder line, byte[] data) {
        logString(line, new String(data));
    }

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    public static void logByteArrayHexDump(StringBuilder line, byte[] data) {
        line.append('[');
        for (int i = 0; i < data.length; i++) {
            if (i > 0) {
                line.append(' ');
            }
            line.append(HEX[(data[i] & 0xf0) >> 4]);
            line.append(HEX[data[i] & 0x0f]);
        }
        line.append(']');
    }

    public static void logNBTTagCompound(StringBuilder line, NBTTagCompound tag) {
        // TODO print the contents!
        line.append("NBTTagCompound(");
        line.append(tag.toString());
        line.append(')');
    }

    //
    // Game dimensions
    //

    /**
     * @return "(rX,rZ;cX,cZ;bX,bY,bZ)", where:
     *   rX,rZ are region coords
     *   cX,cZ are chunk coords
     *   bX,bY,bZ are block coords
     *   Region and chunk coords may be omitted per preferences.
     */
    private static void logCoordsWork(StringBuilder line, int blockX, int blockY, int blockZ) {
        line.append('(');
        if (Controller.getOptions().coordsIncludeRegion) {
            line.append(blockX >> 9).append(',');
            line.append(blockZ >> 9).append(';');
        }
        if (Controller.getOptions().coordsIncludeChunk) {
            line.append(blockX >> 4).append(',');
            line.append(blockZ >> 4).append(';');
        }
        line.append(blockX).append(',');
        line.append(blockY).append(',');
        line.append(blockZ).append(')');
    }
    public static void logCoordsBlockXYZ(StringBuilder line, int blockX, int blockY, int blockZ) {
        logCoordsWork(line, blockX, blockY, blockZ);
    }
    public static void logCoordsAbsoluteIntegerXYZ(StringBuilder line, int absX, int absY, int absZ) {
        logCoordsBlockXYZ(line, absX / 32, absY / 32, absZ / 32);
    }
    public static void logCoordsChunkXZ(StringBuilder line, int chunkX, int chunkZ) {
        logCoordsWork(line, chunkX << 4, 0, chunkZ << 4);
    }

    public static void logAngleFloat(StringBuilder line, float angle) {
        line.append(String.format("%.1f", angle));
    }
    public static void logAngleFloat(StringBuilder line, float yaw, float pitch) {
        line.append(String.format("(%.1f,%.1f)", yaw, pitch));
    }
    public static void logAngleByte(StringBuilder line, byte angle) {
        logAngleFloat(line,
                angle * 360F / 256F);
    }
    public static void logAngleByte(StringBuilder line, byte yaw, byte pitch) {
        logAngleFloat(line,
                yaw * 360F / 256F,
                pitch * 360F / 256F);
    }


    public static void logVelocityFloat(StringBuilder line, float motionX, float motionY, float motionZ) {
        line.append(String.format("(%.3f,%.3f,%.3f)", motionX, motionY, motionZ));
    }

    public static void logVelocityShort(StringBuilder line, int motionX, int motionY, int motionZ) {
        logVelocityFloat(line, motionX / 8000.0F, motionY / 8000.0F, motionZ / 8000.0F);
    }

    public static void logRelativeMove(StringBuilder line, byte absX, byte absY, byte absZ) {
        line.append('(');
        line.append(String.format("%.3f", absX / 32.0)).append(',');
        line.append(String.format("%.3f", absY / 32.0)).append(',');
        line.append(String.format("%.3f", absZ / 32.0)).append(')');
    }

    //
    // Game objects
    //

    public static void logWorldType(StringBuilder line, WorldType worldType) {
        if (worldType == null) {
            line.append("<null>");
        } else {
            line.append(worldType.getWorldTypeName());
        }
    }

    public static void logGameMode(StringBuilder line, EnumGameType gameMode) {
        if (gameMode == null) {
            line.append("<null>");
        } else {
            line.append(gameMode.getID());
            line.append('(').append(gameMode.getName()).append(')');
        }
    }

    public static void logItemType(StringBuilder line, int itemId) {
        line.append(itemId);
        if (itemId == 0) {
            return;
        }
        line.append('(');
        Item item = null;
        if (itemId >= 0 && itemId < Item.itemsList.length) {
            item = Item.itemsList[itemId];
        }
        if (item == null) {
            line.append('?');
        } else {
            line.append(item.getItemName());
        }
        line.append(')');
    }

    public static void logItemStack(StringBuilder line, ItemStack itemStack) {
        line.append("ItemStack(");
        if (itemStack == null) {
            line.append("<null>)");
            return;
        }
        logItemType(line, itemStack.itemID);
        line.append(':').append(itemStack.getItemDamage());
        line.append(",x").append(itemStack.stackSize);
        if (itemStack.stackTagCompound != null) {
            line.append(',');
            logNBTTagCompound(line, itemStack.stackTagCompound);
        }
        line.append(')');
    }

    public static void logEntityType(StringBuilder line, int entityType) {
        line.append(entityType).append('(');
        String name = EntityList.getStringFromID(entityType);
        if (name == null) {
            line.append('?');
        } else {
            line.append(name);
        }
        line.append(')');
    }
    public static void logEntityType(StringBuilder line, Entity entity) {
        if (entity == null) {
            line.append("<null>");
            return;
        }
        String name = EntityList.getEntityString(entity);
        if (name == null && entity instanceof EntityOtherPlayerMP) {
            line.append("<other player>(").append(((EntityOtherPlayerMP) entity).username).append(')');
        } else if (name == null && entity instanceof EntityPlayer) {
            line.append("<player>(").append(((EntityPlayer) entity).username).append(')');
        } else if (name == null) {
            line.append("<unlisted>(?)");
        } else {
            line.append(EntityList.getEntityID(entity)).append('(').append(name).append(')');
        }
    }

    public static void logExistingEntity(StringBuilder line, int entityId) {
        Entity entity = null;
        if (Minecraft.getMinecraft().theWorld != null) {
            entity = Minecraft.getMinecraft().theWorld.getEntityByID(entityId);
        }
        line.append("{eid=").append(entityId);
        if (entity == null) {
            line.append('}');
            return;
        }
        line.append(" type=");
        logEntityType(line, entity);
        if (entity != null) {
            line.append(" pos=");
            logCoordsBlockXYZ(line, (int) entity.posX, (int) entity.posY, (int) entity.posZ);
        }
        line.append('}');
    }

    /**
     * @see http://mc.kev009.com/Entities
     */
    public static void logEntityMetadata(StringBuilder line, List<WatchableObject> metadata) {
        line.append('[');
        for (int i = 0; i < metadata.size(); i++) {
            if (i > 0) {
                line.append(", ");
            }
            WatchableObject w = metadata.get(i);
            switch (w.getDataValueId()) {
            case 0:
                line.append("flags=");
                line.append(w.getObject());
                break;
            case 1:
                line.append("drown=");
                line.append(w.getObject());
                break;
            case 8:
                line.append("potion=");
                line.append(w.getObject());
                break;
            case 12:
                line.append("animal=");
                line.append(w.getObject());
                break;
            default:
                line.append(w.getDataValueId());
                line.append('<');
                CodeTable.entityMetadataType.log(line, w.getObjectType(), false);
                line.append(">=");
                logString(line, w.getObject().toString());
            }
        }
        line.append(']');
    }
}
