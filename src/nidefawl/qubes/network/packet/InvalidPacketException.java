package nidefawl.qubes.network.packet;

public class InvalidPacketException extends Exception {

    private Class clazz;
    private Exception e;

    public InvalidPacketException(Class clazz, Exception e) {
        this.clazz = clazz;
        this.e = e;
    }
    public Class getClazz() {
        return clazz;
    }
    @Override
    public String getMessage() {
        return e.getMessage();
    }
}
