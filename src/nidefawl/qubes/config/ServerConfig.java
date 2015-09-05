package nidefawl.qubes.config;

public class ServerConfig extends AbstractYMLConfig {
	public ServerConfig() {
		super(true);
	}
	
	public int port;
	public int packetTimeout;
    public int chunkCompressionLevel;
	
	@Override
	public void setDefaults() {
        port = 21087;
        packetTimeout = 5000;
        chunkCompressionLevel = 4;
	}

	@Override
	public void load() {
        port = getInt("port", port);
        chunkCompressionLevel = getInt("chunkCompressionLevel", chunkCompressionLevel);
        packetTimeout = getInt("packetTimeout", packetTimeout);
	}

    @Override
    public void save() {
        setInt("port", port);
        setInt("packetTimeout", packetTimeout);
        setInt("chunkCompressionLevel", chunkCompressionLevel);
    }

}
