#version 450
#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable

#pragma include "ubo_scene.glsl"
#pragma include "ubo_shadow.glsl"
#pragma define "MAP_Z_INVERSE"
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

#define SHADOW_FACTOR0 1.00001
#define SHADOW_FACTOR1 1.00005
#define SHADOW_FACTOR2 1.00007

const float clampmin = 1.0/SHADOW_MAP_RESOLUTION;
const float clampmax = 1.0-clampmin;
bool canLookup(in vec4 v) {
    if (clamp(v.x, clampmin, clampmax) == v.x && clamp(v.y, clampmin, clampmax) == v.y) {
        if (v.z >= 0 && v.z <= 1) {
            return true;
        }
    }
    return false;
}

vec4 getShadowTexcoord(in mat4 shadowMVP, in vec4 worldpos) {
    vec4 v2 = shadowMVP * vec4(worldpos/worldpos.w);
    v2.xy = v2.xy * 0.5 + 0.5;
    return v2;
}

float lookupShadowMap(vec3 v, float factor) {

#if MAP_Z_INVERSE
    v.z*=2.0-factor;
    return step(v.z, 1.0-texture(texShadow, v.xy).r);
#else
    v.z*=2.0-factor;
    return step(v.z, texture(texShadow, v.xy).r);
#endif
}
int steps = 0;
vec3 dbgSplit = vec3(0);

float getShadow() {
    vec4 shadowSS = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[2], pass_position);
    float s = 0;
    if (steps<1&&canLookup(shadowSS)) {
        s += lookupShadowMap(vec3(shadowSS.xy, shadowSS.z), SHADOW_FACTOR2);
        steps++;
        dbgSplit.b = 1.0;
    }
    if (steps > 0)
        s/=steps;
    return clamp(s, 0, 1);
}

void main() 
{
    vec4 color = texture(samplerColor, vec3(pass_texcoord, 292.0));
    vec3 N = normalize(pass_normal);
    vec3 L = normalize(pass_LightVec);
    vec3 V = normalize(pass_ViewVec);
    vec3 R = reflect(-L, N);
    vec3 diffuse = max(dot(N, L), 0.0) * vec3(1.0);
    float specular = pow(max(dot(R, V), 0.0), 16.0) * color.a;
    float shadow = mix(getShadow(), 1.0, 0.1)*4.0;
    vec3 final = diffuse * color.rgb + specular;

    vec3 inPosN = pass_position.xyz / pass_position.w;
    vec4 v3 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[2], pass_position);
    outFragColor = vec4(final*shadow+dbgSplit.rgb*0.0, 1.0);   
    if (gl_FragCoord.x < 300)  {
        outFragColor = vec4(vec3((gl_FragCoord.z/Z_NEAR)*10), 1.0);
    }
    else if (gl_FragCoord.x < 600) {
        #if MAP_Z_INVERSE
            outFragColor = vec4(vec3(1.0-texture(texShadow, v3.xy).r), 1.0);
        #else
            outFragColor = vec4(vec3(texture(texShadow, v3.xy).r), 1.0);
        #endif
    }
    // vec4 prevp = vec4(0, 480, 0, 320);
    // if (gl_FragCoord.x > prevp.x && gl_FragCoord.x < prevp.y && gl_FragCoord.y > prevp.z && gl_FragCoord.y < prevp.w) {
    //     vec2 txc = (gl_FragCoord.xy-prevp.xz) / vec2(prevp.y-prevp.x, prevp.w-prevp.z);
    //     vec3 shadowmaps=vec3(texture(texShadow, txc).r);
    //     shadowmaps.xy += txc.xy*0.3;
    //     outFragColor = vec4(shadowmaps, 1.0);
    // }
}