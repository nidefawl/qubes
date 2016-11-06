package nidefawl.qubes.world.biomes;

import java.io.DataInput;
import java.io.DataOutput;

public abstract class IBiomeSettings {
    public static IBiomeSettings fromId(int id) {
        BiomeManagerType enumType = BiomeManagerType.fromId(id);
        switch (enumType) {
            case HEX:
                return new BiomeSettingsHex();
            case SINGLE:
                return new BiomeSettingsStatic();
        }
        return new BiomeSettingsStatic();
    }
    public static int getId(IBiomeSettings biomeSettings) {
        BiomeManagerType enumType = biomeSettings.getType();
        switch (enumType) {
            case HEX:
                return 1;
            case SINGLE:
                return 0;
        }
        return 0;
    }
    public abstract BiomeManagerType getType();
    public void read(DataInput stream) {
    }
    public void write(DataOutput stream) {
    }
}
