package nidefawl.qubes.nbt;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import nidefawl.qubes.vec.Vec3D;
import nidefawl.qubes.vec.Vector3f;

public abstract class Tag {

    public static enum TagType {
        END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTEARRAY, STRING, LIST, COMPOUND, VEC3, UUID;
        public int getID() {
            return ordinal();
        }

        public static TagType fromID(int type) {
            return values()[type];
        }
    }

    private static final int MAX_STR_LEN              = 32 * 1024;
    private static final int MAX_BYTE_ARR_SIZE        = 1024 * 1024;
    private static final int MAX_LIST_LEN             = 1024 * 32;
    private static final int MAX_COMPOUND_TAG_ENTRIES = 1024;

    String name = "";

    public static class End extends Tag {

        @Override
        public Object getValue() {
            return null;
        }

        @Override
        public TagType getType() {
            return TagType.END;
        }

        @Override
        protected void writeData(DataOutput out) throws IOException {
        }

        @Override
        protected void readData(DataInput in, TagReadLimiter limit) throws IOException {
        }

    }

    public static class Byte extends Tag {

        byte byteVal;

        public Byte(int b) {
            this.byteVal = (byte) b;
        }

        public Byte() {
        }

        @Override
        protected void writeData(DataOutput out) throws IOException {
            out.writeByte(this.byteVal);
        }

        @Override
        public TagType getType() {
            return TagType.BYTE;
        }

        @Override
        public Object getValue() {
            return this.byteVal;
        }

        @Override
        protected void readData(DataInput in, TagReadLimiter limit) throws IOException {
            this.byteVal = in.readByte();
            limit.add(1);
        }

        public byte getByte() {
            return byteVal;
        }

        public void setByte(byte byteVal) {
            this.byteVal = byteVal;
        }
    }

    public static class ByteArray extends Tag {

        byte[] data;

        public ByteArray(byte[] b) {
            this.data = b;
        }

        public ByteArray() {
        }

        @Override
        protected void writeData(DataOutput out) throws IOException {
            out.writeInt(data.length);
            out.write(this.data);
        }

        @Override
        public TagType getType() {
            return TagType.BYTEARRAY;
        }

        @Override
        public Object getValue() {
            return this.data;
        }

        @Override
        protected void readData(DataInput in, TagReadLimiter limit) throws IOException {
            int len = in.readInt();
            limit.add(4);
            if (len > MAX_BYTE_ARR_SIZE) {
                throw new IOException("Maximum byte[] length exceeded (" + len + " >= " + MAX_BYTE_ARR_SIZE + ")");
            }
            this.data = new byte[len];
            in.readFully(this.data);
            limit.add(len);
        }

        public void setArray(byte[] d) {
            this.data = d;
        }

        public byte[] getArray() {
            return data;
        }
    }

    public static class Double extends Tag {

        public Double(double val) {
            this.doubleVal = val;
        }

        public Double() {
        }

        double doubleVal;

        @Override
        protected void writeData(DataOutput out) throws IOException {
            out.writeDouble(this.doubleVal);
        }

        @Override
        public TagType getType() {
            return TagType.DOUBLE;
        }

        @Override
        public Object getValue() {
            return this.doubleVal;
        }

        @Override
        protected void readData(DataInput in, TagReadLimiter limit) throws IOException {
            this.doubleVal = in.readDouble();
            limit.add(8);
        }

        public void setDouble(double d) {
            this.doubleVal = d;
        }

        public double getDouble() {
            return doubleVal;
        }

    }

    public static class Vec3Tag extends Tag {

        public Vec3Tag(Vector3f val) {
            this.vec3 = val;
        }

        public Vec3Tag() {
            this(new Vector3f());
        }

        Vector3f vec3;

        @Override
        protected void writeData(DataOutput out) throws IOException {
            out.writeFloat(this.vec3.x);
            out.writeFloat(this.vec3.y);
            out.writeFloat(this.vec3.z);
        }

        @Override
        public TagType getType() {
            return TagType.VEC3;
        }

        @Override
        public Object getValue() {
            return this.vec3;
        }

        @Override
        protected void readData(DataInput in, TagReadLimiter limit) throws IOException {
            this.vec3 = new Vector3f(in.readFloat(), in.readFloat(), in.readFloat());
            limit.add(12);
        }

        public void setVec3(Vector3f vec3) {
            this.vec3 = vec3;
        }

        public void setVec3(Vec3D vec3) {
            this.vec3 = new Vector3f(vec3);
        }

        public Vector3f getVec3() {
            return vec3;
        }

    }

    public static class TagList extends Tag {

        List<Tag>       data = new ArrayList<>();
        private TagType tagType;

        @Override
        protected void writeData(DataOutput out) throws IOException {
            int len = data.size();
            out.writeInt(len);
            out.writeByte(tagType.getID());
            for (int a = 0; a < len; a++) {
                data.get(a).writeData(out);
            }
        }

        @Override
        public TagType getType() {
            return TagType.LIST;
        }

        @Override
        public Object getValue() {
            return this.data;
        }

        @Override
        protected void readData(DataInput in, TagReadLimiter limit) throws IOException {
            int len = in.readInt();
            limit.add(4);
            if (len > MAX_LIST_LEN) {
                throw new IOException("Maximum list length exceeded (" + len + " >= " + MAX_LIST_LEN + ")");
            }
            this.tagType = TagType.fromID(in.readByte() & 0xFF);
            limit.add(1);
            limit.push();
            for (int a = 0; a < len; a++) {
                Tag tag = newFromType(tagType);
                tag.readData(in, limit);
                this.data.add(tag);
            }
            limit.pop();
        }

        public List<Tag> getList() {
            return data;
        }

        public void add(Tag t) {
            if (this.tagType == null) {
                this.tagType = t.getType();
            } else if (t.getType() != this.tagType) {
                throw new IllegalArgumentException("Cannot add a tag of type " + t.getType() + " to list of tag-type " + this.tagType);
            }
            this.data.add(t);
        }

        public TagType getListTagType() {
            return tagType;
        }

    }

    public static class Compound extends Tag {

        Map<String, Tag> data = new HashMap<>();

        @Override
        public TagType getType() {
            return TagType.COMPOUND;
        }

        @Override
        public Object getValue() {
            return this.data;
        }

        @Override
        protected void writeData(DataOutput out) throws IOException {
            Iterator<Map.Entry<String, Tag>> it = this.data.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, Tag> entry = it.next();
                Tag tagEntry = entry.getValue();
                out.writeByte(tagEntry.getType().getID());
                writeString(entry.getKey(), out);
                entry.getValue().writeData(out);
            }
            out.writeByte(0);
        }

        @Override
        protected void readData(DataInput in, TagReadLimiter l) throws IOException {
            int limit = MAX_COMPOUND_TAG_ENTRIES;
            l.push();
            while (true) {
                if (limit-- < 0) {
                    throw new IOException("Maximum list compound tag size exceeded");
                }
                int tagTypeID = in.readByte() & 0xFF;
                l.add(1);
                if (tagTypeID == 0) {
                    break;
                }
                TagType tagType = TagType.fromID(tagTypeID);
                Tag tag = newFromType(tagType);
                tag.setName(readString(in));
                l.add(2 + tag.getName().length() * 2);
                tag.readData(in, l);
                this.data.put(tag.getName(), tag);
            }
            l.pop();
        }

        public Map<String, Tag> getMap() {
            return data;
        }

        public void set(String s, Tag t) {
            t.setName(s);
            ;
            this.data.put(s, t);
        }

        public void setInt(String string, int x) {
            this.set(string, new Tag.Int(x));
        }

        public void setByteArray(String string, byte[] b) {
            this.set(string, new Tag.ByteArray(b));
        }

        public ByteArray getByteArray(String string) {
            Tag t = this.get(string);
            return (ByteArray) t;
        }

        private Tag get(String string) {
            return this.data.get(string);
        }

        public double getDouble(String string) {
            Tag t = this.data.get(string);
            return t instanceof Tag.Double ? ((Tag.Double) t).getDouble() : 0;
        }

        public Vector3f getVec3(String string) {
            Tag t = this.data.get(string);
            return t instanceof Tag.Vec3Tag ? ((Tag.Vec3Tag) t).getVec3() : new Vector3f();
        }

        public void setVec3(String string, Vector3f pos) {
            this.data.put(string, new Tag.Vec3Tag(pos));
        }

        public UUID getUUID(String string) {
            Tag t = this.data.get(string);
            return t instanceof Tag.UUIDTag ? ((Tag.UUIDTag) t).getUUID() : null;
        }

        public void setUUID(String string, UUID uuid) {
            this.data.put(string, new Tag.UUIDTag(uuid));
        }

        public int getByte(String string) {
            Tag t = this.data.get(string);
            return t instanceof Tag.Byte ? ((Tag.Byte) t).getByte()&0xFF : 0;
        }

        public void setByte(String string, int b) {
            this.data.put(string, new Tag.Byte(b));
        }


        public void setBoolean(String string, boolean bool) {
            this.data.put(string, new Tag.Byte(bool?1:0));
        }

        public int getInt(String string) {
            Tag t = this.data.get(string);
            return t instanceof Tag.Int ? ((Tag.Int) t).getInt() : 0;
        }

        public boolean getBoolean(String string) {
            return getByte(string) > 0;
        }
    }

    public static class Float extends Tag {

        float floatVal;

        @Override
        protected void writeData(DataOutput out) throws IOException {
            out.writeFloat(this.floatVal);
        }

        @Override
        public TagType getType() {
            return TagType.FLOAT;
        }

        @Override
        public Object getValue() {
            return this.floatVal;
        }

        @Override
        protected void readData(DataInput in, TagReadLimiter limit) throws IOException {
            this.floatVal = in.readFloat();
            limit.add(4);
        }

        public float getFloat() {
            return floatVal;
        }

        public void setFloat(float floatVal) {
            this.floatVal = floatVal;
        }
    }

    public static class Int extends Tag {

        int data;

        public Int(int data) {
            this.data = data;
        }

        public Int() {
        }

        @Override
        protected void writeData(DataOutput out) throws IOException {
            out.writeInt(this.data);
        }

        @Override
        public TagType getType() {
            return TagType.INT;
        }

        @Override
        public Object getValue() {
            return this.data;
        }

        @Override
        protected void readData(DataInput in, TagReadLimiter limit) throws IOException {
            this.data = in.readInt();
            limit.add(4);
        }

        public int getInt() {
            return data;
        }

        public void setInt(int data) {
            this.data = data;
        }
    }

    public static class StringTag extends Tag {

        String data;

        public StringTag(String data) {
            this.data = data;
        }

        public StringTag() {
        }

        @Override
        public TagType getType() {
            return TagType.STRING;
        }

        @Override
        public Object getValue() {
            return this.data;
        }

        @Override
        protected void readData(DataInput in, TagReadLimiter limit) throws IOException {
            this.data = readString(in);
            limit.add(2 + this.data.length() * 2);
        }

        @Override
        protected void writeData(DataOutput out) throws IOException {
            writeString(data, out);
        }

        public void setString(String data) {
            this.data = data;
        }

        public String getString() {
            return data;
        }
    }

    public static class Long extends Tag {

        long data;

        @Override
        protected void writeData(DataOutput out) throws IOException {
            out.writeLong(this.data);
        }

        @Override
        public TagType getType() {
            return TagType.LONG;
        }

        @Override
        public Object getValue() {
            return this.data;
        }

        @Override
        protected void readData(DataInput in, TagReadLimiter limit) throws IOException {
            this.data = in.readLong();
            limit.add(8);
        }

        public long getLong() {
            return data;
        }

        public void setLong(long data) {
            this.data = data;
        }
    }

    public static class UUIDTag extends Tag {

        UUID data;

        public UUIDTag(UUID uuid) {
            this.data = uuid;
        }

        public UUIDTag() {
        }

        @Override
        protected void writeData(DataOutput out) throws IOException {
            out.writeLong(this.data.getMostSignificantBits());
            out.writeLong(this.data.getLeastSignificantBits());
        }

        @Override
        public TagType getType() {
            return TagType.UUID;
        }

        @Override
        public Object getValue() {
            return this.data;
        }

        @Override
        protected void readData(DataInput in, TagReadLimiter limit) throws IOException {
            this.data = new UUID(in.readLong(), in.readLong());
            limit.add(8);
        }

        public UUID getUUID() {
            return data;
        }

        public void setUUID(UUID data) {
            this.data = data;
        }
    }
    public static class Short extends Tag {

        short data;

        @Override
        protected void writeData(DataOutput out) throws IOException {
            out.writeShort(this.data);
        }

        @Override
        public TagType getType() {
            return TagType.SHORT;
        }

        @Override
        public Object getValue() {
            return this.data;
        }

        @Override
        protected void readData(DataInput in, TagReadLimiter limit) throws IOException {
            this.data = in.readShort();
            limit.add(2);
        }

        public void setShort(short data) {
            this.data = data;
        }

        public short getShort() {
            return data;
        }
    }

    public String getName() {
        return name;
    }

    public abstract Object getValue();

    public abstract TagType getType();

    protected abstract void writeData(DataOutput out) throws IOException;

    protected abstract void readData(DataInput in, TagReadLimiter limit) throws IOException;

    public static void write(Tag tag, DataOutput out) throws IOException {
        out.writeByte(tag.getType().getID());
        writeString(tag.name, out);
        tag.writeData(out);
    }

    public static Tag read(DataInput in, TagReadLimiter limit) throws IOException {
        TagType tagType = TagType.fromID(in.readByte() & 0xFF);
        Tag tag = newFromType(tagType);
        limit.add(1);
        tag.name = readString(in);
        limit.add(2 + tag.name.length() * 2);
        limit.push();
        tag.readData(in, limit);
        limit.pop();
        return tag;
    }

    private static Tag newFromType(TagType tagType) {
        switch (tagType) {
            case END:
                return new End();
            case BYTE:
                return new Byte();
            case BYTEARRAY:
                return new ByteArray();
            case COMPOUND:
                return new Compound();
            case DOUBLE:
                return new Double();
            case FLOAT:
                return new Float();
            case INT:
                return new Int();
            case LIST:
                return new TagList();
            case LONG:
                return new Long();
            case SHORT:
                return new Short();
            case STRING:
                return new StringTag();
            case VEC3:
                return new Vec3Tag();
            case UUID:
                return new UUIDTag();
            default:
                break;
        }
        return null;
    }

    public static String readString(DataInput stream) throws IOException {
        int len = stream.readShort();
        if (len >= MAX_STR_LEN) {
            throw new IOException("Maximum string length exceeded (" + len + " >= " + MAX_STR_LEN + ")");
        }
        byte[] str = new byte[len];
        stream.readFully(str);
        return new String(str, "UTF-16");
    }

    public static void writeString(String s, DataOutput stream) throws IOException {
        byte[] str = s.getBytes("UTF-16");
        int len = str.length;
        if (len >= MAX_STR_LEN) {
            throw new IOException("Maximum string length exceeded (" + len + " >= " + MAX_STR_LEN + ")");
        }
        stream.writeShort(len);
        stream.write(str);
    }

    public void setName(String name) {
        this.name = name;
    }
}