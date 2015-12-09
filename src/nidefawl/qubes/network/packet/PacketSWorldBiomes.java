/**
 * 
 */
package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.Handler;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class PacketSWorldBiomes extends AbstractPacketWorldRef {

    public int numBiomes;
    public int[] coordsX;
    public int[] coordsZ;
    public byte[] biomes;
    /**
     * 
     */
    public PacketSWorldBiomes() {
    }
   public PacketSWorldBiomes(int id) {
       super(id);
   }
    

    @Override
    public void readPacket(DataInput stream) throws IOException {
        super.readPacket(stream);
        this.numBiomes = stream.readInt();
        this.coordsX = new int[this.numBiomes];
        this.coordsZ = new int[this.numBiomes];
        this.biomes = new byte[this.numBiomes];
        for (int i = 0; i < this.numBiomes; i++) {
            this.coordsX[i] = stream.readInt();
            this.coordsZ[i] = stream.readInt();
            this.biomes[i] = stream.readByte();
        }
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        super.writePacket(stream);
        stream.writeInt(this.numBiomes);
        for (int i = 0; i < this.numBiomes; i++) {
            stream.writeInt(this.coordsX[i]);
            stream.writeInt(this.coordsZ[i]);
            stream.writeByte(this.biomes[i]);
        }
    }

    @Override
    public int getID() {
        return 25;
    }

    @Override
    public void handle(Handler h) {
        if (h.isValidWorld(this))
            h.handleWorldBiomes(this);
    }

}
