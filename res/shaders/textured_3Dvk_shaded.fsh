#version 450
#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable

#pragma include "ubo_scene.glsl"
#pragma include "ubo_shadow.glsl"
#pragma include "unproject.glsl"
#pragma define "RENDER_PASS"
#pragma define "RENDER_MATERIAL_BUFFER" "0"
#pragma define "RENDER_VELOCITY_BUFFER" "0"
#pragma define "RENDER_AMBIENT_OCCLUSION"
#pragma define "SHADOW_MAP_RESOLUTION" "2048.0"

uniform sampler2DArray samplerColor;
uniform sampler2D texShadow;

in vec2 pass_texcoord;
in float pass_LodBias;
in vec3 pass_normal;
in vec3 pass_ViewVec;
in vec3 pass_LightVec;
in vec4 pass_position;

out vec4 outFragColor;

#define SAMPLE_DISTANCE ((1.0/SHADOW_MAP_RESOLUTION) / 4.0)
#define SOFT_SHADOW_TAP_RANGE 0
#define SOFT_SHADOW_TAP_RANGE2 1
#define SAMPLE_DISTANCE2 ((1.0/SHADOW_MAP_RESOLUTION) / 4.0)
#define SOFT_SHADOW_WEIGHT ((SOFT_SHADOW_TAP_RANGE*2+1)*(SOFT_SHADOW_TAP_RANGE*2+1))
#define SOFT_SHADOW_WEIGHT2 ((SOFT_SHADOW_TAP_RANGE2*2+1)*(SOFT_SHADOW_TAP_RANGE2*2+1))
#if Z_INVERSE
#define SHADOW_FACTOR0 1.00001
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

const float clampmin = 1.0/SHADOW_MAP_RESOLUTION;//1.0/8.0;
const float clampmax = 1.0-clampmin;
bool canLookup(in vec4 v) {
    if (clamp(v.x, clampmin, clampmax) == v.x && clamp(v.y, clampmin, clampmax) == v.y) {
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

vec4 getShadowTexcoord(in mat4 shadowMVP, in vec3 worldpos) {
    vec4 v2 = shadowMVP * vec4(worldpos, 1.0);
#if Z_INVERSE
    v2.xy = v2.xy * 0.5 + 0.5;
#else
    v2 = v2 * 0.5 + 0.5;
#endif
    return v2;
}
const float shadowNEAR = 512*8;
const float shadowFAR = 5;
float lin01_shadow(float depth) {
    float clipSpaceZ= (depth-shadowNEAR) / (shadowFAR-shadowNEAR);
    return clipSpaceZ;
}
float linearizeShadowDepth(float depth) {
    return (shadowNEAR * shadowFAR) / (depth * (shadowFAR - shadowNEAR));
}
float lookupShadowMap(vec3 v, float factor) {
    v.z*=factor;
    #if SOFT_SHADOW_TAP_RANGE == 0
        return step(texture(texShadow, v.xy).r, v.z);
    #else
        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE; x <= SOFT_SHADOW_TAP_RANGE; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE; y <= SOFT_SHADOW_TAP_RANGE; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE;
                if (SHADOW_COMPARE(v.z, texture(texShadow, v.xy+offs).r)) {
                    s += 1;
                }
            }
        }
        s /= SOFT_SHADOW_WEIGHT;
        return s;
    #endif
}
int steps = 0;
int maxCascade = -1;
vec3 dbgSplit = vec3(0);

float getShadow2() {
    vec3 inPosN = pass_position.xyz / pass_position.w;
    vec4 v = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[0], inPosN);
    vec4 v2 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[1], inPosN);
    vec4 v3 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[2], inPosN);
    vec4 mapZSplits = in_matrix_shadow.shadow_split_depth;
    float s = 0.0;
    if (canLookup(v)) {
        s += lookupShadowMap(vec3(v.xy*0.5, v.z), SHADOW_FACTOR0);
       if (clamp(v.z, 0.15, 0.85) == v.z)
            steps++;
        maxCascade = max(maxCascade, 0);
        dbgSplit.r = 2.0;
    }
    if (steps<1&&canLookup(v2)) {
        s += lookupShadowMap(vec3(v2.xy*0.5+vec2(0.5,0.0), v2.z), SHADOW_FACTOR1);
        if (clamp(v2.z, 0.15, 0.85) == v2.z)
            steps++;
        maxCascade = max(maxCascade, 1);
        dbgSplit.g = 1.0;
    }
    if (steps<1&&canLookup(v3)) {
        s += lookupShadowMap(vec3(v3.xy*0.5+vec2(0.0, 0.5), v3.z), SHADOW_FACTOR2);
        steps++;
        maxCascade = max(maxCascade, 2);
        dbgSplit.b = 1.0;
    }
    if (steps > 0)
        s/=steps;
    // return 1.0;
        return clamp(s, 0, 1);
}

void main() 
{
    vec4 color = texture(samplerColor, vec3(pass_texcoord, 5.0));
    // if (color.r > 0) color = vec4(1.0);
    vec3 N = normalize(pass_normal);
    vec3 L = normalize(pass_LightVec);
    vec3 V = normalize(pass_ViewVec);
    vec3 R = reflect(-L, N);
    vec3 diffuse = max(dot(N, L), 0.0) * vec3(1.0);
    float specular = pow(max(dot(R, V), 0.0), 16.0) * color.a;
    float shadow = mix(getShadow2(), 1.0, 0.1)*4.0;
    vec3 final = diffuse * color.rgb + specular;
    // final.r = shadow.r;
    if (maxCascade > -1) {
        // final[maxCascade] = 1.0;
    }
    #if Z_INVERSE
        // final = vec3(0);
    #endif

    outFragColor = vec4(final*shadow+dbgSplit.rgb*0.0, 1.0);   
}