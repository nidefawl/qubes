#version 150 core
#extension GL_ARB_shader_storage_buffer_object : enable


#pragma include "ubo_scene.glsl"
#pragma include "ubo_shadow.glsl"
#pragma include "blockinfo.glsl"
#pragma include "sky_scatter.glsl"
#pragma include "unproject.glsl"
#pragma include "tonemap.glsl"
#pragma define "RENDER_PASS"
#pragma define "BLUE_NOISE"
#pragma define "RENDER_MATERIAL_BUFFER" "0"
#pragma define "RENDER_VELOCITY_BUFFER" "0"
#pragma define "RENDER_AMBIENT_OCCLUSION"
#pragma define "SHADOW_MAP_RESOLUTION" "2048"

float isEyeInWater = 0.0;

layout(set = 3, binding = 0, std140) uniform LightInfo
{
  vec4 dayLightTime; 
  vec4 posSun; // Light position in world space
  vec4 lightDir; // Light dir in world space
  vec4 La; // Ambient light intensity
  vec4 Ld; // Diffuse light intensity
  vec4 Ls; // Specular light intensity
} SkyLight;


struct SurfaceProperties {
    vec3    albedo;         //Diffuse texture aka "color texture"
    vec3    normal;         //Screen-space surface normals
    float   depth;          //Scene depth
    float   linearDepth;    //Linear depth
    float   NdotL;
    vec4    position;       // camera/eye space position
    vec4    worldposition;  // world space position
    vec3    viewVector;     //Vector representing the viewing direction
    uvec4   blockinfo;
    float   sunSpotDens;
    vec4    blockLight;
    vec4    light;
    float sunTheta;
    float roughness;
    float reflective;
} prop;


layout (set = 1, binding = 0) uniform sampler2D texColor;
layout (set = 1, binding = 1) uniform sampler2D texNormals;
layout (set = 1, binding = 2) uniform usampler2D texMaterial;
layout (set = 1, binding = 3) uniform sampler2D texBlockLight;
layout (set = 1, binding = 4) uniform sampler2D texDepth;
layout (set = 1, binding = 5) uniform sampler2D texShadow;
layout (set = 1, binding = 6) uniform sampler2D texLight;
#if RENDER_AMBIENT_OCCLUSION
layout (set = 1, binding = 7) uniform sampler2D texAO;
#endif
#if RENDER_PASS == 1
layout (set = 1, binding = 7) uniform sampler2D texDepthPreWater;
layout (set = 1, binding = 8) uniform sampler2D texWaterNoise;
#endif
#ifdef BLUE_NOISE
uniform int texSlotNoise;
layout (set = 1, binding = 9) uniform sampler2DArray texArrayNoise;
#endif


#if RENDER_VELOCITY_BUFFER
uniform mat4 mat_reproject;
#endif


in vec2 pass_texcoord;

in float dayNoon;
in float nightNoon;
in float dayLightIntens;
in float lightAngleUp;
in float moonSunFlip;

out vec4 out_Color;
#if RENDER_MATERIAL_BUFFER
out vec4 out_FinalMaterial;
#endif
#if RENDER_VELOCITY_BUFFER
out vec4 out_Velocity;
#endif

// this needs to be included after sampler definition
#if RENDER_PASS == 1
#define noisetex texWaterNoise
#pragma include "water.glsl"
#endif




vec4 getShadowTexcoord(in mat4 shadowMVP, in vec4 worldpos) {
    vec4 v2 = shadowMVP * worldpos;
#if Z_INVERSE
    v2.xy = v2.xy * 0.5 + 0.5;
#else
    v2 = v2 * 0.5 + 0.5;
#endif

    return v2;
}

vec4 dbgSplit = vec4(0);
const float clampmin = 1.0/SHADOW_MAP_RESOLUTION;//1.0/8.0;
const float clampmax = 1.0-clampmin;
bool canLookup(in vec4 v, in float zPos, in float mapZ) {
    if (clamp(v.x, clampmin, clampmax) == v.x && clamp(v.y, clampmin, clampmax) == v.y) {
        // if (zPos < mapZ) {
        //     return true;
        // }
#ifdef VULKAN_GLSL
        if (v.z >= 0 && v.z <= 1) {
            return true;
        }
#endif
#if Z_INVERSE
        if (v.z > 0) {
            return true;
        }
#elif defined VULKAN_GLSL
        if (v.z < 1) {
            return true;
        }
#else
        if (v.z < 1&&v.z > -1) {
            return true;
        }
#endif
    }
    return false;
    // return clamp(v.x, clampmin, clampmax) == v.x && clamp(v.y, clampmin, clampmax) == v.y && zPos > mapZ;
}



#define SAMPLE_DISTANCE ((1.0/SHADOW_MAP_RESOLUTION) / 4.0)
#define SOFT_SHADOW_TAP_RANGE 1
#define SOFT_SHADOW_TAP_RANGE2 1
#define SAMPLE_DISTANCE2 ((1.0/SHADOW_MAP_RESOLUTION) / 4.0)
#define SOFT_SHADOW_WEIGHT ((SOFT_SHADOW_TAP_RANGE*2+1)*(SOFT_SHADOW_TAP_RANGE*2+1))
#define SOFT_SHADOW_WEIGHT2 ((SOFT_SHADOW_TAP_RANGE2*2+1)*(SOFT_SHADOW_TAP_RANGE2*2+1))
#if defined VULKAN_GLSL
#define SHADOW_FACTOR0 0.9999
#define SHADOW_FACTOR1 0.9999
#define SHADOW_FACTOR2 0.9999
#define SHADOW_COMPARE(a, b) a <= b
#elif Z_INVERSE
#define SHADOW_FACTOR0 1.00002
#define SHADOW_FACTOR1 1.00005
#define SHADOW_FACTOR2 1.00007
#define SHADOW_COMPARE(a, b) a >= b
#else
//absolutly sucks
#define SHADOW_FACTOR0 0.99999
#define SHADOW_FACTOR1 0.99998
#define SHADOW_FACTOR2 0.99996
#define SHADOW_COMPARE(a, b) a <= b
#endif
#if 1
#endif

float getShadowAt(vec4 worldPos, float linDepth, float zOffset) {
    vec4 v = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[0], worldPos);
    vec4 v2 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[1], worldPos);
    vec4 v3 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[2], worldPos);
    float weight = 0.9;
    vec4 mapZSplits = in_matrix_shadow.shadow_split_depth;
    if (canLookup(v, linDepth, mapZSplits.x*0.9)) {
        v.z*=SHADOW_FACTOR0;
#if SOFT_SHADOW_TAP_RANGE2 < 1
        return SHADOW_COMPARE(v.z, texture(texShadow, v.xy*0.5).r) ? 1.0 : 0.0;
#else
        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE2; x <= SOFT_SHADOW_TAP_RANGE2; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE2; y <= SOFT_SHADOW_TAP_RANGE2; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE2;
                s += texture(texShadow, v.xy*0.5+offs).r;   
            }
        }
        s /= SOFT_SHADOW_WEIGHT2;
        return SHADOW_COMPARE(v.z, s) ? 1.0 : 0.0;
#endif
    }
    if (canLookup(v2, linDepth, mapZSplits.y*weight)) {
        v2.z*=SHADOW_FACTOR1;
#if SOFT_SHADOW_TAP_RANGE2 < 1
        return SHADOW_COMPARE(v2.z, texture(texShadow, v2.xy*0.5+vec2(0.5,0)).r) ? 1.0 : 0.0;
#else
        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE2; x <= SOFT_SHADOW_TAP_RANGE2; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE2; y <= SOFT_SHADOW_TAP_RANGE2; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE2;
                s += texture(texShadow, v2.xy*0.5+vec2(0.5,0)+offs).r;   
            }
        }
        s /= SOFT_SHADOW_WEIGHT2;
        return SHADOW_COMPARE(v2.z, s) ? 1.0 : 0.0;
#endif
    }
    if (canLookup(v3, linDepth, mapZSplits.z)) {
        v3.z*=SHADOW_FACTOR2;
#if SOFT_SHADOW_TAP_RANGE2 < 1
        return SHADOW_COMPARE(v3.z, texture(texShadow, v3.xy*0.5+vec2(0,0.5)).r) ? 1.0 : 0.0;
#else
        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE2; x <= SOFT_SHADOW_TAP_RANGE2; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE2; y <= SOFT_SHADOW_TAP_RANGE2; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE2;
                s += texture(texShadow, v3.xy*0.5+vec2(0,0.5)+offs).r;   
            }
        }
        s /= SOFT_SHADOW_WEIGHT2;
        return SHADOW_COMPARE(v3.z, s) ? 1.0 : 0.0;
#endif
    }
    return 1.0;
}
float lookupShadowMap(vec3 v, float factor) {
    #ifdef VULKAN_GLSL
        factor-=(1.0-prop.depth)*0.0002;
    #endif
    v.z*=factor;

    #if SOFT_SHADOW_TAP_RANGE == 0
        float shadowSample = texture(texShadow, v.xy).r;
        #ifdef VULKAN_GLSL
            return step(v.z, 1.0-shadowSample);
        #endif
        return step(shadowSample, v.z);
    #else
        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE; x <= SOFT_SHADOW_TAP_RANGE; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE; y <= SOFT_SHADOW_TAP_RANGE; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE;
                vec2 shadow_tc = v.xy+offs;
                float shadowSample = texture(texShadow, shadow_tc).r;
                #ifdef VULKAN_GLSL
                    shadowSample = 1.0 -shadowSample;
                #endif
                if (SHADOW_COMPARE(v.z, shadowSample)) {
                    s += 1;
                }
            }
        }
        s /= SOFT_SHADOW_WEIGHT;
        return s;
    #endif
}
float getShadow2() {

    vec4 v = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[0], prop.worldposition);
    vec4 v2 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[1], prop.worldposition);
    vec4 v3 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[2], prop.worldposition);
    vec2 cPos = pass_texcoord*2.0-1.0;
    vec4 mapZSplits = in_matrix_shadow.shadow_split_depth;
    float s = 0.0;
    int steps = 0;
    if (canLookup(v, prop.linearDepth, mapZSplits.x)) {
        // v.z = 1.0-v.z;
        s += lookupShadowMap(vec3(v.xy*0.5, v.z), SHADOW_FACTOR0);
        dbgSplit.xyz=v.xyz;
        dbgSplit.a=1;
        // prop.albedo.r = 1.0;
        // if (clamp(v.z, 0.15, 0.85) == v.z)
            steps++;
    }
    if (steps<1&&canLookup(v2, prop.linearDepth, mapZSplits.y)) {
        s += lookupShadowMap(vec3(v2.xy*0.5+vec2(0.5,0.0), v2.z), SHADOW_FACTOR1);
        dbgSplit.y=1;
        dbgSplit.a=1;
        // if (clamp(v2.z, 0.15, 0.85) == v2.z)
            steps++;
    }
    if (steps<1&&canLookup(v3, prop.linearDepth, mapZSplits.z)) {
        s += lookupShadowMap(vec3(v3.xy*0.5+vec2(0.0,0.5), v3.z), SHADOW_FACTOR2);
        dbgSplit.z=1;
        dbgSplit.a=1;
        steps++;
    }
    if (steps > 0)
        s/=steps;
    // return 1.0;
        return clamp(s, 0, 1);
}
// Mie scaterring approximated with Henyey-Greenstein phase function.
#define G_SCATTERING 0.87f
#define VOL_STRENGTH 0.05f
#define NB_STEPS 5
// #define PI 10
const float pi = 3.1415927;
float ComputeScattering(float lightDotView)
{
    float result = 1.0f - G_SCATTERING;
    result *= result;
    result /= (4.0f * pi * pow(1.0f + G_SCATTERING * G_SCATTERING - (2.0f * G_SCATTERING) * lightDotView, 1.7f));
    return result;
}

float VolumetricLight() {
#ifdef BLUE_NOISE
    ivec3 texSize = textureSize(texArrayNoise, 0);
    vec2 pixelSize = vec2(in_scene.viewport.xy)/vec2(texSize.xy);
    vec4 tex = texture(texArrayNoise, vec3(pass_texcoord*pixelSize, texSlotNoise), 0);
    float dither = tex.r;
#else 
    ivec2 pixelPos = ivec2(pass_texcoord.xy*in_scene.viewport.xy);
    vec4 ditherPattern[4];
    ditherPattern[0] = vec4(0.0f, 0.5f, 0.125f, 0.625f);
    ditherPattern[1] = vec4( 0.75f, 0.22f, 0.875f, 0.375f);
    ditherPattern[2] = vec4( 0.1875f, 0.6875f, 0.0625f, 0.5625);
    ditherPattern[3] = vec4( 0.9375f, 0.4375f, 0.8125f, 0.3125);

    float dither = ditherPattern[pixelPos.x%4][pixelPos.y%4];
#endif

    vec3 startPosition = CAMERA_POS;
     
    vec3 rayVector = prop.worldposition.xyz - startPosition;
     
    float rayLength = min(120000, length(rayVector));

    float stepLength = rayLength / NB_STEPS;
     
    vec3 stepSize = -prop.viewVector * stepLength;
    float amount = ComputeScattering(prop.sunTheta);
    vec3 currentPosition = startPosition+dither*stepSize;
    float accumFog = 0;
    float accumDepth = dither*stepLength;
    float minDepth = 100000;
    float maxDepth = -100000;
    for (int i = 0; i < NB_STEPS; i++)
    {
        vec4 worldPos = vec4(currentPosition, 1.0);
        float linDepth = accumDepth;
        minDepth = min(minDepth, linDepth);
        maxDepth = max(maxDepth, linDepth);
        float shadowSample = getShadowAt(worldPos, linDepth, 0);
        if (shadowSample > 0.5)
        {
            accumFog += amount;
        }
        currentPosition += stepSize;
        accumDepth += stepLength;
    }
    // if (pixelPos.x==int(in_scene.viewport.x/2)&&pixelPos.y==int(in_scene.viewport.y/2)) {
    //     debugBuf.debugVals[0] = minDepth;
    //     debugBuf.debugVals[1] = maxDepth;  
    // }
    accumFog /= NB_STEPS;
    return clamp(accumFog, 0, 1);
}

#define EDO_Size 20.0                               //Set ambient occlusion size. [10.0 20.0 30.0 40.0 50.0 60.0]
#define EDOPASS 8.0
#define EDOSTR 1.0   
#define EDO   
#ifdef EDO

float pw = 1.0/ in_scene.viewport.x;
float ph = 1.0/ in_scene.viewport.y;
float getdist(float rng, vec2 texcoord) {
    return 1-clamp(linearizeDepth(texture(texDepth,texcoord.xy).r)/rng*Z_NEAR,0,1);
}

float getnoise(vec2 pos) {
    return abs(fract(sin(dot(pos ,vec2(18.9898f,28.633f))) * 4378.5453f));
}

float edepth(vec2 coord) {
    return texture(texDepth,coord).z;
}
float edo() {
    vec2 texcoord = pass_texcoord.st;
    //edge detect
    float total = 0;
    float d = edepth(texcoord.xy);
    float dtresh = 1.0/(Z_FAR-Z_NEAR)/1.0;
    vec4 dc = vec4(d,d,d,d);
    vec4 sa;
    vec4 sb;
    
    float dist = (getdist(32,texcoord)*2+getdist(512,texcoord))/3;
#ifdef BLUE_NOISE
    ivec3 texSize = textureSize(texArrayNoise, 0);
    vec2 pixelSize = vec2(in_scene.viewport.xy)/vec2(texSize.xy);
    vec4 tex = texture(texArrayNoise, vec3(pass_texcoord*pixelSize, texSlotNoise), 0);
    float noise = 1+tex.r*3/EDOPASS;
#else 
    float noise = 1+getnoise(texcoord.xy)*3/EDOPASS;
#endif
    float border = floor(EDO_Size/EDOPASS*in_scene.viewport.x/1280);
    float strn = border*dist*noise;
    
    float e = 0;
    
    for (int i = 0; i < EDOPASS; i++) {
        sa.x = edepth(texcoord.xy + vec2(-pw,-ph)*strn*i);
        sa.y = edepth(texcoord.xy + vec2(pw,-ph)*strn*i);
        sa.z = edepth(texcoord.xy + vec2(-pw,0.0)*strn*i);
        sa.w = edepth(texcoord.xy + vec2(0.0,ph)*strn*i);
        // if (sa.w > d) {
        //     sa.w = 1.0;
        // }
        //opposite side samples
        sb.x = edepth(texcoord.xy + vec2(pw,ph)*strn*i);
        sb.y = edepth(texcoord.xy + vec2(-pw,ph)*strn*i);
        // if (sb.y > d) {
        //     sb.y = 1.0;
        // }
        sb.z = edepth(texcoord.xy + vec2(pw,0.0)*strn*i);
        sb.w = edepth(texcoord.xy + vec2(0.0,-ph)*strn*i);
        
        vec4 dd = (2.0* dc - sa - sb) - dtresh;
        dd = vec4(step(dd.x,0.0),step(dd.y,0.0),step(dd.z,0.0),step(dd.w,0.0));
        
        e = clamp(dot(dd,vec4(0.25f)),0.0,1.0);
        e = e*(dist)+1-dist;
        total += e;
    }
    total /= EDOPASS;
    return total;
}
#endif


// constant that are used to adjust lighting
const float C1 = 0.429043;
const float C2 = 0.511664;
const float C3 = 0.743125;
const float C4 = 0.886227;
const float C5 = 0.247708;

// scale for restored amount of lighting
const float u_scaleFactor = 1.0;

// coefficients of spherical harmonics and possible values
const vec3 u_L00 = vec3(0.79, 0.44, 0.54);
const vec3 u_L1m1 = vec3(0.39, 0.35, 0.60);
const vec3 u_L10 = vec3(-0.34, -0.18, -0.27);
const vec3 u_L11 = vec3(-0.29, -0.06, 0.01);
const vec3 u_L2m2 = vec3(-0.26, -0.22, -0.47);
const vec3 u_L2m1 = vec3(-0.11, -0.05, -0.12);
const vec3 u_L20 = vec3(-0.16, -0.09, -0.15);
const vec3 u_L21 = vec3(0.56, 0.21, 0.14);
const vec3 u_L22 = vec3(0.21, -0.05, -0.30);

///////////////////////////////////////////
void main2() {
    vec4 nl = texture(texNormals, pass_texcoord);
    prop.roughness = nl.w;
    prop.normal = normalize(nl.rgb * 2.0f - 1.0f);

    #if RENDER_PASS != 1
        discard;
    #endif
    out_Color = vec4((prop.normal.xyz)*0.02f, 1.0);
    if (prop.normal.y < 0) {
        out_Color = vec4(1,0,1,1);
    }
}

#define SHADE
void main() {
    vec4 sceneColor = texture(texColor, pass_texcoord);
	prop.albedo = sceneColor.rgb;
    // if (RENDER_PASS < 1) {
    //     out_Color = vec4(vec3(edo()*0.1), 1.0);
    //     return;
    // }
    float alpha = 1.0;
#ifdef SHADE
    prop.blockinfo = texture(texMaterial, pass_texcoord, 0);
    float renderpass = BLOCK_RENDERPASS(prop.blockinfo);
    if (RENDER_PASS > 0) {
        alpha = sceneColor.a;
        if(renderpass != RENDER_PASS)
            discard;
        if(sceneColor.a < 0.1)
            discard;
    }

    uint blockid = BLOCK_ID(prop.blockinfo);
    float isWater = IS_WATER(blockid);
    float stone = float(blockid==6u||blockid==4u);
    float isLight = IS_LIGHT(blockid);
    float isIllum = float(renderpass==4);
    float isBackface = float(renderpass==3);
    float isEntity = float(renderpass==5);
    float isCloud = float(renderpass==8);
    float fIsSky = isCloud;
    bool isSky = bool(fIsSky==1.0f);
#if RENDER_PASS < 1
    
    #if RENDER_AMBIENT_OCCLUSION
        vec4 ssao=texture(texAO, pass_texcoord);
    #else
        vec4 ssao=vec4(1);
    #endif
#else
    vec4 ssao = vec4(1);
#endif
    float depthUnderWater=0;
    vec4 viewSpacePosUnderWater=vec4(0);
    vec4 worldPosUnderWater=vec4(0);
    // if (RENDER_PASS == 1) {
    //     depthUnderWater = texture(texAO, pass_texcoord).r;
    //     viewSpacePosUnderWater = unprojectScreenCoord(pass_texcoord, depthUnderWater);
    //     worldPosUnderWater = in_matrix_3D.mv_inv * viewSpacePosUnderWater;
    // }


    float lum = dot(prop.albedo, vec3(0.3333f));


    vec4 nl = texture(texNormals, pass_texcoord);
    prop.roughness = nl.w;
	prop.normal = normalize(nl.rgb * 2.0f - 1.0f);
    prop.blockLight = texture(texBlockLight, pass_texcoord, 0);
    prop.reflective = prop.blockLight.w;
    prop.light = texture(texLight, pass_texcoord, 0);
	prop.depth = texture(texDepth, pass_texcoord).r;
    vec4 curScreenPos = screencoord(pass_texcoord.st, prop.depth);
    prop.position = unprojectScreenCoord(curScreenPos);
    prop.worldposition = in_matrix_3D.mv_inv * prop.position;
    prop.linearDepth = linearizeDepth(prop.depth);
    prop.viewVector = normalize(CAMERA_POS - prop.worldposition.xyz);
    prop.NdotL = max(dot( prop.normal, SkyLight.lightDir.xyz ), 0.0);
    
    prop.sunTheta = dot(-prop.viewVector, normalize(SkyLight.lightDir.xyz));
    float sunTheta = max( prop.sunTheta, 0.0 );
    float theta = max(dot(prop.viewVector, prop.normal), 0.0);
    prop.sunSpotDens = pow(sunTheta, 32.0)*1.0;

    float fogDepth = length(prop.position);
    // vec3 fogColor = mix(vec3(0.5,0.6,0.8)*1.2, vec3(0.5,0.6,1.4)*0.2, clamp(nightNoon, 0.0, 1.0));
    vec3 fogColor = mix(vec3(0.55,0.6,0.66)*1.2, vec3(0.5,0.6,1.4)*0.2, clamp(nightNoon, 0.0, 1.0));
    // float isFlower = float(blockid>=48u);
    // if (isBackface > 0) 
    //     discard;

#if RENDER_PASS ==1
    if (isWater > 0.9) {
        vec3 refractv = vec3(0.0);
        vec3 posxz = prop.worldposition.xyz+CAMERA_POS;
        posxz.x += sin(posxz.z+frametime)*0.4;
        posxz.z += cos(posxz.x+frametime*0.5)*0.4;
        float h0 = 0.0;
        float h1 = 0.0;
        float h2 = 0.0;
        float h3 = 0.0;
        float h4 = 0.0;
        float xDelta = 0.0;
        float yDelta = 0.0;
        float deltaPos = 0.4;
        h0 = waterH2(posxz,1,1);
        h1 = waterH2(posxz + vec3(deltaPos,0.0,0.0),1,1);
        h2 = waterH2(posxz + vec3(-deltaPos,0.0,0.0),1,1);
        h3 = waterH2(posxz + vec3(0.0,0.0,deltaPos),1,1);
        h4 = waterH2(posxz + vec3(0.0,0.0,-deltaPos),1,1);
        
        xDelta = ((h1-h0)+(h0-h2))/deltaPos;
        yDelta = ((h3-h0)+(h0-h4))/deltaPos;

        
        float refMult = (0.0005-dot(prop.normal,normalize(prop.viewVector).xyz)*0.0015)*3;
        
        refractv = normalize(vec3(xDelta,yDelta,1.0-xDelta*xDelta-yDelta*yDelta));
        // vec4 rA = texture2D(gcolor, newtc.st + refractv.xy*refMult);
        // rA.rgb = pow(rA.rgb,vec3(2.2));
        // vec4 rB = texture2D(gcolor, newtc.st);
        // rB.rgb = pow(rB.rgb,vec3(2.2));
        // float mask = texture2D(gaux1, newtc.st + refractv.xy*refMult).g;
        float mask =  isWater*(1-isEyeInWater);
        vec2 newtc = (pass_texcoord.st + refractv.xy*refMult)*mask + pass_texcoord.st*(1-mask);
            depthUnderWater = texture(texDepthPreWater, newtc).r;
            viewSpacePosUnderWater = unprojectScreenCoord(screencoord(newtc.st, depthUnderWater));
            worldPosUnderWater = in_matrix_3D.mv_inv * viewSpacePosUnderWater;
            // prop.albedo = sceneColor.rgb;
        // float uDepth = texture2D(depthtex1,newtc.xy).x;
        // color.rgb = pow(texture2D(gcolor,newtc.xy).rgb,vec3(2.2));
        // uPos  = nvec3(gbufferProjectionInverse * nvec4(vec3(newtc.xy,uDepth) * 2.0 - 1.0)); 
    }
#endif

        // sceneColor.rgb = vec3(prop.normal.xyz);
    alpha += isEntity;
    alpha = min(alpha, 1.0);
    if (!isSky) {
        // float minAmb = 0.25;
        // float minAmb2 = 0.1;
        float minAmb = 0.05;
        float minAmb2 = 0.09;
         float diff = 1.5;
        // prop.roughness = 0.3;
        float roughness = pow(2.0, 1.0+(prop.roughness)*24.0)-1.0;
        // out_Color = vec4(vec3(prop.roughness), 1);
        // return;
        // float glossy = 0.02;
        // if (isWater > 0) {
        //     glossy = 6.0;
        //     roughness = 0.2;
        // }


        // if (stone>0) {
        //     glossy = 1.4;
        //     roughness = 0.3;
        //     // out_Color = vec4(0,1,0,1);
        //     // return;
        // }
        const vec3 ambLight1 = normalize(vec3(50, 100, 50));
        const vec3 ambLight2 = normalize(vec3(-50, -70, -50));
        float NdotLAmb1 = max(dot( prop.normal, ambLight1 ), 0.0);
        float NdotLAmb2 = max(dot( prop.normal, ambLight2 ), 0.0);
        // vec3 reflectDirAmb1 = (reflect(-ambLight1, prop.normal));  
        // vec3 reflectDirAmb2 = (reflect(-ambLight2, prop.normal));  
        // float specAmb1 = pow(max(dot(prop.viewVector, reflectDirAmb1), 0.0), roughness)*44;
        // float specAmb2 = pow(max(dot(prop.viewVector, reflectDirAmb2), 0.0), roughness)*44;
        // vec3 halfDir1 = normalize(ambLight1.xyz + prop.viewVector.xyz);
        // vec3 halfDir2 = normalize(ambLight2.xyz + prop.viewVector.xyz);
        // float specAmb1 = pow(max(dot(halfDir1, prop.normal), 0.0), roughness);
        // float specAmb2 = pow(max(dot(halfDir2, prop.normal), 0.0), roughness);

        vec3 reflectDir = (reflect(-SkyLight.lightDir.xyz, prop.normal));  
        float spec = clamp(pow(max(dot(prop.viewVector, reflectDir), 0.0), roughness), 0.0, 1.0)*22.0;

        // sceneColor.xyz = vec3(prop.worldposition.rgb*0.002);



        float fNight = smoothstep(0.0, 1.0, clamp(nightNoon-isLight, 0.0, 1.0));
        float skyLightLvl = smoothstep(0.0, 1.0, prop.blockLight.x);
        float blockLightLvl = prop.blockLight.y;
        // float occlusion = min(prop.blockLight.z, ssao.r);
        float occlusion = min(prop.blockLight.z, ssao.r);
        occlusion+=float(RENDER_PASS==1);
        occlusion = min(1.0, occlusion);/**3.5*/

        float shadowRaw = getShadow2();
        float shadow = shadowRaw*(1.0-isBackface)*(1.0-isWater*0.8);
        // float shadow = mix(getSoftShadow(), 1, 0.04);

        float sunLight = skyLightLvl * prop.NdotL * (shadow+prop.blockLight.z*0.05) * dayLightIntens *(1.0-fNight);
        sunLight = max(shadow*(0.05-fNight*0.035), sunLight);
        sunLight = sunLight*occlusion;
        sunLight = max(0.0, sunLight);

        float blockLight = (1.0-pow(1.0-blockLightLvl,0.05))*1.1;
        vec3 lightColor = mix(vec3(1.0), vec3(0.8, 0.9, 1.1), fNight);
        vec3 Ispec = SkyLight.Ls.rgb * vec3(1.0) *spec;
        vec3 Idiff = SkyLight.Ld.rgb * vec3(1.0) *diff;
        vec3 Iamb = SkyLight.La.rgb * lightColor *  mix(((NdotLAmb1+NdotLAmb2)*(0.45)), 1.2, isEntity*0.0);
         // Iamb += SkyLight.La.rgb * lightColor * NdotLAmb1 *specAmb1 * 0.25;
         // Iamb += SkyLight.La.rgb * lightColor * NdotLAmb2 *specAmb2 * 0.08;
         // float aa = 2.8f;
         // Iamb = vec3(aa);
         // Idiff = vec3(aa);
         // Ispec = vec3(aa);

        vec3 finalLight = vec3(0.0);
        finalLight += Iamb * (minAmb+occlusion*(1.0-minAmb)) * (minAmb2+(skyLightLvl)*(1-minAmb2));

        finalLight += Ispec * sunLight;
        finalLight += Idiff * sunLight;

        const float blockLightConst = 15.0;
        finalLight += lum* (mix(1.0, occlusion, 0.19)) * blockLight*isLight*0.6;
        finalLight+=isIllum*4.0;
        finalLight += vec3(1.0, 0.9, 0.7) * pow(blockLightLvl/8.0,2.0)*((1.0-isLight*0.8)*blockLightConst);
        float mixSSAO = 0.1;
        finalLight *= max(mixSSAO+ssao.r*(1.0-mixSSAO), isWater+isBackface*0.5);
        finalLight+=prop.light.rgb*(occlusion);
        // finalLight*=2;
#if RENDER_PASS ==1
        // float waterDepth = length(prop.position-viewSpacePosUnderWater)*0.05;

        // vec4 vNormal = in_matrix_3D.view* vec4(prop.normal.xyz, 1.0);
        // vec3 uVec = (prop.position-viewSpacePosUnderWater).xyz;
        // float UNdotUP = 0.5+abs(dot(normalize(uVec),normalize(vNormal.xyz)));

    
    
        vec3 uVec = (prop.worldposition - worldPosUnderWater).xyz;
        float len =length(uVec);
        if (len > 0.001) {
            // prop.albedo.r=1;
            float UNdotUP = 0.5+abs(dot(normalize(uVec),normalize(prop.normal.xyz)));
            float depth = len*UNdotUP;
            float sky_absorbance = mix(mix(1.0,exp(-depth/16.5),isWater),1.0,isEyeInWater);
            // alpha = prop.albedo.a;//clamp(clamp(depth, 0.4, (sceneColor.a*1.4)*(1-clamp(sunLight, 0.0, 1.0))), 0.5, 1.0);
            // alpha = 0.5;
            float minShadow = max(shadowRaw, 0.7);
            // alpha = 0.999+0.001*clamp((depth-4.5) / 4.5, 0.0, 1.0);
            finalLight *= clamp(sky_absorbance, 0.2, 1.0)*minShadow*0.08;
            fogDepth = min(depth, 12)*15;
            fogColor *= vec3(0.05, 0.18, 0.12)*0;
            prop.albedo+=pow(1.0-min(depth/12.0, 1.0), 4.0)*(vec3(0.02, 0.035, 0.035)*2)*minShadow*1.0;
            // finalLight*=4;
// vec3 watercolor = vec3(0.71,0.6,0.6);
// vec3 ambient_color = vec3(1)*lightColor;
            // prop.albedo=mix(watercolor*pow(length(ambient_color),0.1)*1,prop.albedo,exp(-depth/32));
        }
#endif

        alpha = clamp(alpha, 0.0, 1.0);


        vec3 terr=prop.albedo*finalLight;
        spec*=shadow;
        prop.albedo = mix (terr, spec*vec3(0.02), isWater*theta);
        prop.albedo=terr;
    } else {
        // prop.albedo.rgb = vec3(1,0,1);
    }

#if RENDER_PASS < 2
    // fogDepth = min(fogDepth, in_scene.viewport.w/4.0);
    // fogDepth = max(fogDepth-100.0, 0.0);
    // float hM = clamp(prop.worldposition.y/100.0, 0.05, 0.9)+clamp((prop.worldposition.y-180)/80.0, 0.0, 1.0)*3;
    // float fogAmount = clamp(1.0 - exp( -fogDepth*0.00001*hM ), 0.0, 1.0);
    // prop.albedo =  mix( prop.albedo, fogColor, fogAmount*(1.0-fIsSky*0.97) );

    fogDepth = min(fogDepth, 256.0);
    fogDepth = max((fogDepth-16.0)*0.1f, 0.0);
    float fogeye = clamp(1.0 - clamp(dot(-prop.viewVector, vec3(0,1,0)), 0.0, 1.0) / 0.8, 0, 1)*0.4;
    fogeye += fogeye*clamp(1.0 - clamp((
        dot(-prop.viewVector, vec3(0,-1,0))
        *dot(-prop.viewVector, vec3(0,1,0)))*3.2, 0.0, 1.0) -0.8, 0, 1)*1.4;
    float hM = clamp(prop.worldposition.y/100.0, 0.05, 0.9)+clamp((prop.worldposition.y-180)/80.0, 0.0, 1.0)*3;
    float fogAmount = clamp(1.0 - exp( -(fogDepth+fIsSky)*0.00005*(fIsSky*2.9*fogeye+hM*(1-fIsSky)) ), 0.0, 1.0);
    prop.albedo =  mix( prop.albedo, fogColor, fogAmount*(1.0) );
#endif
    // prop.albedo = vec3(fIsSky);

#if 0
#if RENDER_PASS < 2
    // vec3 fogColor = mix(vec3(0.5,0.6,0.7), vec3(0.5,0.6,1.4)*0.2, clamp(nightNoon, 0, 1));
     fogAmount = VolumetricLight();
    prop.albedo =  mix( prop.albedo, fogColor, fogAmount*VOL_STRENGTH );
#endif
#endif


#endif
#if RENDER_PASS == 2
#endif
#if RENDER_MATERIAL_BUFFER
    float texSlot = BLOCK_TEX_SLOT(prop.blockinfo);
    out_FinalMaterial = vec4(texSlot/200.0f, 0.0, 0.0, 1.0);
#endif
#if RENDER_VELOCITY_BUFFER

    vec4 prevScreenPos = mat_reproject * curScreenPos;
    vec2 scale = vec2(0.5, 0.5);
    curScreenPos.xy *= scale;
    prevScreenPos.xy *= scale;
    prevScreenPos /= prevScreenPos.w;

    vec2 velocity = (curScreenPos.xy - prevScreenPos.xy);
    float velocityIntens = 1.0-fIsSky;
    out_Velocity = vec4(velocity*velocityIntens, 0, 0);
#endif
    // if (isSky)
    //     prop.position.z=0;
    out_Color = vec4(prop.albedo.rgb, alpha);
    // #if RENDER_PASS == 1
    // out_Color = vec4(vec3(texture(texDepthPreWater, pass_texcoord).r), alpha);
    // #endif
    // out_Color = vec4(sceneColor.rgb*0.02, alpha);
}
