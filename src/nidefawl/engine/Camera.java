package nidefawl.engine;

import nidefawl.engine.util.GameMath;
import nidefawl.game.Main;

import org.lwjgl.input.Keyboard;

public class Camera {

	// How fast we move (higher values mean we move and strafe faster)
	public float camSpeed          = 0.05f;
	// Camera position
	public float camXPos           = 0.0f;
	// Camera rotation
	public float camXRot           = 45.0f;
	// Camera movement speed
	public float camXSpeed         = 0.0f;
	public float camYPos           = 40.0f;
	public float camYRot           = 0.0f;
	public float camYSpeed         = 0.0f;
	public float camZPos           = -30.0f;
	public float camZRot           = 0.0f;
	public float camZSpeed         = 0.0f;
    // Function to calculate which direction we need to move the camera and by
    // what amount
    public float moveStrafe        = 0;
    public float moveStrafeForward = 0;
    public float moveForward       = 0;
    public float moveUpward;
	public void handleInput(double mdX, double mdY) {
        if (mdX != 0) {
            camYRot += ((float) mdX / (float) Main.displayWidth) * 360F;
            while (camYRot > 180F)
                camYRot -= 360F;
            while (camYRot < -180F)
                camYRot += 360F;
        }
        if (mdY != 0) {
            camXRot += ((float) -mdY / (float) Main.displayHeight) * 360F;
            while (camXRot > 90F)
                camXRot = 90F;
            while (camXRot < -90F)
                camXRot = -90F;
        }
	}
	public void calculateCameraMovement(float fTime) {
        moveForward = moveStrafe = moveStrafeForward = moveUpward = 0F;
        if (Keyboard.isKeyDown(Keyboard.KEY_F)) {
            moveForward = moveStrafe = moveStrafeForward = moveUpward = 0F;
        }
        float maxSpeed = camSpeed;
        for (int i = 0; i < 10; i++) {
            if (Keyboard.isKeyDown(Keyboard.KEY_1 + i)) {
                maxSpeed *= Math.pow(2F, i);
                break;
            }
        }
        maxSpeed *= fTime;
        if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
            moveForward += maxSpeed;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
            moveForward -= maxSpeed;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
            moveStrafe += maxSpeed;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
            moveStrafe -= maxSpeed;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_Q)) {
            moveUpward += maxSpeed;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_Y)) {
            moveUpward -= maxSpeed;
        }
        float xRot = camXRot * (float) Math.PI / 180F;
        float yRot = camYRot * (float) Math.PI / 180F;
        // Control X-Axis movement
        camXSpeed =  moveForward * GameMath.sin(yRot) * GameMath.cos(xRot);

        // Control Y-Axis movement
        camYSpeed = -moveForward * GameMath.sin(xRot);

        // Control Z-Axis movement
        camZSpeed = -moveForward * GameMath.cos(yRot) * GameMath.cos(xRot);
        camXSpeed +=  moveStrafeForward * GameMath.sin(yRot) * GameMath.cos(xRot);
        camZSpeed += -moveStrafeForward * GameMath.cos(yRot) * GameMath.cos(xRot);

        camXSpeed += moveStrafe * GameMath.cos(yRot);
        camZSpeed += moveStrafe * GameMath.sin(yRot);

        double vDecrease = 0.1*fTime; // more fps = slower decrease per frame = smaller vDeacrease
        if (vDecrease != vDecrease || vDecrease < 0.01D) {
            vDecrease = 0.01D;
        }
        vDecrease = 1D - vDecrease;
        moveStrafe *= vDecrease;
        moveUpward *= vDecrease;
        moveForward *= vDecrease;
        moveStrafeForward *= vDecrease;
        camYSpeed += moveUpward;
        double curSpeed = Math.sqrt(camXSpeed*camXSpeed + camYSpeed*camYSpeed + camZSpeed*camZSpeed);
        if (curSpeed > maxSpeed) {
            curSpeed = maxSpeed / curSpeed;
            camXSpeed *= curSpeed;
            camYSpeed *= curSpeed;
            camZSpeed *= curSpeed;
        }
        camXPos += camXSpeed;
        camYPos += camYSpeed;
        camZPos += camZSpeed;
    }

}
