package nidefawl.qubes.server.config;

import java.io.*;

import org.yaml.snakeyaml.Yaml;

import com.google.common.io.Closeables;

public class ConfigLoader {
	public <T> T loadConfig(File f, Class<T> a) throws IOException {
		InputStream is = null;
		try {
			is = new BufferedInputStream(new FileInputStream(f));
			return loadConfig(is, a);
		} finally {
			Closeables.closeQuietly(is);
		}
	}
	
	public <T> T loadConfig(InputStream is, Class<T> a) {
        Yaml loader = new Yaml();
        return loader.loadAs(is, a);
	}
}
