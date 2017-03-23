#version 150 core
 
#pragma include "ubo_scene.glsl"
#pragma include "dither.glsl"
#pragma include "unproject.glsl"

#pragma include "blockinfo.glsl"
#pragma include "sky_scatter.glsl"
#pragma define "RENDER_TO_SCENE_FB"
#ifndef RENDER_TO_SCENE_FB
#define RENDER_TO_SCENE_FB 0
#endif

layout(set = 3, binding = 0, std140) uniform LightInfo
{
  vec4 dayLightTime; 
  vec4 posSun; // Light position in world space
  vec4 lightDir; // Light dir in world space
  vec4 La; // Ambient light intensity
  vec4 Ld; // Diffuse light intensity
  vec4 Ls; // Specular light intensity
} SkyLight;

uniform sampler2D tex0;

in vec2 pass_texcoord;
in float dayNoon;
in float nightNoon;
in float dayLightIntens;
in float lightAngleUp;
in float moonSunFlip;


out vec4 out_Color;
#if RENDER_TO_SCENE_FB
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;
#endif



void blendColor(inout vec4 dest, in vec3 color, in float alpha) {
  dest.rgb = mix(dest.rgb, color, alpha);
  dest.a=1.0;
}

void main() { 
	  vec4 pos = unprojectPos(pass_texcoord.st, DEPTH_FAR);
	  vec3 rayDir=-normalize(pos.xyz);
  	vec3 rayOrigin = vec3(0);

    vec3 sunDir = normalize(-SkyLight.lightDir.xyz);
    float sunTheta = max( dot(rayDir, sunDir), 0.0 );
    float sunTheta2 = max( dot(SkyLight.lightDir.xyz, vec3(0, 1, 0)), 0.0 );
    sunTheta2 = max(1.0f-(sunTheta2*sunTheta2*2.0f), 0.0);
    float fNight = smoothstep(0.0f, 1.0f, clamp(nightNoon, 0.0, 1.0f));
    float sunSpotDens = pow(sunTheta, 4.0+fNight*12)*(1.8-fNight*1.7);
    vec3 skySunScat = skyAtmoScat(-rayDir, SkyLight.lightDir.xyz, moonSunFlip, sunTheta2*1);
    float intens = mix(1, 0, moonSunFlip)*dayLightIntens;

    float yt =1-smoothstep(0, 1, abs(dot(rayDir, vec3(0, 1, 0))));
    vec3 fogColor=vec3(0.74f, 1.24f, 1.66f)*0.7;
     fogColor=vec3(0.74f, 1.24f, 1.66f)*0.7;
    vec3 fogColor2=vec3(1.14f, 1.34f, 1.66f)*0.4;
     fogColor=mix(fogColor, fogColor2, yt);
    vec3 fogColorLit=mix(fogColor, vec3(fogColor*0.0001), fNight)*0.1;
    vec3 sky = vec3(fogColorLit);
    sunSpotDens*=(1-fNight*0.82);
    float scatbr = clamp((skySunScat.r+skySunScat.b+skySunScat.g) / 2.0f, 0, 1);
    // sky = mix(sky, sky*skySunScat, clamp(0.14f+sunTheta, 0, 1));
    sky += skySunScat*1.5f;
    // sky = mix(sky, sky*skySunScat, 0.3f-fNight*0.2f);
    // sky += skySunScat*sunSpotDens*1.2;
    // sky += sky*SkyLight.La.rgb*(1.0-sunSpotDens)*1.1f;
    // sky *= .32;



    // float scatbr = clamp((skySunScat.r+skySunScat.b+skySunScat.g) / 2.0f, 0, 1);
    // sky = mix(sky, sky*skySunScat, clamp(0.14f+sunTheta, 0, 1));
    // sky += skySunScat*0.5f;
    // float zfar = in_scene.viewport.w;


    vec4 cloudColor = vec4(sky, 1);


    blendColor(cloudColor, fogColorLit, yt*0.9);
#if RENDER_TO_SCENE_FB
    out_Color = vec4(cloudColor.rgb*0.02, 1.0);
    out_Normal = vec4(0.5);
    uint renderData = ENCODE_RENDERPASS(8);
    out_Material = uvec4(0u,0u+renderData,0u,0u);
#else
    out_Color = vec4(cloudColor.rgb, 1.0);
#endif
    out_Color = vec4(1,1,0,1);
}
