package nidefawl.qubes.world.biomes;

public enum BiomeManagerType {
    SINGLE, HEX;

    public static BiomeManagerType fromId(int id) {
        return BiomeManagerType.values()[id];
    }
}
