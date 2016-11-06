package nidefawl.qubes.block;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableBiMap;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.config.InvalidConfigException;
import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.util.GameContext;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.Side;

public class IDMappingBlocks {
    public static boolean CHANGED = false;
    public static boolean LOADED = false;

    public static int HIGHEST_BLOCK_ID = 0;
    static ImmutableBiMap<String, Integer> map = ImmutableBiMap.<String, Integer>builder().build();
    final static Object sync = new Object();
    
    public static void load() {
        InputStream is = null;
        try {
            synchronized (sync) {
                if (GameContext.getSide() == Side.SERVER) {
                    File inFile = new File(WorkingEnv.getConfigFolder(), "blockmapping.yml");
                    if (inFile.exists()) {
                        is = new FileInputStream(inFile);
                    }
                } else {
                    is = AssetManager.getInstance().getInputStream("config/blockmapping.yml");
                }
                is = new BufferedInputStream(is);
                Yaml yaml = new Yaml();
                Object o = yaml.load(new InputStreamReader(is, Charsets.UTF_8));
                if (!(o instanceof Map)) {
                    throw new InvalidConfigException("config file has invalid format");
                }
                map = ImmutableBiMap.<String, Integer>builder().putAll((Map)o).build();
                for (int i = 0; i < Block.NUM_BLOCKS; i++) {
                    if (map.inverse().get(i) != null) {
                        if (i > HIGHEST_BLOCK_ID) {
                            HIGHEST_BLOCK_ID = i;
                        }
                    }
                }
                System.out.println("HIGHEST "+HIGHEST_BLOCK_ID);
            }
        } catch (Exception e) {
            throw new GameError("Failed loading block id mapping", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e2) {
                }
            }
        }
        
        LOADED = true;
    }
    public static void save() {
        if (!CHANGED) return;
        FileLock lock = null;
        RandomAccessFile raf = null;
        FileChannel filechannel = null;
        OutputStream os  = null;
        OutputStreamWriter oswriter = null;
        try {
            synchronized (sync) {
                File outFile = new File(WorkingEnv.getConfigFolder(), "blockmapping.yml");
                raf = new RandomAccessFile(outFile, "rws");
                filechannel = raf.getChannel();
                lock = filechannel.lock(0, Long.MAX_VALUE, false);
                os = Channels.newOutputStream(filechannel);
                DumperOptions options=new DumperOptions();
                options.setIndent(4);
                options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                options.setPrettyFlow(true);
                Yaml yaml = new Yaml(new SafeConstructor(),new Representer(),options);
                oswriter = new OutputStreamWriter(os, Charsets.UTF_8);
                yaml.dump(map, oswriter);
            }
        } catch (Exception e) {
            throw new GameError("Failed loading block id mapping", e);
        } finally {
            try {
                if (oswriter != null) {
                    oswriter.flush();
                    oswriter.close();
                }
                if (os != null) {
                    os.close();
                }
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        CHANGED = false;
    }
    public static int get(String sid) {
        if (!LOADED) {
            throw new GameError("Wrong class loading order");
        }
        try {
            synchronized (sync) {
                Integer id = map.get(sid);
                if (id != null && id.intValue() >= 0) {
                    return id;
                }
                if (GameContext.getSide() == Side.CLIENT) {
                    throw new GameError("Missing block id for "+sid);
                }
                System.out.println("Finding id for new block "+sid);
                Integer newId = null;
                for (int i = 0; i < Block.NUM_BLOCKS; i++) {
                    if (map.inverse().get(i) == null) {
                        newId = i;
                        break;
                    }
                }
                if (newId == null) {
                    throw new GameError("Out of block IDs");
                }
                if (newId > HIGHEST_BLOCK_ID) {
                    HIGHEST_BLOCK_ID = newId;
                }
                map = ImmutableBiMap.<String, Integer>builder().putAll(map).put(sid, newId).build();
                CHANGED = true;
                return newId;
            }
            
        } catch (Exception e) {
            throw new GameError("Failed updating block id map ", e);
        }
    }

}
