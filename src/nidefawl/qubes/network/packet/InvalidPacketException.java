package nidefawl.qubes.network.packet;

@SuppressWarnings({"serial","rawtypes"})
public class InvalidPacketException extends Exception {

    private Class clazz;

    public InvalidPacketException(Class clazz, Exception e) {
        super(e);
        this.clazz = clazz;
    }

    public InvalidPacketException(String string) {
        super(string);
    }

    public InvalidPacketException(String string, Exception e) {
        super(string, e);
    }

    public Class getClazz() {
        return clazz;
    }
}
