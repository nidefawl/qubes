package nidefawl.qubes.vec;

import nidefawl.qubes.gl.BufferedMatrix;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.GameMath;

public class Frustum {

    final static private float HALF_ANG2RAD = (float)(3.14159265358979323846 / 360.0); 
    final static int TOP    = 3;
    final static int BOTTOM = 2;
    final static int LEFT   = 1;
    final static int RIGHT  = 0;
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
    Vector3f up = new Vector3f();
    Vector3f forward = new Vector3f();
    Vector3f cam = new Vector3f();
    Vector3f tmp=new Vector3f();
    Vector3f X=new Vector3f();
    Vector3f Y=new Vector3f();
    Vector3f Z=new Vector3f();

    public void setCamInternals(float angle, float ratio, float nearD, float farD) {
        this.ratio = ratio;
        this.angle = (angle) * HALF_ANG2RAD;
        this.znear = nearD;
        this.zfar = farD;

        // compute width and height of the near and far plane sections
        tang = GameMath.tan(this.angle);
        sphereFactorY = 1.0f/GameMath.cos(this.angle);//tang * sin(this.angle) + cos(this.angle);

        float anglex = GameMath.atan(tang*ratio);
        sphereFactorX = 1.0f/GameMath.cos(anglex); //tang*ratio * sin(anglex) + cos(anglex);

    }
            
    private boolean changed;
    private float znear;
    private float zfar;
    private float ratio;
    private float tang;
    private float angle;
    private float sphereFactorY;
    private float sphereFactorX;
    public static final int FRUSTUM_INSIDE_FULLY = 1;
    public static final int FRUSTUM_INSIDE = 0;
    public static final int FRUSTUM_OUTSIDE = -1;

    public void set2(Matrix4f mvp) {
//        TimingHelper.startSilent(4);
        this.changed = frustum[LEFT].setChecked(mvp.m03 + mvp.m00, mvp.m13 + mvp.m10, mvp.m23 + mvp.m20, mvp.m33 + mvp.m30);
        changed |= frustum[RIGHT].setChecked(mvp.m03 - mvp.m00, mvp.m13 - mvp.m10, mvp.m23 - mvp.m20, mvp.m33 - mvp.m30);
        changed |= frustum[BOTTOM].setChecked(mvp.m03 + mvp.m01, mvp.m13 + mvp.m11, mvp.m23 + mvp.m21, mvp.m33 + mvp.m31);
        changed |= frustum[TOP].setChecked(mvp.m03 - mvp.m01, mvp.m13 - mvp.m11, mvp.m23 - mvp.m21, mvp.m33 - mvp.m31);
        changed |= frustum[NEARP].setChecked(mvp.m03 + mvp.m02, mvp.m13 + mvp.m12, mvp.m23 + mvp.m22, mvp.m33 + mvp.m32);
        changed |= frustum[FARP].setChecked(mvp.m03 - mvp.m02, mvp.m13 - mvp.m12, mvp.m23 - mvp.m22, mvp.m33 - mvp.m32);
//        long l = TimingHelper.stopSilent(4);
//        System.out.println("changed: "+changed+" - took "+l);


    }
    /**
     * @return the changed
     */
    public boolean isChanged() {
        return this.changed;
    }
    
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
        
//        frustumCorner
//        System.out.println(mvp.m33+"/"+mvp.m30+"/"+mvp.m31+"/"+mvp.m32);
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

    public float planeDistance(Vector4f plane, float x, float y, float z) {
        return (plane.w + (plane.x * x + plane.y * y + plane.z * z));
    }
    
    
    
    public int pointInFrustum(Vector3f pnt) {

        float pcz,pcx,pcy,aux;

        // compute vector from camera position to p
        Vector3f.sub(pnt, this.cam, tmp);
        Vector3f v = tmp;
//        System.out.println(v);

        // compute and test the Z coordinate
        pcz = Vector3f.dot(v, Z);
        if (pcz > zfar || pcz < znear)
            return FRUSTUM_OUTSIDE;

        // compute and test the Y coordinate
        pcy = Vector3f.dot(v, Y);
        aux = pcz * tang;
//        System.out.println(aux);
        if (pcy > aux || pcy < -aux)
            return FRUSTUM_OUTSIDE;
            
//        // compute and test the X coordinate
        pcx = Vector3f.dot(v, X);
        aux = aux * ratio;
        if (pcx > aux || pcx < -aux)
            return FRUSTUM_OUTSIDE;


        return FRUSTUM_INSIDE;
    
        
    }
    
    
    public int sphereInFrustum(Vector3f pnt, float radius) {
    
//        System.out.println(Engine.GLOBAL_OFFSET);
        float x = pnt.x - this.cam.x;
        float y = pnt.y - this.cam.y;
        float z = pnt.z - this.cam.z;
        final float az =  x * Z.x + y * Z.y + z * Z.z;
        if (az > zfar + radius || az < znear-radius)
            return (FRUSTUM_OUTSIDE);

        final float ax =  x * X.x + y * X.y + z * X.z;
        final float zz1 = az * tang * ratio;
        final float d1 = sphereFactorX * radius;
        if (ax > zz1+d1 || ax < -zz1-d1)
            return (FRUSTUM_OUTSIDE);

        final float ay =  x * Y.x + y * Y.y + z * Y.z;
        final float zz2 = az * tang;
        final float d2 = sphereFactorY * radius;
        if (ay > zz2+d2 || ay < -zz2-d2)
            return (FRUSTUM_OUTSIDE);
    
    
    
        if (az > zfar - radius || az < znear+radius)
            return FRUSTUM_INSIDE;
        if (ay > zz2-d2 || ay < -zz2+d2)
            return FRUSTUM_INSIDE;
        if (ax > zz1-d1 || ax < -zz1+d1)
            return FRUSTUM_INSIDE;
    
    
        return FRUSTUM_INSIDE_FULLY;
    
    }
    /**
     * check if aabb is inside or collides frustum
     * @param aabb
     * @return -1 if outside, 0 if collides, 1 if fully inside
     */
    public int checkFrustum(AABB aabb) {
        int result = FRUSTUM_INSIDE_FULLY;
        for(int i=0; i < 6; i++) {
            Vector4f plane = frustum[i];
            float pX = (float) (plane.x > 0 ? aabb.maxX : aabb.minX);
            float pY = (float) (plane.y > 0 ? aabb.maxY : aabb.minY);
            float pZ = (float) (plane.z > 0 ? aabb.maxZ : aabb.minZ);
            if (planeDistance(plane, pX, pY, pZ) < 0) {
                return FRUSTUM_OUTSIDE;
            }
            float nX = (float) (plane.x < 0 ? aabb.maxX : aabb.minX);
            float nY = (float) (plane.y < 0 ? aabb.maxY : aabb.minY);
            float nZ = (float) (plane.z < 0 ? aabb.maxZ : aabb.minZ);
            if (planeDistance(plane, nX, nY, nZ) < 0)
                result = FRUSTUM_INSIDE;
        }
        return result;
    }

    public int checkFrustumPnt(Vector3f pnt, double maxDist) {
        int result = FRUSTUM_INSIDE_FULLY;
        for(int i=0; i < 6; i++) {
            Vector4f plane = frustum[i];
            float pX = (float) (plane.x > 0 ? maxDist : -maxDist);
            float pY = (float) (plane.y > 0 ? maxDist : -maxDist);
            float pZ = (float) (plane.z > 0 ? maxDist : -maxDist);
            if (planeDistance(plane, pX, pY, pZ) < 0) {
                return FRUSTUM_OUTSIDE;
            }
            float nX = (float) (plane.x < 0 ? maxDist : -maxDist);
            float nY = (float) (plane.y < 0 ? maxDist : -maxDist);
            float nZ = (float) (plane.z < 0 ? maxDist : -maxDist);
            if (planeDistance(plane, nX, nY, nZ) < 0)
                result = FRUSTUM_INSIDE;
        }
        return result;
    }

    public int checkFrustum(AABBInt aabb, float f) {
//        tmp.set(aabb.getCenterX(), aabb.getCenterY(), aabb.getCenterZ());
//        int result = sphereInFrustum(tmp, f*1.1f);
//        if (result == FRUSTUM_INSIDE) {

            return checkFrustum(aabb);
//        }
//        return result;
    }
    /**
     * check if aabb is inside or collides frustum
     * @param aabb
     * @return -1 if outside, 0 if collides, 1 if fully inside
     */
    public int checkFrustum(AABBInt aabb) {
        int result = FRUSTUM_INSIDE_FULLY;
        float maxX = (float) aabb.maxX;
        float maxY = (float) aabb.maxY;
        float maxZ = (float) aabb.maxZ;
        float minX = (float) aabb.minX;
        float minY = (float) aabb.minY;
        float minZ = (float) aabb.minZ;
        for(int i=0; i < 6; i++) {
            Vector4f plane = frustum[i];
            float pX = plane.x > 0 ? maxX : minX;
            float pY = plane.y > 0 ? maxY : minY;
            float pZ = plane.z > 0 ? maxZ : minZ;
            if (planeDistance(plane, pX, pY, pZ) < 0) {
                return FRUSTUM_OUTSIDE;
            }
            float nX = plane.x < 0 ? maxX : minX;
            float nY = plane.y < 0 ? maxY : minY;
            float nZ = plane.z < 0 ? maxZ : minZ;
            if (planeDistance(plane, nX, nY, nZ) < 0)
                result = FRUSTUM_INSIDE;
        }
        return result;
    }

    public void setPos(Vector3f vec, Matrix4f viewInv) {
        this.cam.set(vec);
        this.cam.x-=Engine.GLOBAL_OFFSET.x;
        this.cam.y-=Engine.GLOBAL_OFFSET.y;
        this.cam.z-=Engine.GLOBAL_OFFSET.z;
        up.set(0, 1, 0);
        forward.set(0, 0, zfar - znear);
        Matrix4f.transform(viewInv, forward, forward);

        forward.scale(-1);

        Z.set(forward);
        // compute the Z axis of camera
        Z.normalise();

        // X axis of camera of given "up" vector and Z axis
        Vector3f.cross(up, Z, X);
        X.normalise();

        // the real "up" vector is the cross product of Z and X
        Vector3f.cross(Z, X, Y);
    }

}
