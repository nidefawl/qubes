package nidefawl.qubes.world;

import java.io.File;
import java.util.UUID;

import nidefawl.qubes.config.AbstractYMLConfig;
import nidefawl.qubes.util.StringUtil;

public interface IWorldSettings {
    public long getSeed();
    public UUID getUUID();
    public int getTime();
    
    /** dynamically generated at boot time 
     * do not use for storage
     * @return a consistent world-id at runtime only
     */
    public int getId();
}
