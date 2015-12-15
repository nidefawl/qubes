/**
 * 
 */
package nidefawl.qubes.input;

import nidefawl.qubes.Game;
import nidefawl.qubes.network.packet.PacketCDigState;
import nidefawl.qubes.network.packet.PacketCSetBlock;
import nidefawl.qubes.util.RayTrace.RayTraceIntersection;
import nidefawl.qubes.vec.BlockPos;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class DigController {
    boolean digging = false;
    int speed = 60;
    int tick = 0;
    public BlockPos mouseOver;
    private RayTraceIntersection intersect;
    /**
     * @param button
     * @param isDown
     */
    public void onMouseClick(int button, boolean isDown) {
        if (button == 0) {
            if (!isDown) {
                endDigging();
            } else {
                startDigging();
            }
        }
        speed = 15;
    }


    /**
     * 
     */
    public int getTicks() {
        return this.tick;
    }

    /**
     * 
     */
    public int getSpeed() {
        return this.speed;
    }

    /**
     * @return
     */
    public boolean isDigAnimation() {
        return digging || tick != 0;
    }

    /**
     * @param grabbed
     */
    public void onGrabChange(boolean grabbed) {
        if (!grabbed)
            endDigging();
    }
    private void startDigging() {
        if (!this.digging) {
            sendDigState(0);
            this.digging = true;
        }
    }
    private void endDigging() {
        if (this.digging) {
            sendDigState(1);
            this.digging = false;
        }
    }

    /**
     * 
     */
    public void preRenderUpdate() {
    }
    public void update() {
        if (digging) {
            sendDigState(2);
            tick++;
        } else if (tick != 0) {
            tick++;
        }
        if (digging && tick == 8) {
            sendDigState(3);
        }
        if (tick >= speed) {
            tick = 0;
        }
    }

    private void sendDigState(int state) {
        if (intersect != null) {
            int faceHit = intersect.face;
            BlockPos pos = intersect.blockPos;
            Game.instance.sendPacket(new PacketCDigState(Game.instance.getWorld().getId(), state, pos, intersect.pos, faceHit, Game.instance.getPlayer().getEquippedItem()));

        }
    }

    public void setBlock(RayTraceIntersection hit, BlockPos mouseOver) {
        if (this.mouseOver == null || !mouseOver.equals(this.mouseOver)) {
//            reset();
        }
        this.mouseOver = mouseOver;
        this.intersect = hit;
    }

    private void reset() {
        tick = 0;
        sendDigState(1);
    }

}
