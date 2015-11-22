package nidefawl.qubes.assets;

public abstract class Asset {
    private AssetPack pack;
    public void setPack(AssetPack pack) {
        this.pack = pack;
    }
    /**
     * @return the pack
     */
    public AssetPack getPack() {
        return this.pack;
    }
}
