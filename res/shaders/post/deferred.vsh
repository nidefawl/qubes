#version 150 core

layout(std140) uniform LightInfo {
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
	vec2 pos;
	pos.x = float(gl_VertexID & 1)*2.0;
	pos.y = float(gl_VertexID & 2);
    pass_texcoord.st = pos;
    gl_Position = vec4(pos * 2.0 - 1.0, 0, 1);
}