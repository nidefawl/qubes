package nidefawl.qubes.config;

import java.io.*;
import java.util.*;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import com.google.common.io.Closeables;

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractYMLConfig {

    protected Map map = new HashMap<>();

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
            DumperOptions options=new DumperOptions();
            options.setIndent(4);
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            Yaml yaml = new Yaml(new SafeConstructor(),new Representer(),options);
//            Iterator it = map.entrySet().iterator();
//            while (it.hasNext()) {
//                Map.Entry entry = (Map.Entry) it.next();
//                System.out.println(entry.getKey()+"="+entry.getValue());
//            }
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
        if (f.exists()) {
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

    public String getString(String s, String def) {
        Object o = this.map.get(s);
        if (o instanceof String)
            return (String) o;
        return def;
    }

    public Boolean getBoolean(String s, boolean def) {
        Object o = this.map.get(s);
        if (o instanceof Boolean)
            return (Boolean) o;
        return def;
    }

    public int getInt(String s, int def) {
        Object o = this.map.get(s);
        if (o instanceof Number)
            return (int) ((Number) o).intValue();
        return def;
    }

    public long getLong(String s, long def) {
        Object o = this.map.get(s);
        if (o instanceof Number)
            return (long) ((Number) o).longValue();
        return def;
    }

    public double getDouble(String s, double def) {
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
    protected void setBoolean(String string, boolean nr) {
        this.map.put(string, nr);
    }

    protected void setInt(String string, int nr) {
        this.map.put(string, nr);
    }

    protected void setLong(String string, long nr) {
        this.map.put(string, nr);
    }

    public abstract void setDefaults();

    public abstract void load() throws InvalidConfigException;

    public abstract void save();

}
