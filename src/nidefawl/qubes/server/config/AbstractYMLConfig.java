package nidefawl.qubes.server.config;

import java.io.*;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.google.common.io.Closeables;

public abstract class AbstractYMLConfig {

	private Map map;
	public AbstractYMLConfig(boolean setDefaults) {
		if (setDefaults)
			setDefaults();
	}
	
	
	public void load(File f) throws InvalidConfigException {
		setDefaults();
		InputStream is = null;
		try {
			is = new BufferedInputStream(new FileInputStream(f));
	        load(is);
		} catch (Exception e) {
			throw new InvalidConfigException("Cannot read "+f, e);
		} finally {
			Closeables.closeQuietly(is);
		}
	}
	public void load(InputStream is) throws InvalidConfigException {
        Yaml yaml = new Yaml();
		Object o = yaml.load(is);
		if (!(o instanceof Map)) {
			throw new InvalidConfigException("config file has invalid format");
		}
		this.map = (Map) o;
		load();
	}

	protected String getString(String s, String def) {
		Object o = this.map.get(s);
		if (o instanceof String) return (String) o;
		return def;
	}
	protected int getInt(String s, int def) {
		Object o = this.map.get(s);
		if (o instanceof Number) return (int)(Number) o;
		return def;
	}
	protected double getDouble(String s, int def) {
		Object o = this.map.get(s);
		if (o instanceof Number) return (double)(Number) o;
		return def;
	}

	public abstract void setDefaults();
	
	public abstract void load() throws InvalidConfigException;
}
