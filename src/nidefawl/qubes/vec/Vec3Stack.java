package nidefawl.qubes.vec;

public class Vec3Stack {

    final static int MAX_STACK = 16;
    
    final static Vector3f[] stack = new Vector3f[MAX_STACK];
    int stackSize;
    final Vector3f tmp = new Vector3f();
    private StackChangeCallBack cb = null;

    public Vec3Stack() {
        stackSize = 0;
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new Vector3f(Vector3f.ZERO);
        }
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
}