package nidefawl.qubes.config;

public class ServerConfig extends AbstractYMLConfig {
	public ServerConfig() {
		super(true);
	}
	
	public int port;
	public String listenAddr;
	public int packetTimeout;
    public int chunkCompressionLevel;
	
	@Override
	public void setDefaults() {
	    listenAddr = "localhost";
        port = 21087;
        packetTimeout = 5000;
        chunkCompressionLevel = 4;
	}

	@Override
	public void load() {
        listenAddr = getString("listenAddr", listenAddr);
        port = getInt("port", port);
        chunkCompressionLevel = getInt("chunkCompressionLevel", chunkCompressionLevel);
        packetTimeout = getInt("packetTimeout", packetTimeout);
	}

    @Override
    public void save() {
        setString("listenAddr", listenAddr);
        setInt("port", port);
        setInt("packetTimeout", packetTimeout);
        setInt("chunkCompressionLevel", chunkCompressionLevel);
    }

}
