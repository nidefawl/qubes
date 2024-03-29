package nidefawl.qubes.entity;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.PlayerProfile;
import nidefawl.qubes.crafting.CraftingCategory;
import nidefawl.qubes.crafting.CraftingManagerClient;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.input.KeybindManager;
import nidefawl.qubes.inventory.slots.SlotsCrafting;
import nidefawl.qubes.inventory.slots.SlotsInventory;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.lighting.DynamicLight;
import nidefawl.qubes.network.client.ClientHandler;
import nidefawl.qubes.network.packet.PacketCMovement;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.Side;
import nidefawl.qubes.util.SideOnly;
import nidefawl.qubes.world.WorldClient;

@SideOnly(value=Side.CLIENT)
public class PlayerSelf extends Player {

    private float   forward;
    private float   strafe;
    private float   maxSpeed = 0.82F;
    private boolean fly      = false;
    private boolean   jumped;
    private boolean   sneak;
    private float jump;
    public float eyeHeight = 1.3F;
    public PlayerProfile profile;
    private ClientHandler clientHandler;
    public final CraftingManagerClient[] crafting = new CraftingManagerClient[CraftingCategory.NUM_CATS];
    public float cameraYaw;
    public float cameraPitch;
    public float prevCameraPitch;
    public float prevCameraYaw;
    

    public PlayerSelf(ClientHandler clientHandler, PlayerProfile profile) {
        super(false);
        this.slotsInventory = new SlotsInventory(this, 0, 0, Gui.slotW, Gui.slotBDist);
        for (int i = 0; i < CraftingCategory.NUM_CATS; i++) {
            this.slotsCrafting[i] = new SlotsCrafting(this, 1+i, 0, 0, Gui.slotW, Gui.slotBDist);
            this.crafting[i] = new CraftingManagerClient(this, i);
        }
        this.profile = profile;
        this.clientHandler = clientHandler;
        this.name = this.profile.getName();
    }
    public CraftingManagerClient getCrafting(int id) {
        return id < 0 | id >= this.crafting.length ? null : this.crafting[id];
    }

    public void updateInputDirect(KeybindManager movement) {
        float fa = 0.14F;
        float mx = movement.mX * fa;
        float my = -movement.mY * fa;
        float newP = (float) Math.max(-90F, Math.min(90F, this.pitch + my));
        float newY = (float) this.yaw + mx;
        float diffP = newP - this.pitch;
        float diffY = newY - this.yaw;
        this.pitch = newP;
        this.yaw = newY;
        this.lastPitch += diffP;
        this.lastYaw += diffY;
        this.strafe = movement.strafe;
        this.forward = movement.forward;
        if (this.fly) {
            this.jump = movement.jump?1:0;
            jumped = false;
        } else {
            if (jumped) {
                if (!movement.jump) {
                    jumped = false;
                }
                movement.jump = false;
            } else {
                if (hitGround) {
                    if(movement.jump) {
                        jumped = true;
                        this.jump=4;
                        this.timeJump = GameBase.absTime;
                    } else {
                    }
                }
            }
        }
        this.sneak = movement.sneak;
        movement.mX = 0;
        movement.mY = 0;
    }

    public void update(float newP, float newY, float forward, float strafe, float jump, boolean sneak) {
        float diffP = newP - this.pitch;
        float diffY = newY - this.yaw;
        this.pitch = newP;
        this.yaw = newY;
        this.lastPitch += diffP;
        this.lastYaw += diffY;
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sneak = sneak;
    }
    
    @Override
    public void tickUpdate() {
        float vel = GameMath.sqrtf(this.forward * this.forward + this.strafe * this.strafe);
        this.noclip = this.fly;
        if (this.fly) {
            maxSpeed = 0.9F;
            float var7 = 0.0F;
            this.mot.y -= 0.98D * (this.sneak?1:0);
            this.mot.y += 0.98D * this.jump;


            float f4 = 0.0F;
            float f5 = 0.0F;
            float f6 = 0.0F;
            float f7 = 0.0F;
            if (vel >= 0.01F) {
                if (vel < 1.0F) {
                    vel = 1.0F;
                }

                float strafe = -this.strafe / vel;
                float forward = -this.forward / vel;
                float sinY = GameMath.sin(GameMath.degreesToRadians(this.yaw));
                float cosY = GameMath.cos(GameMath.degreesToRadians(this.yaw));
                f4 = strafe * cosY;
                f5 = -forward * sinY;
                f6 = strafe * sinY;
                f7 = forward * cosY;
            }

            float f8 = GameMath.degreesToRadians(-this.pitch);
            float fm = GameMath.cos(f8);
            float f1 = -GameMath.sin(f8) * Math.signum(-this.forward);
            float f2 = f5 * fm + f4;
            float f3 = GameMath.sqrtf(f5 * f5 + f7 * f7) * f1 + var7;
            float f9 = f7 * fm + f6;
            float f10 = GameMath.sqrtf(GameMath.sqrtf(f2 * f2 + f9 * f9) + f3 * f3);
            if (f10 > 0.01F) {
                float f11 = maxSpeed / f10;
                this.mot.x += (double) (f2 * f11);
                this.mot.y += (double) (f3 * f11);
                this.mot.z += (double) (f9 * f11);
            }
            this.jump *= 1;
        } else {
            float jmp = this.jump;
            if (jump > 1) {
                jmp = 0;
            } else if (jump <0.5 && hitGround) {
                this.timeJump = 0;
                 
            }
            maxSpeed = 0.2F;
            this.mot.y += 0.49171D * jmp;
            this.jump *= 0.5;
            if (vel > 0.01F) {
                if (vel < 1)
                    vel = 1;
                float fm = maxSpeed / vel;
                float forward = -this.forward * fm;
                float strafe = -this.strafe * fm;
                float sinY = GameMath.sin(GameMath.degreesToRadians(this.yaw));
                float cosY = GameMath.cos(GameMath.degreesToRadians(this.yaw));
                this.mot.x += -forward * sinY + strafe * cosY;
                this.mot.z += forward * cosY + strafe * sinY;
            }
        }
        prevCameraPitch = cameraPitch;
        prevCameraYaw = cameraYaw;
        super.tickUpdate();
        float xzspeed = GameMath.sqrtf((float) (this.mot.x*this.mot.x+this.mot.z*this.mot.z));
        float yMovement = (float) GameMath.atan((float) (-this.mot.y * 0.2D)) * 15F;
        if (xzspeed > 0.1F) {
            xzspeed = 0.1F;
        }
        if (isDead()) {
            xzspeed = 0.0F;
            yMovement = 0.0F;
        } else if (hitGround) {
            yMovement = 0.0F;
        } else {
            xzspeed = 0.0F;
        }
        cameraYaw += (xzspeed - cameraYaw) * 0.4F;
        cameraPitch += (yMovement - cameraPitch) * 0.8F;
        int flags = 0;
        if (this.hitGround) {
            flags |= 1;
        }
        if (this.fly) {
            flags |= 2;
        }
        this.clientHandler.sendPacket(new PacketCMovement(this.pos, this.yaw, this.pitch, flags));
        DynamicLight e = this.light;
        this.lightcolor.set(1, 1, 0.5f);
        e.tickUpdate((WorldClient) this.world);
//        e.pos.set(this.pos);
//        e.pos.add(0, 4, 0);
        e.intensity = 0.5f;
        
    }
    public boolean isDead() {
        return false;
    }
    public boolean doesFly() {
        return this.fly;
    }


    public float getGravity() {
        return this.fly ? 0 : 0.98F;
    }

    public void toggleFly() {
        this.fly = !fly;
    }
    
    public void setFly(boolean fly) {
        this.fly = fly;
    }
    
    @Override
    public EntityType getEntityType() {
        return EntityType.PLAYER;
    }

    /**
     * @param button
     * @param isDown
     */
    public void clicked(int button, boolean isDown) {
        if (isDown) {
            this.punchTicks = 20;
            this.timePunch = GameBase.absTime;
        }
    }

    @Override
    public BaseStack getActiveItem(int i) {
        if (i == 0)
            return inventory.getItem(0);
        return super.getActiveItem(i);
    }
    protected boolean findEdge() {
        return this.sneak;
    }
}
