#version 150 core
#extension GL_ARB_shader_storage_buffer_object : enable


#pragma include "ubo_scene.glsl"
#pragma include "blockinfo.glsl"
#pragma include "sky_scatter.glsl"
#pragma define "RENDER_PASS"

layout(std140) uniform LightInfo {
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
} prop;

layout (std430) buffer DebugOutputBuffer
{
    float debugVals[16];
    int tileLights[];
} debugBuf;


uniform sampler2D texColor;
uniform sampler2D texNormals;
uniform usampler2D texMaterial;
uniform sampler2D texDepth;
uniform sampler2D texShadow;
uniform sampler2D texBlockLight;
uniform sampler2D texLight;
uniform sampler2D texAO;


in vec2 pass_texcoord;

in float dayNoon;
in float nightNoon;
in float dayLightIntens;
in float lightAngleUp;
in float moonSunFlip;

out vec4 out_Color;


/*

float getSoftShadow() {

    float gdistance = max((length(prop.position.xyz)-26.0f)/17.0f, 0);
    vec4 v = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[0], worldPos);
    vec4 v2 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[1], worldPos);
    vec4 v3 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[2], worldPos);

    vec2 cPos = pass_texcoord*2.0-1.0;
    float dst = sqrt(cPos.x*cPos.x+cPos.y*cPos.y);
    float weight = max(0.68, 1.3-dst);

    if (clamp(v.x, clampmin, clampmax) == v.x && clamp(v.z, clampmin, clampmax) == v.z && prop.linearDepth<in_matrix_shadow.shadow_split_depth.x*weight) {
        v.z-=0.00004f;
        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE; x <= SOFT_SHADOW_TAP_RANGE; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE; y <= SOFT_SHADOW_TAP_RANGE; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE;
                s += texture(texShadow, vec3(v.xy*0.5+offs, v.z));        
            }
        }
        s /= SOFT_SHADOW_WEIGHT;
        return s;
    }
    if (clamp(v2.x, clampmin, clampmax) == v2.x && clamp(v2.z, clampmin, clampmax) == v2.z && prop.linearDepth<in_matrix_shadow.shadow_split_depth.y*weight) {
    // v.z *= (1.0f-0.001f * gdistance);
        v2.z-=0.00007f;

        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE; x <= SOFT_SHADOW_TAP_RANGE; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE; y <= SOFT_SHADOW_TAP_RANGE; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE;
                s += texture(texShadow, vec3(v2.xy*0.5+vec2(0.5,0)+offs, v2.z));        
            }
        }
        s /= SOFT_SHADOW_WEIGHT;
        return s;
    }
    if (clamp(v3.x, clampmin, clampmax) == v3.x && clamp(v3.z, clampmin, clampmax) == v3.z && prop.linearDepth<in_matrix_shadow.shadow_split_depth.z) {
    // v.z *= (1.0f-0.001f * gdistance);
        // v3.z-=bias;
        v2.z-=0.00007f;

        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE; x <= SOFT_SHADOW_TAP_RANGE; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE; y <= SOFT_SHADOW_TAP_RANGE; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE;
                s += texture(texShadow, vec3(v3.xy*0.5+vec2(0,0.5)+offs,v3.z));   
            }
        }
        s /= SOFT_SHADOW_WEIGHT;
        return s;

        // return texture(texShadow, vec3(v3.xy*0.5+vec2(0,0.5),v3.z));   
    }
    return 1;
}
*/
float expToLinearDepth(in float depth)
{
    return 2.0f * in_scene.viewport.z * in_scene.viewport.w / (in_scene.viewport.w + in_scene.viewport.z - (2.0f * depth - 1.0f) * (in_scene.viewport.w - in_scene.viewport.z));
}

vec4 unprojectPos(in vec2 coord, in float depth) { 
    vec4 fragposition = in_matrix_3D.proj_inv * vec4(coord.s * 2.0f - 1.0f, coord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
    fragposition /= fragposition.w;
    return fragposition;
}

vec4 getShadowTexcoord(in mat4 shadowMVP, in vec4 worldpos) {
    vec4 v2 = shadowMVP * worldpos;
    v2 = v2 * 0.5 + 0.5;
    return v2;
}

const float clampmin = 0;//1.0/8.0;
const float clampmax = 1-clampmin;
bool canLookup(in vec4 v, in float zPos, in float mapZ) {
    return clamp(v.x, clampmin, clampmax) == v.x && clamp(v.z, clampmin, clampmax) == v.z && zPos < mapZ;
}
#define SAMPLE_DISTANCE ((1.0/2048.0) / 4.0)
#define SOFT_SHADOW_TAP_RANGE 1
#define SOFT_SHADOW_TAP_RANGE2 1
#define SAMPLE_DISTANCE2 ((1.0/2048.0) / 4.0)
#define SOFT_SHADOW_WEIGHT ((SOFT_SHADOW_TAP_RANGE*2+1)*(SOFT_SHADOW_TAP_RANGE*2+1))
#define SOFT_SHADOW_WEIGHT2 ((SOFT_SHADOW_TAP_RANGE2*2+1)*(SOFT_SHADOW_TAP_RANGE2*2+1))
#if 1
#endif

float getShadowAt(vec4 worldPos, float linDepth, float zOffset) {
    vec4 v = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[0], worldPos);
    vec4 v2 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[1], worldPos);
    vec4 v3 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[2], worldPos);
    float weight = 0.9;
    vec4 mapZSplits = in_matrix_shadow.shadow_split_depth;
    if (canLookup(v, linDepth, mapZSplits.x*0.9)) {
        v.z*=0.9997;
#if SOFT_SHADOW_TAP_RANGE2 < 1
        return v.z > texture(texShadow, v.xy*0.5).r ? 0 : 1;
#else
        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE2; x <= SOFT_SHADOW_TAP_RANGE2; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE2; y <= SOFT_SHADOW_TAP_RANGE2; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE2;
                s += texture(texShadow, v.xy*0.5+offs).r;   
            }
        }
        s /= SOFT_SHADOW_WEIGHT2;
        return v.z > s ? 0 : 1;
#endif
    }
    if (canLookup(v2, linDepth, mapZSplits.y*weight)) {
        v2.z*=0.9999;
#if SOFT_SHADOW_TAP_RANGE2 < 1
        return v2.z > texture(texShadow, v2.xy*0.5+vec2(0.5,0)).r ? 0 : 1;
#else
        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE2; x <= SOFT_SHADOW_TAP_RANGE2; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE2; y <= SOFT_SHADOW_TAP_RANGE2; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE2;
                s += texture(texShadow, v2.xy*0.5+vec2(0.5,0)+offs).r;   
            }
        }
        s /= SOFT_SHADOW_WEIGHT2;
        return v2.z > s ? 0 : 1;
#endif
    }
    if (canLookup(v3, linDepth, mapZSplits.z)) {
        v3.z*=0.9997;
#if SOFT_SHADOW_TAP_RANGE2 < 1
        return v3.z > texture(texShadow, v3.xy*0.5+vec2(0,0.5)).r ? 0 : 1;
#else
        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE2; x <= SOFT_SHADOW_TAP_RANGE2; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE2; y <= SOFT_SHADOW_TAP_RANGE2; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE2;
                s += texture(texShadow, v3.xy*0.5+vec2(0,0.5)+offs).r;   
            }
        }
        s /= SOFT_SHADOW_WEIGHT2;
        return v3.z > s ? 0 : 1;
#endif
    }
    return 1;
}

float getShadow2() {

    vec4 v = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[0], prop.worldposition);
    vec4 v2 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[1], prop.worldposition);
    vec4 v3 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[2], prop.worldposition);
    vec2 cPos = pass_texcoord*2.0-1.0;
    float dst = sqrt(cPos.x*cPos.x+cPos.y*cPos.y);
    float weight = max(0.68, 1.3-dst);
    vec4 mapZSplits = in_matrix_shadow.shadow_split_depth;
    vec4 shadow = vec4(1);
    shadow.xyz = vec3(0.5);
    if (canLookup(v, prop.linearDepth, mapZSplits.x*weight)) {
        v.z*=0.9997;
        // shadow.x = texture(texShadow, v.xy*0.5).r;
        // shadow.w += 1;
        // return v.z > shadow.x ? 0 : 1;
        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE; x <= SOFT_SHADOW_TAP_RANGE; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE; y <= SOFT_SHADOW_TAP_RANGE; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE;
                s += texture(texShadow, v.xy*0.5+offs).r;   
            }
        }
        s /= SOFT_SHADOW_WEIGHT;
        // shadow.x = v.z > s ? 0 : 1;
        // shadow.w += 1;
        return v.z > s ? 0 : 1;
    }
    if (canLookup(v2, prop.linearDepth, mapZSplits.y*weight)) {
        v2.z*=0.9999;
        // shadow.y = texture(texShadow, v2.xy*0.5+vec2(0.5,0)).r;
        // shadow.w += 1;
        // return v2.z > shadow.y ? 0 : 1;
        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE; x <= SOFT_SHADOW_TAP_RANGE; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE; y <= SOFT_SHADOW_TAP_RANGE; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE;
                s += texture(texShadow, v2.xy*0.5+vec2(0.5,0)+offs).r;   
            }
        }
        s /= SOFT_SHADOW_WEIGHT;
        // shadow.y += v2.z > s ? 0 : 1;
        // shadow.w += 1;
        return v2.z > s ? 0 : 1;
    }
    if (canLookup(v3, prop.linearDepth, mapZSplits.z)) {
        v3.z*=0.9997;
        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE; x <= SOFT_SHADOW_TAP_RANGE; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE; y <= SOFT_SHADOW_TAP_RANGE; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE;
                s += texture(texShadow, v3.xy*0.5+vec2(0,0.5)+offs).r;   
            }
        }
        s /= SOFT_SHADOW_WEIGHT;
        // shadow.z = v3.z > s ? 0 : 1;
        // shadow.w += 1;
        // shadow.z = texture(texShadow, v3.xy*0.5+vec2(0,0.5)).r;
        // shadow.w += 1;
        return v3.z > s ? 0 : 1;
    }
    // return (shadow.x+shadow.y+shadow.z) / shadow.w;
    return 1;
}
// Mie scaterring approximated with Henyey-Greenstein phase function.
#define G_SCATTERING 0.8f
#define VOL_STRENGTH 0.1f
#define NB_STEPS 8
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
    vec4 ditherPattern[4];
    ditherPattern[0] = vec4(0.0f, 0.5f, 0.125f, 0.625f);
    ditherPattern[1] = vec4( 0.75f, 0.22f, 0.875f, 0.375f);
    ditherPattern[2] = vec4( 0.1875f, 0.6875f, 0.0625f, 0.5625);
    ditherPattern[3] = vec4( 0.9375f, 0.4375f, 0.8125f, 0.3125);

    ivec2 pixelPos = ivec2(pass_texcoord.xy*in_scene.viewport.xy);
    float dither = ditherPattern[pixelPos.x%4][pixelPos.y%4];
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
#define SHADE
void main() {

    vec4 sceneColor = texture(texColor, pass_texcoord);
	prop.albedo = sceneColor.rgb;

    float alpha = 1.0;
#ifdef SHADE
    vec4 ssao = vec4(1);
    if (RENDER_PASS < 1) {
        ssao=texture(texAO, pass_texcoord);
    }
    float depthUnderWater=0;
    vec4 viewSpacePosUnderWater=vec4(0);
    vec4 worldPosUnderWater=vec4(0);
    if (RENDER_PASS == 1) {
        depthUnderWater = texture(texAO, pass_texcoord).r;
        viewSpacePosUnderWater = unprojectPos(pass_texcoord, depthUnderWater);
        worldPosUnderWater = in_matrix_3D.mv_inv * viewSpacePosUnderWater;
    }


    float lum = dot(prop.albedo, vec3(0.3333f));


    vec4 nl = texture(texNormals, pass_texcoord);
	prop.normal = nl.rgb * 2.0f - 1.0f;
    prop.blockLight = texture(texBlockLight, pass_texcoord, 0);
    prop.light = texture(texLight, pass_texcoord, 0);
	prop.depth = texture(texDepth, pass_texcoord).r;
    prop.blockinfo = texture(texMaterial, pass_texcoord, 0);
    prop.linearDepth = expToLinearDepth(prop.depth);
    prop.position = unprojectPos(pass_texcoord, prop.depth);
    prop.worldposition = in_matrix_3D.mv_inv * prop.position;
    prop.viewVector = normalize(CAMERA_POS - prop.worldposition.xyz);
    prop.NdotL = dot( prop.normal, SkyLight.lightDir.xyz );
    
    prop.sunTheta = dot(-prop.viewVector, normalize(SkyLight.lightDir.xyz));
    float sunTheta = max( prop.sunTheta, 0.0 );
    prop.sunSpotDens = pow(sunTheta, 32.0)*1;
    uint blockid = BLOCK_ID(prop.blockinfo);
    float renderpass = BLOCK_RENDERPASS(prop.blockinfo);

    float isSky = IS_SKY(blockid);
    float isWater = IS_WATER(blockid);
    float isLight = IS_LIGHT(blockid);
    float isIllum = float(renderpass==4);
    float isBackface = float(renderpass==3);
    float isEntity = float(renderpass==5||renderpass==2);
    // float isFlower = float(blockid>=48u);
    if (RENDER_PASS > 0) {
        alpha = sceneColor.a;
        if(renderpass != RENDER_PASS)
            discard;
        if(sceneColor.a < 0.1)
            discard;
    }
    float roughness = 1.3+isWater*40;

    const vec3 ambLight1 = normalize(vec3(50, 100, 50));
    const vec3 ambLight2 = normalize(vec3(-50, -70, -50));
    float NdotLAmb1 = max(dot( prop.normal, ambLight1 ), 0);
    float NdotLAmb2 = max(dot( prop.normal, ambLight2 ), 0);
    vec3 reflectDirAmb1 = (reflect(-ambLight1, prop.normal));  
    vec3 reflectDirAmb2 = (reflect(-ambLight2, prop.normal));  
    float specAmb1 = pow(max(dot(prop.viewVector, reflectDirAmb1), 0.0), 2);
    float specAmb2 = pow(max(dot(prop.viewVector, reflectDirAmb2), 0.0), 2);

    vec3 reflectDir = (reflect(-SkyLight.lightDir.xyz, prop.normal));  
    float spec = pow(max(dot(prop.viewVector, reflectDir), 0.0), roughness);
    float theta = max(dot(prop.viewVector, prop.normal), 0.0);
    float minRefl = 0.02;
    float amtRefl = minRefl + (1.0 - minRefl) * pow(1.0 - theta, 5.0);

    vec3 skySunScat = skyAtmoScat(-prop.viewVector, SkyLight.lightDir.xyz, moonSunFlip);

    float fNight = smoothstep(0, 1, clamp(nightNoon-isLight, 0, 1));
    float skyLightLvl = prop.blockLight.x;
    float blockLightLvl = prop.blockLight.y;
    float occlusion = min(prop.blockLight.z, ssao.r);
    occlusion+=float(RENDER_PASS==1);
    occlusion = min(1, occlusion);

    float shadow = getShadow2()*(1-isBackface);
    // float shadow = mix(getSoftShadow(), 1, 0.04);
  	float nDotL = clamp(max(0.0f, prop.NdotL * 0.99f + 0.01f), 0, 1);

    float sunLight = skyLightLvl * nDotL * shadow * dayLightIntens *(1-fNight);
    sunLight = max(shadow*(0.05-fNight*0.035), sunLight);
    sunLight = sunLight*occlusion;
    sunLight = max(0, sunLight);


    float blockLight = (1-pow(1-blockLightLvl,0.05))*1.1;
    vec3 lightColor = mix(vec3(1), vec3(0.8, 0.9, 1.1), fNight);
	vec3 Ispec = SkyLight.Ls.rgb * lightColor * nDotL * spec;
    vec3 Idiff = SkyLight.Ld.rgb * lightColor * nDotL;
    vec3 Iamb = SkyLight.La.rgb * lightColor *  mix(((NdotLAmb1+NdotLAmb2)*0.5f), 1.2, isEntity*0.8);

    // Idiff*=darkenSkyLitBlocksNight;
    // Ispec*=darkenSkyLitBlocksNight;
    // Iamb = vec3(0);
    vec3 finalLight = vec3(0);
    float minAmb = 0.2;
    vec3 blockAmbientLevel = Iamb * (minAmb+occlusion*(1-minAmb)) * (0.04+skyLightLvl*(1-0.04));
    finalLight += blockAmbientLevel;

    finalLight += lum* (mix(1, occlusion, 0.19)) * blockLight*isLight*0.6;
    float fl=blockLightLvl/15.0f;
    finalLight += Ispec * sunLight;
    finalLight += Idiff * sunLight;
    const float blockLightConst = 60;
    finalLight+=isIllum*4;
    finalLight += vec3(1, 0.9, 0.7) * pow(blockLightLvl/8.0,2)*((1.0-isLight*0.8)*blockLightConst);
    finalLight *= max(0.3+ssao.r*0.7, isWater);
    finalLight+=prop.light.rgb*(occlusion);
#if RENDER_PASS ==1
    float waterDepth = length(prop.position-viewSpacePosUnderWater);
    alpha = clamp(clamp(waterDepth/6, 0.25, (sceneColor.a*1.4)*(1-clamp(sunLight, 0, 1))), 0.25, 1);
#endif
    alpha = clamp(alpha, 0, 1);

    vec3 sky=mix(prop.albedo, vec3(0.0001), fNight)*0.23;
    prop.sunSpotDens*=(1-fNight*0.92);
    float scatbr = clamp((skySunScat.r+skySunScat.b+skySunScat.g) / 2.0f, 0, 1);
    sky = mix(sky, sky*skySunScat, 0.3f-fNight*0.2f);
    sky += skySunScat*prop.sunSpotDens*1.2;
    sky += sky*SkyLight.La.rgb*(1.0-prop.sunSpotDens)*1.1f;
    sky *= 0.4;
    vec3 terr=prop.albedo*finalLight;
    spec*=shadow;//0.6+(shadow*0.1+sunLight*0.3);
    vec3 waterAlb = mix(prop.albedo*finalLight, spec*vec3(0.1), isWater*theta);
    terr = mix (terr, waterAlb, isWater);

    prop.albedo = mix(terr, sky, isSky);
#if RENDER_PASS < 2
    vec3 fogColor = mix(vec3(0.5,0.6,0.7), vec3(0.5,0.6,1.4)*0.2, clamp(nightNoon, 0, 1));
    float fogDepth = length(prop.position);
    fogDepth = min(fogDepth, in_scene.viewport.w/6);
    fogDepth = max(fogDepth-30, 0);
    float fogAmount = clamp(1.0 - exp( -fogDepth*0.00001 ), 0, 1);
    prop.albedo =  mix( prop.albedo, fogColor, fogAmount );
#endif

// #if RENDER_PASS < 2
//     // vec3 fogColor = mix(vec3(0.5,0.6,0.7), vec3(0.5,0.6,1.4)*0.2, clamp(nightNoon, 0, 1));
//      fogAmount = VolumetricLight();
//     prop.albedo =  mix( prop.albedo, fogColor, fogAmount*VOL_STRENGTH );
// #endif
#endif

    out_Color = vec4(prop.albedo, alpha);
}
