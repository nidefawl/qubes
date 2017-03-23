#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"
#pragma attributes "particle"

layout(set = 3, binding = 0, std140) uniform LightInfo
{
  vec4 dayLightTime; 
  vec4 posSun; // Light position in world space
  vec4 lightDir; // Light dir in world space
  vec4 La; // Ambient light intensity
  vec4 Ld; // Diffuse light intensity
  vec4 Ls; // Specular light intensity
} SkyLight;

in vec4 in_texcoord; 
in vec4 in_position; 
in vec4 in_color; 


out vec4 color;
out vec4 texcoord;
out vec4 position;
out float dayNoon;
out float nightNoon;
out float dayLightIntens;
out float lightAngleUp;
out float moonSunFlip;



float   easeInOutCubic(float t)
{
	return t<0.5 ? 4.0*t*t*t : (t-1.0)*(2.0*t-2.0)*(2.0*t-2.0)+1.0;
}
 
void main(void) {

	dayNoon = easeInOutCubic(SkyLight.dayLightTime.x);
	nightNoon = easeInOutCubic(SkyLight.dayLightTime.y);
	dayLightIntens = SkyLight.dayLightTime.z;
	lightAngleUp = SkyLight.dayLightTime.w;
	moonSunFlip = dayNoon > nightNoon ? 0 : 1;



	float dists = 100;
	float rot = in_color.w;
	vec3 nPos = normalize(in_position.xyz);
	float upness = (max(0, dot(vec3(0,1,0), nPos)));
	float scale = in_position.w*upness;

	float d5 = nPos.x * dists;
	float d6 = nPos.y * dists;
	float d7 = nPos.z * dists;

	float d8 = atan(nPos.x, nPos.z);
	float fd = sqrt(nPos.x*nPos.x+nPos.z*nPos.z);
	float d11 = atan(fd, nPos.y);

	float d9 = sin(d8);
	float d10 = cos(d8);

	float d12 = sin(d11);
	float d13 = cos(d11);
	
	float d15 = sin(rot);
	float d16 = cos(rot);
  	vec2 offsetxy = (in_texcoord.xy-vec2(0.5));
  	float d18 = offsetxy.y * 2.0 * scale;
  	float d19 = offsetxy.x * 2.0 * scale;
  	float d21 = d18 * d16 - d19 * d15;
  	float d23 = d19 * d16 + d18 * d15;
  	float d25 = -d21 * d13;
  	float d26 = d25 * d9 - d23 * d10;
  	float d27 = d21 * d12;
  	float d28 = d23 * d9 + d25 * d10;
  	vec3 oPos = vec3(d5 + d26, d6 + d27, d7 + d28);
	vec4 pos = in_matrix_3D.view * vec4(oPos, 1.0);
	position = vec4(oPos, 1.0);//&vec4(pos, 1.0);
    gl_Position = in_matrix_3D.p * pos;
	// vec4 camNormal = in_matrix_3D.normal * vec4(0, 0, -1, 1);
	// normal = normalize(camNormal.xyz);
	texcoord = in_texcoord;
	color = vec4(in_color.rgb, 1.0*upness);
}