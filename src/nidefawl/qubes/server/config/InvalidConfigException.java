package nidefawl.qubes.server.config;

@SuppressWarnings("serial")
public class InvalidConfigException extends Exception {

	public InvalidConfigException(String string) {
		super(string);
	}

	public InvalidConfigException(String string, Exception e) {
		super(string, e);
	}

}
