
layout(set = 4, binding = 0, std140) uniform LightInfo
{
  vec4 dayLightTime; 
  vec4 posSun; // Light position in world space
  vec4 lightDir; // Light dir in world space
  vec4 La; // Ambient light intensity
  vec4 Ld; // Diffuse light intensity
  vec4 Ls; // Specular light intensity
} SkyLight;


out vec2 pass_texcoord;
out float dayNoon;
out float nightNoon;
out float dayLightIntens;
out float lightAngleUp;
out float moonSunFlip;
float   easeInOutCubic(float t)
{
	return t<0.5 ? 4.0*t*t*t : (t-1.0)*(2.0*t-2.0)*(2.0*t-2.0)+1.0;
}
void main() {
	dayNoon = easeInOutCubic(SkyLight.dayLightTime.x);
	nightNoon = easeInOutCubic(SkyLight.dayLightTime.y);
	dayLightIntens = SkyLight.dayLightTime.z;
	lightAngleUp = SkyLight.dayLightTime.w;
	moonSunFlip = dayNoon > nightNoon ? 0 : 1;
	// pass_texcoord = in_texcoord.st;
	// gl_Position = in_matrix_2D.mvp * in_position;

#pragma include "fullscreen_triangle_vertex.glsl"
}