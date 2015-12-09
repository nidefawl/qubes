/**
 * 
 */
package nidefawl.qubes.world;

import java.util.UUID;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.client.ChunkManagerBenchmark;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class WorldClientBenchmark extends WorldClient {

    private final static WorldSettingsClient makeSettings() {
        WorldSettingsClient clSettings = new WorldSettingsClient();
        clSettings.dayLen = 20000;
        clSettings.time = 12000;
        clSettings.isFixedTime = true;
        clSettings.seed = 0l;
        clSettings.id = 99999;
        clSettings.worldName = "test";
        clSettings.uuid = new UUID(398247L, 2384L);
        return clSettings;
    }
    public WorldClientBenchmark() {
        super(makeSettings(), 0);
        ChunkManagerBenchmark chMgr = (ChunkManagerBenchmark) this.getChunkManager();
        chMgr.testChunk = new Chunk(this, 0, 0, 8);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
               for (int y = 0; y < 160; y++) {
                   chMgr.testChunk.blocks[y<<8|z<<4|x] = (short) Block.granite.id;           
               }
            }
        }
        
    }
    
    public ChunkManager makeChunkManager() {
        return new ChunkManagerBenchmark(this);
    }
}
