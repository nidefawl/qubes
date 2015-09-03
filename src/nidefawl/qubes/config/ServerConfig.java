package nidefawl.qubes.config;

public class ServerConfig extends AbstractYMLConfig {
	public ServerConfig() {
		super(true);
	}
	
	public int port;
	public int packetTimeout;
	
	@Override
	public void setDefaults() {
        port = 21087;
        packetTimeout = 5000;
	}

	@Override
	public void load() {
        port = getInt("port", port);
        packetTimeout = getInt("packetTimeout", packetTimeout);
	}

}
