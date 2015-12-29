package nidefawl.qubes.world;

import java.util.UUID;

import nidefawl.qubes.network.StreamIO;

public interface IWorldSettings extends StreamIO {
    public long getSeed();
    public UUID getUUID();
    public long getTime();
    
    /** dynamically generated at boot time 
     * do not use for storage
     * @return a consistent world-id at runtime only
     */
    public int getId();
    
    public String getName();
    /**
     * @return
     */
    public long getDayLen();
    /**
     * @return
     */
    public boolean isFixedTime();
    
   public void setTime(long l);
   public void setDayLen(long l);
   public void setFixedTime(boolean b);
}
