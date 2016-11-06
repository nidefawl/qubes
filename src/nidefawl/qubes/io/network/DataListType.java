package nidefawl.qubes.io.network;

import nidefawl.qubes.network.StreamIO;
import nidefawl.qubes.util.GameError;

public enum DataListType {
    WORLDS(0, WorldInfo.class);
    final int id;
    private Class<? extends StreamIO> clazz;
    DataListType(int id, Class<? extends StreamIO> clazz) {
        this.id =id;
        this.clazz = clazz;
    }
    public static DataListType byId(int id) {
        DataListType[] values = values();
        return values[id];
    }
    public StreamIO makeNew() {
        try {
            return this.clazz.newInstance();
        } catch (Exception e) {
            throw new GameError(e);
        }
    }
    public int getId() {
        return this.id;
    }
}