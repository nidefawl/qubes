/**
 * 
 */
package nidefawl.qubes.render.post;

import java.io.File;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class HBAOPlus {
  //---------------------------------------------------------------------------------------------------
 // Remarks:
//     * The final occlusion is a weighted sum of 2 occlusion contributions. The DetailAO and CoarseAO parameters are the weights.
//     * Setting the DetailAO parameter to 0.0 (default value) is fastest and avoids over-occlusion artifacts on alpha-tested geometry.
//     * Setting the DepthStorage parameter to FP16_VIEW_DEPTHS can be slightly faster but may introduce artifacts for large view depths.
 //---------------------------------------------------------------------------------------------------
    native public static void createContext(int width, int height);
	native public static void deleteContext();
	native public static void renderAO();
    native public static void setProjMatrix(long projMatPtr);
    native public static void setOutputFBO(int outputFBO);
    native public static void setDepthTex(int depthTex);
    native public static void setNormalTex(int normalTex);
    /** 
     * To hide low-tessellation artifacts // 0.0~0.5
     * @param f (default is 0.2f)
     */
    native public static void setBias(float f);
    /** 
     * The AO radius in meters
     * @param f (default is 2.0f)
     */
    native public static void setRadius(float f);
    
    /** 
     * Scale factor for the detail AO, the greater the darker // 0.0~2.0
     * @param f (default is 0.f)
     */
    native public static void setDetailAO(float f);
    
    /** 
     * Scale factor for the coarse AO, the greater the darker // 0.0~2.0
     * @param f (default is 1.f)
     */
    native public static void setCoarseAO(float f);
    
    /** 
     * The final AO output is pow(AO, powerExponent) // 1.0~8.0
     * @param powerExponent (default is 2.f)
     */
    native public static void setPowerExponent(float fpowerExponent);
    
    /** 
     * FP16 = less bandwith + less memory but may introduce false-occlusion artifacts
     * @param mode GFSDK_SSAO_FP32_VIEW_DEPTHS (0) or GFSDK_SSAO_FP16_VIEW_DEPTHS (1)
     */
    native public static void setDepthStorage(int mode);
    
    /** 
     * To hide possible false-occlusion artifacts near screen borders
     * @param mode GFSDK_SSAO_CLAMP_TO_EDGE (0) or GFSDK_SSAO_CLAMP_TO_BORDER (1)
     */
    native public static void setDepthClampMode(int mode);
    
    /**
     * To return white AO for ViewDepths > MaxViewDepth
     * @param enabled false
     * @param MaxViewDepth 0.f Custom view-depth threshold
     * @param Sharpness 100.f The higher, the sharper are the AO-to-white transitions
     */
    native public static void setDepthThreshold(boolean enabled, float MaxViewDepth, float Sharpness);
    
    /**
     * To blur the AO with an edge-preserving blur
     * @param enabled true  To blur the AO with an edge-preserving blur
     * @param Radius GFSDK_SSAO_BLUR_RADIUS_8 (8)  BLUR_RADIUS_2, BLUR_RADIUS_4 or BLUR_RADIUS_8
     * @param Sharpness 4.f The higher, the more the blur preserves edges // 0.0~16.0
     */
    native public static void setBlur(boolean enabled, int Radius, float Sharpness);
    

    /**
     * When enabled, the actual per-pixel blur sharpness value depends on the per-pixel view depth with
     *  <pre>{@code
     * LerpFactor = (PixelViewDepth - ForegroundViewDepth) / (BackgroundViewDepth - ForegroundViewDepth)
     * Sharpness = lerp(Sharpness*ForegroundSharpnessScale, Sharpness, saturate(LerpFactor))
     *  }</pre>
     *  
     * @param enabled Default: false - To make the blur sharper in the foreground
     * @param ForegroundSharpnessScale Default: 4.f - Sharpness scale factor for ViewDepths <= ForegroundViewDepth
     * @param ForegroundViewDepth default 0.f - Maximum view depth of the foreground depth range
     * @param BackgroundViewDepth default 1.f - Minimum view depth of the background depth range
     */
    native public static void setBlurSharpen(boolean enabled, float ForegroundSharpnessScale, float ForegroundViewDepth, float BackgroundViewDepth);
    
    
    // ----- DEBUG ONLY ---------
    native public static void debugControl(int code);
    native public static String[] getCallStack();
    
	static {
	    File f = new File("HBAOPlus.x64.dll");
	    if (!f.exists()) {
	        f = new File("../Game/lib/hbaoplus/HBAOPlus.x64.dll");
	    }
		System.load(f.getAbsolutePath());
	}
	
}