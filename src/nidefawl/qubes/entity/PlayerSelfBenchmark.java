/**
 * 
 */
package nidefawl.qubes.entity;

import nidefawl.qubes.PlayerProfile;
import nidefawl.qubes.input.Movement;
import nidefawl.qubes.network.client.ClientHandler;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class PlayerSelfBenchmark extends PlayerSelf {

    /**
     * @param clientHandler
     * @param profile
     */
    public PlayerSelfBenchmark(ClientHandler clientHandler, PlayerProfile profile) {
        super(clientHandler, profile);
    }

    /**
     * 
     */
    public PlayerSelfBenchmark() {
        super(null, null);
    }
    @Override
    public void updateInputDirect(Movement movement) {
    }
    @Override
    public void tickUpdate() {
        this.noclip = true;
        this.yaw = 0;
        this.lastYaw = this.yaw;
        this.pitch = 60;
        this.lastPitch = this.pitch;
        move(0, 200, 0);
    }

}
