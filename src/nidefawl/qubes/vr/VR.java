package nidefawl.qubes.vr;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.HashSet;

import javax.management.RuntimeErrorException;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import com.sun.jna.Memory;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import jopenvr.*;
import jopenvr.JOpenVRLibrary.ETrackedDeviceClass;
import jopenvr.JOpenVRLibrary.EVRCompositorError;
import jopenvr.TrackedDevicePose_t.ByReference;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.SimpleResourceManager;
import nidefawl.qubes.vec.*;

public class VR {

	public static VR_IVRSystem_FnTable vrsystem;
	public static class VRViewProjection {
		public Matrix4f projLeft = new Matrix4f();
		public Matrix4f projRight = new Matrix4f();
		public Matrix4f poseEyeLeft = new Matrix4f();
		public Matrix4f poseEyeRight = new Matrix4f();
		public Matrix4f viewLeft=new Matrix4f();
		public Matrix4f viewRight=new Matrix4f();
		public Vector3f unifiedFrustumCameraOffset = new Vector3f();
		public void setEyeToHeadTransform() {
			HmdMatrix34_t matL = vrsystem.GetEyeToHeadTransform.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left);
//			matL.read();
			convertSteamVRMatrix3ToMatrix4f(matL, this.poseEyeLeft);
			this.poseEyeLeft.invert();
			HmdMatrix34_t matR = vrsystem.GetEyeToHeadTransform.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right);
//			matR.read();
			convertSteamVRMatrix3ToMatrix4f(matR, this.poseEyeRight);
			this.poseEyeRight.invert();
		}
		public void setEyeProj(float nearClip, float farClip)
		{
			HmdMatrix44_t matL = vrsystem.GetProjectionMatrix.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left, nearClip, farClip, JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL);
//			matL.read();
			convertSteamVRMatrix4ToMatrix4f(matL, this.projLeft);
			HmdMatrix44_t matR = vrsystem.GetProjectionMatrix.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right, nearClip, farClip, JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL);
//			matR.read();
			convertSteamVRMatrix4ToMatrix4f(matR, this.projRight);
		}
		public void update(float f) {
			Matrix4f.mul(this.poseEyeLeft, VR.hmdPose, viewLeft);
			Matrix4f.mul(this.poseEyeRight, VR.hmdPose, viewRight);
		}
        public void calcFrustumOffset() {
            //TODO: optimize if called each frame
            Matrix4f left = this.poseEyeLeft;
            Matrix4f right = this.poseEyeRight;
            Vector3f vL = new Vector3f();
            Vector3f vR = new Vector3f();
            Vector3f vCamDir = new Vector3f(0, 0, 1);
            Vector3f ipd = new Vector3f();
            Matrix4f.transform(left, vL, vL);
            Matrix4f.transform(right, vR, vR);
            Matrix4f.transform(right, vCamDir, vCamDir);
            Vector3f.sub(vL, vR, ipd);
            float fipd = ipd.x/2.0f*left.m00;
            vCamDir.scale(fipd*2.1f);
            unifiedFrustumCameraOffset.set(vCamDir);
        }
	}
	static class VRSettings {
		boolean seated;
		public boolean vrReverseHands;
	}
	public static VRSettings settings = new VRSettings();
	public static VRViewProjection cam = new VRViewProjection();
	public static FloatBuffer tlastVsync;
	public static LongBuffer _tframeCount;
	public static IntBuffer hmdDisplayFrequency;
	public static ByReference hmdTrackedDevicePoseReference;
	public static TrackedDevicePose_t[] hmdTrackedDevicePoses;
	public static double timePerFrame;
    public static boolean initCalled=false;
	public static boolean initSuccess;
    public static boolean initDone;
	public static String initStatus;
	public static VR_IVRCompositor_FnTable vrCompositor;
	public static IntBuffer hmdErrorStore;
	public static VR_IVROverlay_FnTable vrOverlay;
	public static VR_IVRRenderModels_FnTable vrRenderModels;
	public static VR_IVRSettings_FnTable vrSettings;
	final static Texture_t texType0 = new Texture_t();
	final static Texture_t texType1 = new Texture_t();


	public static Matrix4f[] poseMatrices;
	public static Vec3D	[] deviceVelocity;
    public static final Matrix4f hmdPose = new Matrix4f();
    public static final Matrix4f lasthmdPose = new Matrix4f();
	private static boolean headIsTracking;
	// Controllers
	private static int RIGHT_CONTROLLER = 0;
	private static int LEFT_CONTROLLER = 1;
	
	static boolean[] controllerTracking = new boolean[2];
	public static Matrix4f[] controllerPose = new Matrix4f[2];
	public static Matrix4f[] controllerRotation = new Matrix4f[2];
	public static int[] controllerDeviceIndex = new int[2];
	public static VRControllerState_t.ByReference[] inputStateRefernceArray = new VRControllerState_t.ByReference[2];
	public static VRControllerState_t[] lastControllerState = new VRControllerState_t[2];
	public static VRControllerState_t[] controllerStateReference = new VRControllerState_t[2];
	public static final int maxControllerVelocitySamples = 5;
	public static Vec3D[][] controllerVelocitySamples = new Vec3D[2][maxControllerVelocitySamples];
	public static int[] controllerVelocitySampleCount = new int[2];
	public static Matrix4f[] controllerTipTransform = new Matrix4f[2];
	public static Matrix4f[] handRotation = new Matrix4f[2];
	public static int renderWidth;
	public static int renderHeight;
    public static CGLRenderModelNative[] m_rTrackedDeviceToRenderModel;
    public static boolean[] isTrackedDeviceConnected;
    public static int[] trackedDeviceClass;
    static HashMap<String, CGLRenderModelNative> models = new HashMap<>();
    static HashSet<String> missingModels = new HashSet<>();
    public static boolean isInputCaptured;

	static {

		for (int c=0;c<2;c++)
		{
//			aimSource[c] = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
			for (int sample = 0; sample < 5; sample++)
			{
//				touchpadSamples[c][sample] = new Vector2f(0, 0);
			}
//			touchpadSampleCount[c] = 0;
            m_rTrackedDeviceToRenderModel = new CGLRenderModelNative[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
            isTrackedDeviceConnected = new boolean[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
            trackedDeviceClass = new int[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
            poseMatrices = new Matrix4f[JOpenVRLibrary.k_unMaxTrackedDeviceCount];

            for(int i=0;i<poseMatrices.length;i++) poseMatrices[i] = new Matrix4f();
            
			controllerPose[c] = new Matrix4f();
			controllerRotation[c] = new Matrix4f();
			controllerDeviceIndex[c] = -1;
			controllerTipTransform[c] = new Matrix4f();
			handRotation[c] = new Matrix4f();
			
			lastControllerState[c] = new VRControllerState_t();
			controllerStateReference[c] = new VRControllerState_t();
			inputStateRefernceArray[c] = new VRControllerState_t.ByReference();

			inputStateRefernceArray[c].setAutoRead(false);
			inputStateRefernceArray[c].setAutoWrite(false);
			inputStateRefernceArray[c].setAutoSynch(false);
			for (int i = 0; i < 5; i++)
			{
				lastControllerState[c].rAxis[i] = new VRControllerAxis_t();
			}

			//controllerVelocitySamples[c] = new Vec3[2][maxControllerVelocitySamples];
			controllerVelocitySampleCount[c] = 0;
			for (int i=0;i<maxControllerVelocitySamples;i++)
			{
				controllerVelocitySamples[c][i] = new Vec3D(0, 0, 0);
			}
		}
	}
	private static Pointer ptrFomrString(String in){
		Pointer p = new Memory(in.length()+1);
		p.setString(0, in);
		return p;

	}
//	public static void main(String[] args) {
//		if (!InitVR()) {
//			System.out.println("fail");
//			System.out.println("init "+initStatus);
//		}
//		
//	}

	static boolean InitVR() {
        initSuccess = false;
		try {
		    System.out.println("INIT VR");
		    
	        NativeLibrary.addSearchPath("openvr_api", ".");     

	        if(jopenvr.JOpenVRLibrary.VR_IsHmdPresent() == 0){
	            initStatus =  "VR Headset not detected.";
	            initSuccess = false;
	            return false;
	        }

			initializeJOpenVR();
			initOpenVRCompositor(true) ;
			initOpenVROverlay() ;	
			initOpenVROSettings();
			initOpenVRRenderModels();
			initModelShaders();
		} catch (Exception e) {
			e.printStackTrace();
			initSuccess = false;
			initStatus = e.getLocalizedMessage();
			return false;
		}

		deviceVelocity = new Vec3D[JOpenVRLibrary.k_unMaxTrackedDeviceCount];

		for(int i=0;i<poseMatrices.length;i++)
		{
			poseMatrices[i] = new Matrix4f();
			deviceVelocity[i] = new Vec3D(0, 0, 0);
		}
		cam.setEyeToHeadTransform();
		return true;
	
	}
	static Shader modelShader = null;
	private static void initModelShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_modelShader = assetMgr.loadShader(null, "model/openvr_model", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("ALPHA_TEST".equals(define)) {
                        return "#define ALPHA_TEST";
                    }
                    return null;
                }
            });
            modelShader = new_modelShader;
            modelShader.enable();
            modelShader.setProgramUniform1i("tex0", 0);
            modelShader.setProgramUniformMatrix4("model_matrix", false, Engine.getIdentityMatrix().get(), false);
            modelShader.setProgramUniformMatrix4("normal_matrix", false, Engine.getMatSceneNormal().get(), false);
            Shader.disable();
        } catch (ShaderCompileError e) {
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
        }
	}
    private static String GetTrackedDeviceString( int unDevice, int TrackedDeviceProperty )
	{              
        int buffsize=256;
        Pointer s=new Memory(buffsize);
        int unRequiredBufferLen = vrsystem.GetStringTrackedDeviceProperty.apply(unDevice,
                TrackedDeviceProperty,
                s, buffsize, hmdErrorStore);
        if (hmdErrorStore.get(0) == 0) {
            String id=s.getString(0);
            return id;
        }
        throw new RuntimeException("Failed reading device property string");
	}


	private static void SetupRenderModelForTrackedDevice(int unTrackedDeviceIndex) {
	    if( unTrackedDeviceIndex >= JOpenVRLibrary.k_unMaxTrackedDeviceCount )
	        return;
	    String sRenderModelName = GetTrackedDeviceString( unTrackedDeviceIndex, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_RenderModelName_String );
	    CGLRenderModelNative pRenderModel = FindOrLoadRenderModel(sRenderModelName);
	    if( pRenderModel != null )
	    {
	        m_rTrackedDeviceToRenderModel[ unTrackedDeviceIndex ] = pRenderModel;
        }
	    else
	    {
//	      String sTrackingSystemName = GetTrackedDeviceString( unTrackedDeviceIndex, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_TrackingSystemName_String );
//	      System.err.printf( "Unable to load render model for tracked device %d (%s.%s)\n", unTrackedDeviceIndex, sTrackingSystemName, sRenderModelName );
	  
	    }
    }

    private static CGLRenderModelNative FindOrLoadRenderModel(String sRenderModelName) {
        CGLRenderModelNative model = models.get(sRenderModelName);
        if (model != null) {
            return model;
        }
        if (missingModels.contains(sRenderModelName)) {
            return null;
        }
        PointerByReference byRef = new PointerByReference();
        int err = vrRenderModels.LoadRenderModel_Async.apply(ptrFomrString(sRenderModelName), byRef);
        if (err == JOpenVRLibrary.EVRRenderModelError.EVRRenderModelError_VRRenderModelError_Loading) {
            System.out.println("loading...");
            return null;
        }
        if (err != JOpenVRLibrary.EVRRenderModelError.EVRRenderModelError_VRRenderModelError_None) {
            System.err.printf( "Unable to load render model %s: err %d\n", sRenderModelName, err);
            missingModels.add(sRenderModelName);
            return null;
        }
        RenderModel_t t = new RenderModel_t(byRef.getValue());
        t.read();
        byRef = new PointerByReference();
        err = vrRenderModels.LoadTexture_Async.apply(t.diffuseTextureId, byRef);
        if (err == JOpenVRLibrary.EVRRenderModelError.EVRRenderModelError_VRRenderModelError_Loading) {
            System.out.println("loading...");
            return null;
        }
        if (err != JOpenVRLibrary.EVRRenderModelError.EVRRenderModelError_VRRenderModelError_None) {
            System.err.printf( "Unable to loading texture for render model %s: err %d\n", sRenderModelName, err);
            missingModels.add(sRenderModelName);
            return null;
        }
        RenderModel_TextureMap_t tex = new RenderModel_TextureMap_t(byRef.getValue());
        tex.read();
        CGLRenderModelNative rmodel = new CGLRenderModelNative(t, tex);
        models.put(sRenderModelName, rmodel);
        System.out.println("got model async "+rmodel);
        return rmodel;
    }
    public static void initOpenVRRenderModels() throws Exception
	{
		vrRenderModels = new VR_IVRRenderModels_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRRenderModels_Version, hmdErrorStore));
		if (vrRenderModels != null && hmdErrorStore.get(0) == 0) {
			vrRenderModels.setAutoSynch(false);
			vrRenderModels.read();
			int count = vrRenderModels.GetRenderModelCount.apply();
			Pointer pointer = new Memory(JOpenVRLibrary.k_unMaxPropertyStringSize);
			for (int i = 0; i < count; i++) {
				vrRenderModels.GetRenderModelName.apply(i, pointer, JOpenVRLibrary.k_unMaxPropertyStringSize - 1);
				String name = pointer.getString(0);
				System.out.println("Render Model " + i + ": " + name);
			}
			System.out.println("OpenVR RenderModels initialized OK");
		} else {
			throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));
		}
	}
	// needed for in-game keyboard
	public static void initOpenVROverlay() throws Exception
	{
		vrOverlay =   new VR_IVROverlay_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVROverlay_Version, hmdErrorStore));
		if (vrOverlay != null &&  hmdErrorStore.get(0) == 0) {     		
			vrOverlay.setAutoSynch(false);
			vrOverlay.read();					
			System.out.println("OpenVR Overlay initialized OK");
		} else {
			throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));		
		}
	}
	public static void initOpenVROSettings() throws Exception
	{
		vrSettings =   new VR_IVRSettings_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRSettings_Version, hmdErrorStore));
		if (vrSettings != null &&  hmdErrorStore.get(0) == 0) {     		
			vrSettings.setAutoSynch(false);
			vrSettings.read();					
			System.out.println("OpenVR Settings initialized OK");
			
			IntByReference e = new IntByReference();
		
			float ret =	vrSettings.GetFloat.apply(ptrFomrString("steamvr"), ptrFomrString("renderTargetMultiplier"), -1f, e);

			int a = 9;
			
		} else {
			throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));		
		}
	}
	private static void initializeJOpenVR() throws Exception {
		hmdErrorStore = IntBuffer.allocate(1);
		JOpenVRLibrary.VR_InitInternal(hmdErrorStore, JOpenVRLibrary.EVRApplicationType.EVRApplicationType_VRApplication_Scene);
		
		if( hmdErrorStore.get(0) == 0 ) {
			// ok, try and get the vrsystem pointer..
			vrsystem = new VR_IVRSystem_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRSystem_Version, hmdErrorStore));
		}
		if( vrsystem == null || hmdErrorStore.get(0) != 0 ) {
			throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));		
		} else {
			
			vrsystem.setAutoSynch(false);
			vrsystem.read();
			
			System.out.println("OpenVR initialized & VR connected.");
			
			tlastVsync = FloatBuffer.allocate(1);
			_tframeCount = LongBuffer.allocate(1);

			hmdDisplayFrequency = IntBuffer.allocate(1);
			hmdDisplayFrequency.put( (int) JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayFrequency_Float);
			hmdTrackedDevicePoseReference = new TrackedDevicePose_t.ByReference();
			hmdTrackedDevicePoses = (TrackedDevicePose_t[])hmdTrackedDevicePoseReference.toArray(JOpenVRLibrary.k_unMaxTrackedDeviceCount);

			timePerFrame = 1.0 / hmdDisplayFrequency.get(0);

			
			// disable all this stuff which kills performance
			hmdTrackedDevicePoseReference.setAutoRead(false);
			hmdTrackedDevicePoseReference.setAutoWrite(false);
			hmdTrackedDevicePoseReference.setAutoSynch(false);
			for(int i=0;i<JOpenVRLibrary.k_unMaxTrackedDeviceCount;i++) {
				hmdTrackedDevicePoses[i].setAutoRead(false);
				hmdTrackedDevicePoses[i].setAutoWrite(false);
				hmdTrackedDevicePoses[i].setAutoSynch(false);
			}

			initSuccess = true;
		}
		
	}

	public static void initOpenVRCompositor(boolean set) throws Exception
	{
		if( set && vrsystem.GetFloatTrackedDeviceProperty != null ) {
			vrCompositor = new VR_IVRCompositor_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRCompositor_Version, hmdErrorStore));
			if(vrCompositor != null && hmdErrorStore.get(0) == 0){                
				System.out.println("OpenVR Compositor initialized OK.");
				vrCompositor.setAutoSynch(false);
				vrCompositor.read();
				vrCompositor.SetTrackingSpace.apply(JOpenVRLibrary.ETrackingUniverseOrigin.ETrackingUniverseOrigin_TrackingUniverseStanding);
				
				int buffsize=20;
				Pointer s=new Memory(buffsize);

				//vrCompositor.GetTrackingSpace.apply();
//				debugOut();

				vrsystem.GetStringTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd,JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_ManufacturerName_String,s,buffsize,hmdErrorStore);
				String id=s.getString(0);
				System.out.println("Device manufacturer is: "+id);
				
//				if(!id.equals("HTC")) {
//					isVive=false;
//					mc.vrSettings.loadOptions();
//				}
//				
//				//TODO: detect tracking system
//				if(mc.vrSettings.seated && !isVive)
//					resetPosition();
//				else
//					clearOffset();
				
			} else {
				throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));			 
			}
		}
		if( vrCompositor == null ) {
			System.out.println("Skipping VR Compositor...");
//			if( vrsystem != null ) {
//				vsyncToPhotons = vrsystem.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_SecondsFromVsyncToPhotons_Float, hmdErrorStore);
//			} else {
//				vsyncToPhotons = 0f;
//			}
		}


		// texture type
		texType0.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
		texType0.eType = JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL;
		texType0.setAutoSynch(false);
		texType0.setAutoRead(false);
		texType0.setAutoWrite(false);
		texType0.handle = -1;
		texType0.write();

		
		// texture type
		texType1.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
		texType1.eType = JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL;
		texType1.setAutoSynch(false);
		texType1.setAutoRead(false);
		texType1.setAutoWrite(false);
		texType1.handle = -1;
		texType1.write();
		
		System.out.println("OpenVR Compositor initialized OK.");

	}


	
	public static String getCompostiorError(int code){
		switch (code){
		case EVRCompositorError.EVRCompositorError_VRCompositorError_DoNotHaveFocus:
			return "DoesNotHaveFocus";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_IncompatibleVersion:
			return "IncompatibleVersion";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_IndexOutOfRange:
			return "IndexOutOfRange";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_InvalidTexture:
			return "InvalidTexture";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_IsNotSceneApplication:
			return "IsNotSceneApplication";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_RequestFailed:
			return "RequestFailed";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_SharedTexturesNotSupported:
			return "SharedTexturesNotSupported";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_TextureIsOnWrongDevice:
			return "TextureIsOnWrongDevice";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_TextureUsesUnsupportedFormat:
			return "TextureUsesUnsupportedFormat:";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_None:
			return "None:";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_AlreadySubmitted:
			return "AlreadySubmitted:";
		}
		return "Unknown";
	}
	
	

	public static void updatePose(float f)
	{
		if ( vrsystem == null || vrCompositor == null || vrCompositor.WaitGetPoses == null)
			return;

        isInputCaptured = VR.vrsystem.IsInputFocusCapturedByAnotherProcess.apply()<=0;
		vrCompositor.WaitGetPoses.apply(hmdTrackedDevicePoseReference, JOpenVRLibrary.k_unMaxTrackedDeviceCount, null, 0);

		for (int nDevice = 0; nDevice < JOpenVRLibrary.k_unMaxTrackedDeviceCount; ++nDevice )
		{
			hmdTrackedDevicePoses[nDevice].read();
			if ( hmdTrackedDevicePoses[nDevice].bPoseIsValid != 0 )
			{
				HmdMatrix34_t deviceTrackingMat = hmdTrackedDevicePoses[nDevice].mDeviceToAbsoluteTracking;
				convertSteamVRMatrix3ToMatrix4f(deviceTrackingMat, poseMatrices[nDevice]);
				deviceVelocity[nDevice].x = hmdTrackedDevicePoses[nDevice].vVelocity.v[0];
				deviceVelocity[nDevice].y = hmdTrackedDevicePoses[nDevice].vVelocity.v[1];
				deviceVelocity[nDevice].z = hmdTrackedDevicePoses[nDevice].vVelocity.v[2];
			}
		}

		if ( hmdTrackedDevicePoses[JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd].bPoseIsValid != 0 )
		{
			hmdPose.load(poseMatrices[JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd]);
			//hellovr does hmdPose.invert() here
			hmdPose.invert();
//			Matrix4f.sub(hmdPose, lasthmdPose, lasthmdPose);
//			System.out.println(lasthmdPose.toString());
//			lasthmdPose.load(hmdPose);
			headIsTracking = true;
//			System.out.println("headIsTracking "+hmdPose);
		}
		else
		{
			headIsTracking = false;
			hmdPose.setIdentity();
			hmdPose.m31 = 1.62f;
		}

		findControllerDevices();
//
		for (int c=0;c<2;c++)
		{
			if (controllerDeviceIndex[c] != -1)
			{
				controllerTracking[c] = true;
				controllerPose[c].load(poseMatrices[controllerDeviceIndex[c]]);
			}
			else
			{
				controllerTracking[c] = false;
				controllerPose[c].setIdentity();
			}
		}
		getTipTransforms();
		cam.update(f);
	}
	static Pointer pointer;
	private static void getTipTransforms(){
		if (pointer == null)
		    pointer = new Memory(JOpenVRLibrary.k_unMaxPropertyStringSize);
		for (int i = 0; i < 2; i++) {
			if (controllerDeviceIndex[i] != -1 && !settings.seated) {
				vrsystem.GetStringTrackedDeviceProperty.apply(controllerDeviceIndex[i], JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_RenderModelName_String, pointer, JOpenVRLibrary.k_unMaxPropertyStringSize - 1, hmdErrorStore);
				RenderModel_ControllerMode_State_t modeState = new RenderModel_ControllerMode_State_t();
				RenderModel_ComponentState_t componentState = new RenderModel_ComponentState_t();
				vrRenderModels.GetComponentState.apply(pointer, ptrFomrString("tip"), controllerStateReference[i], modeState, componentState);
				convertSteamVRMatrix3ToMatrix4f(componentState.mTrackingToComponentLocal, controllerTipTransform[i]);
			} else {
				controllerTipTransform[i].setIdentity();
			}
		}
	}

	private static void findControllerDevices()
	{
		controllerDeviceIndex[RIGHT_CONTROLLER] = -1;
		controllerDeviceIndex[LEFT_CONTROLLER] = -1;
		
			if(settings.vrReverseHands){
				controllerDeviceIndex[RIGHT_CONTROLLER]  = vrsystem.GetTrackedDeviceIndexForControllerRole.apply(JOpenVRLibrary.ETrackedControllerRole.ETrackedControllerRole_TrackedControllerRole_LeftHand);
				controllerDeviceIndex[LEFT_CONTROLLER] = vrsystem.GetTrackedDeviceIndexForControllerRole.apply(JOpenVRLibrary.ETrackedControllerRole.ETrackedControllerRole_TrackedControllerRole_RightHand);
			}else {
				controllerDeviceIndex[LEFT_CONTROLLER]  = vrsystem.GetTrackedDeviceIndexForControllerRole.apply(JOpenVRLibrary.ETrackedControllerRole.ETrackedControllerRole_TrackedControllerRole_LeftHand);
				controllerDeviceIndex[RIGHT_CONTROLLER] = vrsystem.GetTrackedDeviceIndexForControllerRole.apply(JOpenVRLibrary.ETrackedControllerRole.ETrackedControllerRole_TrackedControllerRole_RightHand);
			}
	}

	public static void Submit() {
		if(VR.vrCompositor.Submit == null) return;
		if (VR.texType0.handle != fbLeft.getTexture(0)) {
			VR.texType0.handle = fbLeft.getTexture(0);
			VR.texType0.write();
		}
		if (VR.texType1.handle != fbRight.getTexture(0)) {
			VR.texType1.handle = fbRight.getTexture(0);
			VR.texType1.write();
		}
		int lret = VR.vrCompositor.Submit.apply(
				JOpenVRLibrary.EVREye.EVREye_Eye_Left,
				VR.texType0, null,
				JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);

		int rret = VR.vrCompositor.Submit.apply(
				JOpenVRLibrary.EVREye.EVREye_Eye_Right,
				VR.texType1, null,
				JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);
		if (lret == EVRCompositorError.EVRCompositorError_VRCompositorError_DoNotHaveFocus)
			lret = 0;
		
		if (rret == EVRCompositorError.EVRCompositorError_VRCompositorError_DoNotHaveFocus){
			rret = 0;
		}
		
		if(lret + rret > 0){
			throw new RuntimeException("Compositor Error: Texture submission error: Left/Right " + VR.getCompostiorError(lret) + "/" + VR.getCompostiorError(rret));		
		}
//		IntBuffer rtx = IntBuffer.allocate(1);
//		IntBuffer rty = IntBuffer.allocate(1);
//		VR.vrsystem.GetRecommendedRenderTargetSize.apply(rtx, rty);
//		if (rtx.get(0) != Game.displayWidth 
//				&& rty.get(0) != Game.displayHeight 
//				&& rtx.get(0) != renderWidth 
//				&& rty.get(0) != renderHeight) {
//			throw new RuntimeException("res changed: vr: "+rtx.get(0)+"x"+rty.get(0)+ " prev: "+renderWidth+"x"+renderHeight + " Game "+Game.displayWidth+"x"+Game.displayHeight);		
//		}

//		VR.vrCompositor.PostPresentHandoff.apply();
	}

	public static void initApp(GameBase instance) {
	    initCalled = true;
		if (VR.InitVR()) {
    		IntBuffer rtx = IntBuffer.allocate(1);
    		IntBuffer rty = IntBuffer.allocate(1);
    		VR.vrsystem.GetRecommendedRenderTargetSize.apply(rtx, rty);
    		renderWidth = rtx.get(0);
    		renderHeight = rty.get(0);
    	    System.out.printf("GetRecommendedRenderTargetSize %d %d\n", renderWidth, renderHeight);
    		initVRFB(renderWidth, renderHeight);
            VR.cam.setEyeProj(Engine.znear, Engine.zfar);
            VR.cam.setEyeToHeadTransform();
            VR.cam.calcFrustumOffset();
    		initDone = true;
        }
	}

	static int ticka = 0;
    public static void tick() {
        for (int unTrackedDevice = JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd + 1; unTrackedDevice < JOpenVRLibrary.k_unMaxTrackedDeviceCount; unTrackedDevice++) {
            trackedDeviceClass[unTrackedDevice] = vrsystem.GetTrackedDeviceClass.apply(unTrackedDevice);
            isTrackedDeviceConnected[unTrackedDevice] = vrsystem.IsTrackedDeviceConnected.apply(unTrackedDevice) > 0;
            if (isTrackedDeviceConnected[unTrackedDevice]) {
                SetupRenderModelForTrackedDevice( unTrackedDevice );
            }
        }
        if (ticka++>20) {
            ticka=0;
//            CGLRenderModelNative pRenderModel = FindOrLoadRenderModel( "oculus_cv1_controller_right" );
//            modelShader.release();
//            modelShader = null;
//            initModelShaders();
        }
    }

	static FrameBuffer fbLeft, fbRight;
	private static void initVRFB(int w, int h) {
		if (fbLeft != null) fbLeft.release();
		if (fbRight != null) fbRight.release();
        fbLeft = new FrameBuffer(w, h);
        fbLeft.setColorAtt(GL_COLOR_ATTACHMENT0, GL11.GL_RGBA8);
        fbLeft.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 1F, 1F);
        fbLeft.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 1F);
        fbLeft.setColorTexExtFmt(GL11.GL_RGBA);
        fbLeft.setColorTexExtType(GL11.GL_UNSIGNED_BYTE);
        fbLeft.setHasDepthAttachment();
        fbLeft.setup(null);
        fbRight = new FrameBuffer(w, h);
        fbRight.setColorAtt(GL_COLOR_ATTACHMENT0, GL11.GL_RGBA8);
        fbRight.setClearColor(GL_COLOR_ATTACHMENT0, 1F, 0F, 0F, 1F);
        fbRight.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 1F);
        fbRight.setColorTexExtFmt(GL11.GL_RGBA);
        fbRight.setColorTexExtType(GL11.GL_UNSIGNED_BYTE);
        fbRight.setHasDepthAttachment();
        fbRight.setup(null);
        FrameBuffer.unbindFramebuffer();
	}
	final static Matrix4f tmpMat = new Matrix4f();
	public static void setupCamera(int i, float f) {
		switch (i) {
		case 0:
			Engine.getMatSceneP().load(VR.cam.projLeft);
			Engine.getMatSceneP().update();
			getViewMat(0);
			break;
		case 1:
			Engine.getMatSceneP().load(VR.cam.projRight);
			Engine.getMatSceneP().update();
            getViewMat(1);
			break;
		case 2:
			Engine.getMatSceneP().load(Engine.getMatSceneP_internal());
			Engine.getMatSceneP().update();
			tmpMat.load(Engine.camera.getViewMatrix());
			break;
		}
    	Engine.updateCamera(tmpMat, Engine.camera.getPosition());
        UniformBuffer.updateUBO(null, f);
	}
	public static Matrix4f getViewMat(int i) {
        tmpMat.setIdentity();
//        tmpMat.rotate(Engine.camera.bearingAngle * GameMath.PI_OVER_180, 0f, 1f, 0f);
        switch (i) {
        case 0:
            Matrix4f.mul(cam.viewLeft, tmpMat, tmpMat);
            break;
        case 1:
        default:
            Matrix4f.mul(cam.viewRight, tmpMat, tmpMat);
            break;
        }
        return tmpMat;
	}

	
	public static boolean isInit() {
		return initDone;
	}

	public static FrameBuffer getFB(int i) {
		switch (i)
		{
		case 0:
			return fbLeft;
		case 1:
			return fbRight;
		case 2:
		default:
			return null;
		}
	}

	public static void setViewPort(int i) {
		switch (i) {
		case 0:
			Engine.setViewport(0, 0, fbLeft.getWidth(), fbLeft.getHeight());
			break;
		case 1:
			Engine.setViewport(0, 0, fbRight.getWidth(), fbRight.getHeight());
			break;
		case 2:
			Engine.setViewport(0, 0, fbRight.getWidth(), fbRight.getHeight());
			break;
		}
	}
	
	

    public static Matrix4f convertSteamVRMatrix3ToMatrix4f(HmdMatrix34_t hmdMatrix, Matrix4f mat){
        Matrix4fSet(mat,
                hmdMatrix.m[0], hmdMatrix.m[1], hmdMatrix.m[2], hmdMatrix.m[3],
                hmdMatrix.m[4], hmdMatrix.m[5], hmdMatrix.m[6], hmdMatrix.m[7],
                hmdMatrix.m[8], hmdMatrix.m[9], hmdMatrix.m[10], hmdMatrix.m[11],
                0f, 0f, 0f, 1f
        );
        return mat;
    }
    
    public static Matrix4f convertSteamVRMatrix4ToMatrix4f(HmdMatrix44_t hmdMatrix, Matrix4f mat)
    {
        Matrix4fSet(mat, hmdMatrix.m[0], hmdMatrix.m[1], hmdMatrix.m[2], hmdMatrix.m[3],
                hmdMatrix.m[4], hmdMatrix.m[5], hmdMatrix.m[6], hmdMatrix.m[7],
                hmdMatrix.m[8], hmdMatrix.m[9], hmdMatrix.m[10], hmdMatrix.m[11],
                hmdMatrix.m[12], hmdMatrix.m[13], hmdMatrix.m[14], hmdMatrix.m[15]);
        return mat;
    }
    public static void Matrix4fSet(Matrix4f mat, float m11, float m12, float m13, float m14, float m21, float m22, float m23, float m24, float m31, float m32, float m33, float m34, float m41, float m42, float m43, float m44)
    {
        mat.m00 = m11;
        mat.m10 = m12;
        mat.m20 = m13;
        mat.m30 = m14;
        mat.m01 = m21;
        mat.m11 = m22;
        mat.m21 = m23;
        mat.m31 = m24;
        mat.m02 = m31;
        mat.m12 = m32;
        mat.m22 = m33;
        mat.m32 = m34;
        mat.m03 = m41;
        mat.m13 = m42;
        mat.m23 = m43;
        mat.m33 = m44;
    }

    public static void drawFullscreenCompanion(int guiWidth, int guiHeight) {
        Shaders.textured.enable();
        Tess.instance.setColor(-1, 255);
//        Tess.instance.setOffset(displayWidth / 2, displayHeight / 2, 0);
        float x = 0;
        float y = 0;
        float w = guiWidth/2.0f;
        float h = guiHeight;
        Tess.instance.add(x, y, 0, 0, 1);
        Tess.instance.add(x, y+h, 0, 0, 0);
        Tess.instance.add(x+w, y+h, 0, 1, 0);
        Tess.instance.add(x+w, y, 0, 1, 1);
        GL.bindTexture(GL13.GL_TEXTURE0, GL_TEXTURE_2D, VR.getFB(0).getTexture(0));
        Tess.instance.drawQuads();
        x+=w;
        Tess.instance.add(x, y, 0, 0, 1);
        Tess.instance.add(x, y+h, 0, 0, 0);
        Tess.instance.add(x+w, y+h, 0, 1, 0);
        Tess.instance.add(x+w, y, 0, 1, 1);
        GL.bindTexture(GL13.GL_TEXTURE0, GL_TEXTURE_2D, VR.getFB(1).getTexture(0));
//        CGLRenderModelNative pRenderModel = FindOrLoadRenderModel( "oculus_cv1_controller_right" );
//        if (pRenderModel != null) {
//            GL.bindTexture(GL13.GL_TEXTURE0, GL_TEXTURE_2D, pRenderModel.texId);
//        }
        Tess.instance.drawQuads();
        Shader.disable();
    }

    public static void shutdown() {
        if (vrsystem!=null) {
            JOpenVRLibrary.VR_ShutdownInternal();
        }
    }

    static Vector4f tmp1 = new Vector4f();
    static Vector4f tmp2 = new Vector4f();
    static Vector3f tmp3 = new Vector3f();
    public static void renderControllers() {
        if (modelShader == null) return;
        BufferedMatrix bufMat = Engine.getTempMatrix();
        if (isInputCaptured) {
            Shaders.colored3D.enable();
            Shaders.colored3D.setProgramUniform1f("color_brightness", 1f);
            Engine.checkGLError("Shaders.textured3D.enable");
            for (int iDevice = 0; iDevice < JOpenVRLibrary.k_unMaxTrackedDeviceCount; iDevice++) {
                if (!isTrackedDeviceConnected[iDevice]) {
                    continue;
                }
                if (trackedDeviceClass[iDevice] != ETrackedDeviceClass.ETrackedDeviceClass_TrackedDeviceClass_Controller) {
                    continue;
                }
                if (hmdTrackedDevicePoses[iDevice].bPoseIsValid <= 0 ){
                    continue;
                }
                Matrix4f n = poseMatrices[iDevice];
                renderControllerAxes(iDevice, n);
            }

            Shaders.colored3D.setProgramUniform1f("color_brightness", 0.1f);
        }
        Engine.bindVAO(GLVAO.openVRModel);
        Engine.checkGLError("bindVAO");
        modelShader.enable();
        for (int iDevice = 0; iDevice < JOpenVRLibrary.k_unMaxTrackedDeviceCount; iDevice++) {
            if (!isTrackedDeviceConnected[iDevice]) {
                continue;
            }
            if (!isInputCaptured && trackedDeviceClass[iDevice] == ETrackedDeviceClass.ETrackedDeviceClass_TrackedDeviceClass_Controller) {
                continue;
            }
            if (hmdTrackedDevicePoses[iDevice].bPoseIsValid <= 0 ){
                continue;
            }
            CGLRenderModelNative model = m_rTrackedDeviceToRenderModel[iDevice];
            if (model == null)
                continue;
//            System.out.println(iDevice+","+GetTrackedDeviceString( iDevice, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_RenderModelName_String ));
            Matrix4f n = poseMatrices[iDevice];
            bufMat.load(n);
            bufMat.update();
            modelShader.setProgramUniformMatrix4("model_matrix", false, bufMat.get(), false);
            bufMat.clearTranslation();
            bufMat.invert();
            bufMat.transpose();
            bufMat.update();
            modelShader.setProgramUniformMatrix4("normal_matrix", false, bufMat.get(), false);
            model.render();
            
        }
        if (false) {
            CGLRenderModelNative pRenderModel = FindOrLoadRenderModel( "oculus_cv1_controller_right" );
            if (pRenderModel != null) {
                bufMat.setIdentity();
                bufMat.scale(10);
                bufMat.update();
                modelShader.setProgramUniformMatrix4("model_matrix", false, bufMat.get(), false);
                bufMat.clearTranslation();
                bufMat.invert();
                bufMat.transpose();
                bufMat.update();
                modelShader.setProgramUniformMatrix4("normal_matrix", false, bufMat.get(), false);
                Engine.checkGLError("setProgramUniformMatrix4");
                pRenderModel.render();
            }
        }
        Engine.bindVAO(null);
//        Shaders.textured3D.setProgramUniformMatrix4("model_matrix", false, Engine.getIdentityMatrix().get(), false);
        
    }
    private static void renderControllerAxes(int iDevice, Matrix4f n) {
        tmp1.set(0, 0, 0, 1);
        Matrix4f.transform(n, tmp1, tmp1);
        Tess t = Tess.instance;
        for ( int i = 0; i < 3; ++i )
        {
            Vector3f color = tmp3;
            color.set(0, 0, 0);
            color.setElement(i, 1.0f);
            Vector4f point = tmp2;
            point.set(0, 0, 0, 1);
            point.setElement(i, 0.05f);
            Matrix4f.transform(n, point, point);
            t.setColorRGBAF(color.x, color.y, color.z, 1.0f);
            t.add(tmp1);
            t.add(point);
        }
        tmp1.set(0, 0, -0.02f, 1);
        tmp2.set(0, 0, -39.f, 1);
        Matrix4f.transform(n, tmp1, tmp1);
        Matrix4f.transform(n, tmp2, tmp2);
        t.setColorRGBAF(.92f, .92f, .71f, 1.0f);
        t.add(tmp1);
        t.add(tmp2);
        GL11.glLineWidth(2.0f);
        t.draw(GL11.GL_LINES);
        Engine.checkGLError("t.draw(GL11.GL_LINES)");
    }

}
