package nidefawl.qubes.vec;

import org.lwjgl.opengl.GL11;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.VkRect2D.Buffer;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.GameMath;

public class TransformStack {

    final static int MAX_STACK = 16;
    
    final static Vector3f[] stack = new Vector3f[MAX_STACK];
    int stackSize;
    final Vector3f tmp = new Vector3f();
    private StackChangeCallBack cb = null;

    private VkRect2D.Buffer pScissors;

    private VkOffset2D offset;

    private VkExtent2D extent;

    public TransformStack() {
        stackSize = 0;
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new Vector3f(Vector3f.ZERO);
        }
        pScissors = VkRect2D.calloc(1);
        VkRect2D rect = pScissors.get(0);
        this.offset = rect.offset();
        this.extent = rect.extent();
    }
    public void translate(float x, float y, float z) {
        if (stackSize == 0) {
            throw new IllegalStateException("push stack first");
        }
        Vector3f stackTop = stack[stackSize];
        stackTop.x+=x;
        stackTop.y+=y;
        stackTop.z+=z;
        if (cb != null) {
            cb.onChange(get());
        }
    }
    public void push() {
        if (stackSize+1>=MAX_STACK) {
            throw new StackOverflowError();
        }
        stackSize++;
    }
    public void push(float x, float y, float z) {
        push();
        translate(x, y, z);
        if (cb != null) {
            cb.onChange(get());
        }
    }
    public void pop() {
        stack[stackSize].set(Vector3f.ZERO);
        this.stackSize--;
        if (cb != null) {
            cb.onChange(get());
        }
    }
    public Vector3f get() {
        tmp.set(0,0,0);
        for (int i = 0; i < stackSize; i++) {
            tmp.addVec(stack[i+1]);
        }
        return tmp;
    }
    public void setCallBack(StackChangeCallBack cb) {
        this.cb = cb;
    }
    public void setTranslation(float x, float y, float z) {
        if (stackSize == 0) {
            throw new IllegalStateException("push stack first");
        }
        Vector3f stackTop = stack[stackSize];
        stackTop.x=x;
        stackTop.y=y;
        stackTop.z=z;
        if (cb != null) {
            cb.onChange(get());
        }
    }
    
    public void setScissors(int posX, int posY, int width, int height) {
        Vector3f v = get();
        int[] viewPort = Engine.getViewport();
        if (!Engine.isVulkan) {
            GL11.glScissor((int)(v.x+posX), (int)(viewPort[3]-((int)v.y+posY+height)), (int)width, (int)height);
        } else {
            offset.set(GameMath.clampI((int)(v.x+posX), 0, viewPort[2]), GameMath.clampI((int)(((int)v.y+posY)), 0, viewPort[3]));
            extent.set(GameMath.clampI(width, 0, viewPort[2]), GameMath.clampI(height, 0, viewPort[3]));
            vkUpdateLastScissors();
        }
    }
    public void vkUpdateLastScissors() {
        VK10.vkCmdSetScissor(Engine.getDrawCmdBuffer(), 0, pScissors);
    }
}
