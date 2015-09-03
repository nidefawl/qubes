package nidefawl.qubes.network.packet;

import java.io.*;

import nidefawl.qubes.network.IHandler;


public abstract class Packet {
    public final static int                       MAX_ID       = 255;
    public final static int                       MAX_STR_LEN       = 32*1024;
    
    @SuppressWarnings("unchecked")
    public final static Class<? extends Packet>[] packets      = new Class[MAX_ID + 1];
    public final static boolean[]                 sentByServer = new boolean[MAX_ID + 1];
    public final static boolean[]                 sentByClient = new boolean[MAX_ID + 1];
    static {
        register(PacketPing.class, 1, true, true);
        register(PacketHandshake.class, 2, true, true);
        register(PacketDisconnect.class, 3, true, true);
    }
    public Packet() {
    }

    public static Packet read(final DataInput stream) throws IOException {
        int t = stream.readUnsignedByte();
        Packet p;
        try {
            p = makePacket(t);
            if (p == null) {
                throw new IOException("Invalid packet "+t);
            }
            p.readPacket(stream);
        } catch (InvalidPacketException e) {
            throw new IOException("Invalid packet "+t+"/"+e.getClazz()+": "+e.getMessage());
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

    public abstract int getID();

    public abstract void handle(IHandler h);


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


    private static void register(final Class<? extends Packet> c, int id, final boolean ss, final boolean sc) {
        packets[id] = c;
        sentByServer[id] = ss;
        sentByClient[id] = sc;
    }
    
    public boolean handleSynchronized() {
        return true;
    }

    public static void init() {
    }

    public static String readString(DataInput stream) throws IOException {
		int len = stream.readShort();
		if (len >= MAX_STR_LEN) {
			throw new IOException("Maximum string length exceeded ("+len+" >= "+MAX_STR_LEN+")");
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
}
