#version 150 core
 
#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

layout(std140) uniform LightInfo {
  vec4 vSun; // Light position in eye coords.
  vec4 vMoon; // Light position in eye coords.
  vec4 La; // Ambient light intensity
  vec4 Ld; // Diffuse light intensity
  vec4 Ls; // Specular light intensity
} Light;

layout(std140) uniform MaterialInfo {
  vec4 Ka; // Ambient reflectivity
  vec4 Kd; // Diffuse reflectivity
  vec4 Ks; // Specular reflectivity
  float Shininess; // Specular shininess factor
} Material;

out vec2 pass_texcoord;
out vec3 sunDirection;
out vec3 moonDirection;
out float cosSunUpAngle;
out float dayLight;
out float nightlight;
out float dayLightIntens;

void main() {
	sunDirection = normalize(Light.vSun.xyz);
	moonDirection = normalize(Light.vMoon.xyz);
	cosSunUpAngle = dot(sunDirection, vec3(0,3,0));
	dayLight = clamp(cosSunUpAngle, 0.0f, 1.0f);
	nightlight = 1-dayLight;
	dayLightIntens = clamp(dot(normalize(mix(Light.vSun, Light.vMoon, nightlight).xyz), vec3(0, 1, 0)), 0, 1);
	pass_texcoord = in_texcoord.st;
	gl_Position = in_matrix.mvp * in_position;
}
