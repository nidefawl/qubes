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
        if (this.posticks>0) {
            if (this.posticks==1) {
                this.pos.set(this.remotePos);
            } else {
                this.pos.x+=(this.remotePos.x-this.pos.x)/3;
                this.pos.y+=(this.remotePos.y-this.pos.y)/3;
                this.pos.z+=(this.remotePos.z-this.pos.z)/3;
            }
            this.posticks--;
        }
        if (this.rotticks>0) {
            if (this.rotticks==1) {
                this.yaw = this.remoteRotation.x;
                this.yawBodyOffset = this.remoteRotation.y;
                this.pitch = this.remoteRotation.z;
            } else {
                this.yaw+=(this.remoteRotation.x-this.yaw)/3;
                this.yawBodyOffset+=(this.remoteRotation.y-this.yawBodyOffset)/3;
                this.pitch+=(this.remoteRotation.z-this.pitch)/3;
            }
            this.rotticks--;
        }
        updateTicks();
    }

}
