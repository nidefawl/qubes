#version 150 core

#pragma include "tonemap.glsl"
#pragma include "blockinfo.glsl"

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

#ifdef VULKAN_GLSL
layout(push_constant) uniform PushConstsClouds {
  float transparency;
  float spritebrightness;
} pushConstsClouds;
#define SPRITE_TRANSPARENCY pushConstsClouds.transparency
#define SPRITE_BRIGHTNESS pushConstsClouds.spritebrightness
#else
uniform float transparency;
uniform float spritebrightness;
#define SPRITE_TRANSPARENCY transparency
#define SPRITE_BRIGHTNESS spritebrightness
#endif



in vec4 color;
in vec4 texcoord;
in vec4 position;
in float dayNoon;
in float nightNoon;
in float dayLightIntens;
in float lightAngleUp;
in float moonSunFlip;

out vec4 out_Color;

void main(void) {

  float fNight = smoothstep(0.0, 1.0, clamp(nightNoon, 0.0, 1.0));
  float sunTheta = dot(normalize(position.xyz), normalize(SkyLight.lightDir.xyz))+0.7;
  float sun = clamp( pow(sunTheta, 16)*0.3, 0.0, 1.0 );
  float sunLight = dayLightIntens *(0.008+0.67*(1.0-fNight)+0.3*sun);
	vec4 tex = texture(tex0, texcoord.st);
  float dist = length(position);
  vec3 rgb = tex.rgb*color.rgb*spritebrightness*sunLight;
  out_Color = vec4(rgb, tex.a*transparency*color.a); //vec4(skycolor*0.41, clamp(tex.a*(1-fogAmount)*color.a*0.2, 0, 1));
}