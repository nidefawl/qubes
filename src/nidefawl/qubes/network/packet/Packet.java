package nidefawl.qubes.network.packet;

import java.io.*;
import java.util.Map;

import com.google.common.collect.Maps;

import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.item.BlockStack;
import nidefawl.qubes.item.ItemStack;
import nidefawl.qubes.network.Handler;


public abstract class Packet {
    public final static int                       MAX_ID       = 255;
    public final static int                       MAX_STR_LEN       = 32*1024;
    public static final int NET_VERSION = 3;
    private static int NEXT_PACKET_ID;
    
    @SuppressWarnings("unchecked")
    public final static Class<? extends Packet>[] packets      = new Class[MAX_ID + 1];
    public final static boolean[]                 sentByServer = new boolean[MAX_ID + 1];
    public final static boolean[]                 sentByClient = new boolean[MAX_ID + 1];
    static Map<Class<?>, Integer> classToIDMap = Maps.newHashMap();
    
    static {
        register(PacketPing.class, true, true);
        register(PacketHandshake.class, true, true);
        register(PacketDisconnect.class, true, true);
        register(PacketAuth.class, true, true);
        register(PacketSSpawnInWorld.class, true, false);
        register(PacketCMovement.class, true, true);
        register(PacketSChunkData.class, true, false);
        register(PacketCSetBlock.class, false, true);
        register(PacketCSetBlocks.class, false, true);
        register(PacketSSetBlock.class, true, false);
        register(PacketSSetBlocks.class, true, false);
        register(PacketSLightChunk.class, true, false);
        register(PacketSTrackChunk.class, true, false);
        register(PacketCSettings.class, false, true);
        register(PacketCSwitchWorld.class, false, true);
        register(PacketChatMessage.class, true, true);
        register(PacketChatChannels.class, true, false);
        register(PacketSWorldTime.class, true, false);
        register(PacketSTeleport.class, true, false);
        register(PacketSyncBlocks.class, true, true);
        register(PacketCTeleportAck.class, false, true);
        register(PacketSEntityTrack.class, true, false);
        register(PacketSEntityUnTrack.class, true, false);
        register(PacketSEntityMove.class, true, false);
        register(PacketSWorldBiomes.class, true, false);
        register(PacketCDigState.class, false, true);
        register(PacketSDigState.class, true, false);
        register(PacketSDebugBB.class, true, false);
        register(PacketCInvClick.class, true, true);
        register(PacketSInvSync.class, true, false);
        register(PacketSInvSyncIncr.class, true, false);
        register(PacketCCrafting.class, false, true);
        register(PacketSCraftingProgress.class, true, true);
        register(PacketSInvCarried.class, true, false);
        register(PacketCInvTransaction.class, false, true);
        register(PacketSDebugPath.class, true, false);
        register(PacketCSetProperty.class, false, true);
        register(PacketSEntityProperties.class, true, false);
    }
    
    private int id;
    
    public Packet() {
        this.id = classToIDMap.get(getClass());
    }

    static Packet lastSuccess = null;
    public static Packet read(final DataInput stream) throws IOException, InvalidPacketException {
        int t = stream.readUnsignedByte();
        Packet p;
        try {
            p = makePacket(t);
            if (p == null) {
                throw new InvalidPacketException("Invalid packet "+t);
            }
            p.readPacket(stream);
            lastSuccess = p;
        } catch (InvalidPacketException e) {
            throw new InvalidPacketException("Invalid packet "+t+"/"+e.getClazz()+": "+e.getMessage()+ " Last success "+lastSuccess, e);
        }
        return p;
    }

    public static void write(Packet p, DataOutput stream) throws IOException {
        stream.writeByte(p.getID());
        p.writePacket(stream);
    }


    private static Packet makePacket(int t) throws InvalidPacketException {
        return makePacket(packets[t]);
    }

    public abstract void readPacket(DataInput stream) throws IOException;

    public abstract void writePacket(DataOutput stream) throws IOException;

    public abstract void handle(Handler h);

    private static Packet makePacket(final Class<? extends Packet> c) throws InvalidPacketException {
        Packet p = null;
        if (c != null) {
            try {
                p = c.newInstance();
            } catch (final Exception e) {
                throw new InvalidPacketException(c, e);
            }   
        }
        return p;
    }

    private int getID() {
        return this.id;
    }

    private static void register(final Class<? extends Packet> c, final boolean ss, final boolean sc) {
        int id = NEXT_PACKET_ID++;
        packets[id] = c;
        sentByServer[id] = ss;
        sentByClient[id] = sc;
        classToIDMap.put(c, id);
    }
    
    public boolean handleSynchronized() {
        return true;
    }

    public static void init() {
    }
    public static String readString(DataInput stream) throws IOException {
        return readString(stream, -1);
    }

    public static String readString(DataInput stream, int i) throws IOException {
		int len = stream.readShort();
		int maxLen = i > 0 ? i : MAX_STR_LEN;
		if (len >= maxLen) {
			throw new IOException("Maximum string length exceeded ("+len+" >= "+maxLen+")");
		}
		byte[] str = new byte[len];
		stream.readFully(str);
		return new String(str, "UTF-16");
	}
    public static void writeString(String s, DataOutput stream) throws IOException {
		byte[] str = s.getBytes("UTF-16");
		int len = str.length;
		if (len >= MAX_STR_LEN) {
			throw new IOException("Maximum string length exceeded ("+len+" >= "+MAX_STR_LEN+")");
		} 
		stream.writeShort(len);
		stream.write(str);
	}

    public static BaseStack readStack(DataInput in) throws IOException {
        int i = in.readUnsignedByte();
        switch (i) {
            default:
            case 0:
                return null;
            case 1:
                ItemStack itemStack = new ItemStack();
                itemStack.read(in);
                return itemStack;
            case 2:
                BlockStack blockStack = new BlockStack();
                blockStack.read(in);
                return blockStack;
        }
    }

    public static void writeStack(BaseStack stack, DataOutput out) throws IOException {
        
        if (stack != null && stack.isItem()) {
            out.writeByte(1);
            stack.write(out);
            return;
        }
        if (stack != null && stack.isBlock()) {
            out.writeByte(2);
            stack.write(out);
            return;
        }
        out.writeByte(0);
    }
}
