package nidefawl.qubes.config;

public class ServerConfig extends AbstractYMLConfig {
	public ServerConfig() {
		super(true);
	}
	
	public int port;
	public String host;
	public long packetTimeout;
	
	@Override
	public void setDefaults() {
		host = "";
		port = 21087;
	}

	@Override
	public void load() {
		host = getString("host", host);
		port = getInt("port", port);
	}

}
