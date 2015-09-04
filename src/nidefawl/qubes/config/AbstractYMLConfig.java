package nidefawl.qubes.config;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.google.common.io.Closeables;

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractYMLConfig {

    private Map map = new HashMap<>();

    public AbstractYMLConfig(boolean setDefaults) {
        if (setDefaults)
            setDefaults();
    }

    public void write(File f) throws InvalidConfigException {
        save();
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(f));
            save(os);
        } catch (Exception e) {
            throw new InvalidConfigException("Cannot read " + f, e);
        } finally {
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void save(OutputStream os) {
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(os);
            Yaml yaml = new Yaml();
            yaml.dump(this.map, writer);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } 
            }
        }
    }
    
    public void load(File f) throws InvalidConfigException {
        setDefaults();
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(f));
            load(is);
        } catch (Exception e) {
            throw new InvalidConfigException("Cannot read " + f, e);
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
        if (o instanceof String)
            return (String) o;
        return def;
    }

    protected int getInt(String s, int def) {
        Object o = this.map.get(s);
        if (o instanceof Number)
            return (int) (Number) o;
        return def;
    }

    protected double getDouble(String s, double def) {
        Object o = this.map.get(s);
        if (o instanceof Number)
            return (double) (Number) o;
        return def;
    }

    protected void setString(String string, String nr) {
        this.map.put(string, nr);
    }

    protected void setDouble(String string, double nr) {
        this.map.put(string, nr);
    }

    protected void setInt(String string, int nr) {
        this.map.put(string, nr);
    }

    public abstract void setDefaults();

    public abstract void load() throws InvalidConfigException;

    public abstract void save();

}
