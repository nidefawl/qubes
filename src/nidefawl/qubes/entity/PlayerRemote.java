/**
 * 
 */
package nidefawl.qubes.entity;

import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.TagType;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class PlayerRemote extends Player {

    /**
     * 
     */
    public PlayerRemote() {
    }

    public void readClientData(Tag tag) {
        if (tag.getType() == TagType.COMPOUND) {
            this.name = ((Tag.Compound)tag).getString("name");
        }
    }
    
    @Override
    public void tickUpdate() {
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;
        this.lastMot.set(this.mot);
        this.lastPos.set(this.pos);
    }

}
