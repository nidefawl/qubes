package nidefawl.qubes.gl;

import java.nio.FloatBuffer;

import jopenvr.HmdMatrix44_t;
import jopenvr.JOpenVRLibrary;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.util.Project;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.vr.VR;

public class ShadowProjector implements IRenderComponent {
    final static int NUM_SPLITS = 3;
    private Matrix4f[]       shadowSplitProj;
    private BufferedMatrix[] shadowSplitMVP;
    public float[]           shadowSplitDepth;
    public Frustum[]         shadowCamFrustum;
    Matrix4f newMat = new Matrix4f();
    Matrix4f newMatInv = new Matrix4f();
    Matrix4f matLookAt = new Matrix4f();
    Matrix4f matLookAtInv = new Matrix4f();
    Matrix4f matOrtho = new Matrix4f();

    Vector3f frustumCenter = new Vector3f(0, 0, 0);
    Vector3f tmp = new Vector3f(0, 0, 0);
    Vector3f eye = new Vector3f();
    private float[] splits;

    static Vector3f[] furstumCornersIn = new Vector3f[] {
            new Vector3f(-1,  1, 0),
            new Vector3f( 1,  1, 0),
            new Vector3f( 1, -1, 0),
            new Vector3f(-1, -1, 0),
            new Vector3f(-1,  1, 1),
            new Vector3f( 1,  1, 1),
            new Vector3f( 1, -1, 1),
            new Vector3f(-1, -1, 1), 
    };
    Vector3f[] furstumCornersOut = new Vector3f[] {
            new Vector3f(-1,  1, 0),
            new Vector3f( 1,  1, 0),
            new Vector3f( 1, -1, 0),
            new Vector3f(-1, -1, 0),
            new Vector3f(-1,  1, 1),
            new Vector3f( 1,  1, 1),
            new Vector3f( 1, -1, 1),
            new Vector3f(-1, -1, 1), 
    };
    public void init() {
        int i;
        shadowSplitProj = new Matrix4f[NUM_SPLITS];
        shadowSplitMVP = new BufferedMatrix[NUM_SPLITS];
        shadowSplitDepth = new float[NUM_SPLITS];
        shadowCamFrustum = new Frustum[NUM_SPLITS];
        for (i = 0; i < shadowSplitProj.length; i++) {
            shadowSplitProj[i] = new Matrix4f();
        }
        for (i = 0; i < shadowSplitMVP.length; i++) {
            shadowSplitMVP[i] = new BufferedMatrix();
        }
        for (i = 0; i < shadowCamFrustum.length; i++) {
            shadowCamFrustum[i] = new Frustum();
        }
    }

    public void calcShadow(int split, Matrix4f modelview, Vector3f lightDirection, float shadowTextureSize) {
        Matrix4f.mul(shadowSplitProj[split], modelview, newMat);
        Matrix4f.transpose(newMat, newMat);
        Matrix4f.invert(newMat, newMatInv);
//        AABB bb = new AABB(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        for (int i = 0; i < 8; i++) {
            Matrix4f.transformTransposed(newMatInv, furstumCornersIn[i], furstumCornersOut[i]);
//            if (bb.minX > furstumCornersOut[i].x)
//                bb.minX = furstumCornersOut[i].x;
//            if (bb.maxX < furstumCornersOut[i].x)
//                bb.maxX = furstumCornersOut[i].x;
//            if (bb.minY > furstumCornersOut[i].y)
//                bb.minY = furstumCornersOut[i].y;
//            if (bb.maxY < furstumCornersOut[i].y)
//                bb.maxY = furstumCornersOut[i].y;
//            if (bb.minZ > furstumCornersOut[i].z)
//                bb.minZ = furstumCornersOut[i].z;
//            if (bb.maxZ < furstumCornersOut[i].z)
//                bb.maxZ = furstumCornersOut[i].z;
        }
        frustumCenter.set(0, 0, 0);
        for (int i = 0; i < 8; i++) {
            Vector3f.add(frustumCenter, furstumCornersOut[i], frustumCenter);
        }
        frustumCenter.scale(1.0f / 8.0f);
//                Engine.worldRenderer.debugBBs.put(split, bb);
        float radius = Vector3f.sub(furstumCornersOut[0], furstumCornersOut[6], tmp).length() / 2.0f; //constant with aspect ratio/fov/and mv scaling
        float snap = 5f;
        radius = GameMath.floor(radius/snap)*snap;
//        System.out.println(""+split+"/"+radius);
        float len = Vector3f.sub(furstumCornersOut[5], furstumCornersOut[1], tmp).length();
//        radius*=1.3f;
        /** SNAP TO TEXTURE INCREMENTS, (Seems not to work with deferred) */
        /*
        */
        //        Matrix4f scale = new Matrix4f();
        float texelsPerUnit = (radius * 2.0f) / (float) (shadowTextureSize);
//        
        //        scale.scale(new Vector3f(texelsPerUnit, texelsPerUnit, texelsPerUnit));
        Project.lookAt(0, 0, 0, lightDirection.x, lightDirection.y, lightDirection.z, 0, 1, 0, matLookAt);
        //        Matrix4f.mul(matLookAt, scale, matLookAt);
//        
        Matrix4f.invert(matLookAt, matLookAtInv);

//        texelsPerUnit = (1+split);
//        if (split != 0) {
            Matrix4f.transform(matLookAt, frustumCenter, frustumCenter);
            frustumCenter.scale(1.0f / texelsPerUnit);
//            frustumCenter.x = GameMath.floor(frustumCenter.x)+0.5f;
//            frustumCenter.y = GameMath.floor(frustumCenter.y)+0.5f;
//            frustumCenter.z = GameMath.floor(frustumCenter.z)+0.5f;
            frustumCenter.x = GameMath.floor(frustumCenter.x);
            frustumCenter.y = GameMath.floor(frustumCenter.y);
            frustumCenter.z = GameMath.floor(frustumCenter.z);
            frustumCenter.scale(texelsPerUnit);
            Matrix4f.transform(matLookAtInv, frustumCenter, frustumCenter);
//        }
        tmp.set(lightDirection);
        Vector3f.sub(frustumCenter, tmp.scale(-(512 + radius * 2.0f)), eye);

        shadowSplitMVP[split].setIdentity();
        Project.lookAt(eye.x, eye.y, eye.z, frustumCenter.x, frustumCenter.y, frustumCenter.z, 0, 1, 0, shadowSplitMVP[split]);
        
//      shadowCamFrustum[split].setCamPos(eye);
//      shadowCamFrustum[split].setView(shadowSplitMVP[split]);

//      matLookAt.load(shadowSplitMVP[split]);
//      matLookAt.translate(eye.x, eye.y, eye.z);
//
//      Matrix4f.invert(matLookAt, matLookAtInv);
//      Project.lookAt(eye.x-frustumCenter.x, eye.y-frustumCenter.y, eye.z-frustumCenter.z, 0,0,0, 0, 1, 0, shadowSplitMVP[split]);
        
        Project.orthoMat01(-radius, radius, -radius, radius, 0, 512*8, matOrtho);
        
//        if (!Engine.INVERSE_Z_BUFFER) {
//            Project.orthoMat(-radius, radius, radius, -radius, 0, 512 * 8, matOrtho);
//        } else {
        if (!Engine.INVERSE_MAP)
            Project.orthoMat01(-radius, radius, -radius, radius, 0, 512*8, matOrtho);
//            Project.orthoMat(-radius, radius, -radius, radius,512*8, 5, matOrtho);
//        }
        
//        Project.fovProjMatInfInvZ(90, 1, 512*8, matOrtho);
//        Project.fovProjMat(90, 1, 0.1f, 512*8f, matOrtho);
        
//        System.out.println(matOrtho);
//        Matrix4f.mul(matOrtho, matLookAt, matLookAt);

        Matrix4f.mul(matOrtho, shadowSplitMVP[split], shadowSplitMVP[split]);
//        shadowSplitMVP[split].load(Engine.getMatSceneMVP());
        shadowSplitMVP[split].update();
//        shadowSplitMVP[split].update();
//        shadowSplitDepth[split] = radius / 1.41F;// I'm 90% sure this is wrong, the shader requires 1 x radius and should reduce the input depth for each cascade as they are not centered on the same point
        shadowCamFrustum[split].set(shadowSplitMVP[split]);
////
//        float maxZ = splits[splits.length-1];
//        float minZ = splits[0];
//        float rangeZ = maxZ-minZ;
//        float farZ = (splits[split+1]);
////        
//        tmp.set(0, 0, radius);
//        Matrix4f.transform(matLookAtInv, tmp, tmp);
//        System.out.println(radius);
        shadowSplitDepth[split] = radius;//tmp.z*2;//splits[split+1];
        shadowSplitDepth[split] = splits[split+1];
//    System.out.println(split+"="+radius+"/"+farZ+"/"+tmp.z);
    }

    public void calcSplits(Matrix4f modelview, Vector3f lightDirection, float textureSize) {
        for (int i = 0; i < shadowSplitMVP.length; i++) {
            calcShadow(i, modelview, lightDirection, textureSize);
        }
    }
    public int checkFrustum(int i, AABBInt aabb, float f) {
        return shadowCamFrustum[i].checkFrustum(aabb, f);
    }

    public int checkFrustum(int i, AABBInt aabb) {
        return shadowCamFrustum[i].checkFrustum(aabb);
    }


    public FloatBuffer getSMVP(int i) {
        return shadowSplitMVP[i].get();
    }


    public void setSplits(float[] splits, float fieldOfView, float aspectRatio) {
        this.splits = splits;
        for (int i = 0; i < splits.length-1; i++) {
            float fov = fieldOfView;
//            if (i == 0)
//                fov *= 1.15f;
            Project.fovProjMat(fov, aspectRatio, splits[i], splits[i+1], shadowSplitProj[i]);
//            shadowSplitDepth[i] = splits[i+1] / 0.11F;

        }
    }   
    public void updateProjection(float znear, float zfar, float aspectRatio, float fov) {
        splits = new float[] {znear, 34, 124, 420};
//      splits = new float[] {znear, 12, 24, 66};
        splits = new float[] {znear, 16, 33, 122};
        splits = new float[] {znear, 16, 22, 277};
        if (!GameBase.VR_SUPPORT) {
            for (int i = 0; i < NUM_SPLITS; i++) {
                Project.fovProjMat(fov, aspectRatio, splits[i], splits[i+1], shadowSplitProj[i]);
            }
        } else {
            for (int i = 0; i < NUM_SPLITS; i++) {
                HmdMatrix44_t matR = VR.vrsystem.GetProjectionMatrix.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left, splits[i], splits[i+1], JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL);
                VR.convertSteamVRMatrix4ToMatrix4f(matR, shadowSplitProj[i]);
            }
        }
    }

    @Override
    public void release() {
        int i;
        for (i = 0; shadowSplitMVP != null && i < shadowSplitMVP.length; i++) {
            if (shadowSplitMVP[i] != null) {
                shadowSplitMVP[i].free();
            }
        }
    }

    @Override
    public void preinit() {
    }

}
