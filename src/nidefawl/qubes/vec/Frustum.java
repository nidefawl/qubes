package nidefawl.qubes.vec;

public class Frustum {

    final static int TOP    = 0;
    final static int BOTTOM = 1;
    final static int LEFT   = 2;
    final static int RIGHT  = 3;
    final static int NEARP  = 4;
    final static int FARP   = 5;

    Vector4f[] frustum = new Vector4f[] {
        new Vector4f(),
        new Vector4f(),
        new Vector4f(),
        new Vector4f(),
        new Vector4f(),
        new Vector4f(),
    };
    
    public void set(Matrix4f mvp) {
        frustum[LEFT].x = mvp.m03+mvp.m00;
        frustum[LEFT].y = mvp.m13+mvp.m10;
        frustum[LEFT].z = mvp.m23+mvp.m20;
        frustum[LEFT].w = mvp.m33+mvp.m30;
        frustum[RIGHT].x = mvp.m03-mvp.m00;
        frustum[RIGHT].y = mvp.m13-mvp.m10;
        frustum[RIGHT].z = mvp.m23-mvp.m20;
        frustum[RIGHT].w = mvp.m33-mvp.m30;
        frustum[BOTTOM].x = mvp.m03+mvp.m01;
        frustum[BOTTOM].y = mvp.m13+mvp.m11;
        frustum[BOTTOM].z = mvp.m23+mvp.m21;
        frustum[BOTTOM].w = mvp.m33+mvp.m31;
        frustum[TOP].x = mvp.m03-mvp.m01;
        frustum[TOP].y = mvp.m13-mvp.m11;
        frustum[TOP].z = mvp.m23-mvp.m21;
        frustum[TOP].w = mvp.m33-mvp.m31;
        frustum[NEARP].x = mvp.m03+mvp.m02;
        frustum[NEARP].y = mvp.m13+mvp.m12;
        frustum[NEARP].z = mvp.m23+mvp.m22;
        frustum[NEARP].w = mvp.m33+mvp.m32;
        frustum[FARP].x = mvp.m03-mvp.m02;
        frustum[FARP].y = mvp.m13-mvp.m12;
        frustum[FARP].z = mvp.m23-mvp.m22;
        frustum[FARP].w = mvp.m33-mvp.m32;
        for (int i = 0; i < 6; i++) {
            normalize(i);
        }
    }
    private void normalize(int i) {
        float l = frustum[i].length();
        frustum[i].x /= l;
        frustum[i].y /= l;
        frustum[i].z /= l;
        frustum[i].w /= l;
    }

    float planeDistance(Vector4f plane, float x, float y, float z) {
    
        return (plane.w + (plane.x * x + plane.y * y + plane.z * z));
    }

    /**
     * check if aabb is inside or collides frustum
     * @param aabb
     * @return -1 if outside, 0 if collides, 1 if fully inside
     */
    public int checkFrustum(AABB aabb) {
        int result = 1;
        for(int i=0; i < 6; i++) {
            Vector4f plane = frustum[i];
            float pX = (float) (plane.x > 0 ? aabb.maxX : aabb.minX);
            float pY = (float) (plane.y > 0 ? aabb.maxY : aabb.minY);
            float pZ = (float) (plane.z > 0 ? aabb.maxZ : aabb.minZ);
            if (planeDistance(plane, pX, pY, pZ) < 0)
                return -1;
            float nX = (float) (plane.x < 0 ? aabb.maxX : aabb.minX);
            float nY = (float) (plane.y < 0 ? aabb.maxY : aabb.minY);
            float nZ = (float) (plane.z < 0 ? aabb.maxZ : aabb.minZ);
            if (planeDistance(plane, nX, nY, nZ) < 0)
                result =  0;
        }
        return result;
    }

}
