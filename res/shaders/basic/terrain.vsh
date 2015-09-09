#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

// uniform vec4 terroffset;
out vec4 color;
out vec4 texcoord;
out vec3 normal;
flat out uvec4 blockinfo;
flat out vec4 faceAO;
flat out vec4 faceLight;
flat out vec4 faceLightSky;
out vec2 texPos;
#define MAX_AO 3.0

const vec4 aoLevels = vec4(0, 0.2, 0.4, 0.6)*0.85;
#define LIGHT_MASK 0xFu
#define AO_MASK 0x3u
void main() {
	texcoord = in_texcoord;
	vec4 camNormal = in_matrix.normal * vec4(in_normal.xyz, 1);
	camNormal.xyz/=camNormal.w;
	normal = normalize(camNormal.xyz);
	color = in_color;

	faceAO = vec4(
		aoLevels[in_blockinfo.z&AO_MASK],
		aoLevels[(in_blockinfo.z>>2)&AO_MASK],
		aoLevels[(in_blockinfo.z>>4)&AO_MASK],
		aoLevels[(in_blockinfo.z>>6)&AO_MASK]
		);
	// faceAO = vec4(0, 0, 0, 0);
	faceLightSky = vec4(
		float(in_light.y&LIGHT_MASK)/15.0,
		float((in_light.y>>4)&LIGHT_MASK)/15.0,
		float((in_light.y>>8)&LIGHT_MASK)/15.0,
		float((in_light.y>>12)&LIGHT_MASK)/15.0
		);
	faceLight = vec4(
		float(in_light.x&LIGHT_MASK)/15.0,
		float((in_light.x>>4)&LIGHT_MASK)/15.0,
		float((in_light.x>>8)&LIGHT_MASK)/15.0,
		float((in_light.x>>12)&LIGHT_MASK)/15.0
		);
	// faceLight = vec4(1,1,1,1)*0;
	// faceLightSky = vec4(1,1,1,1)*0.8;
	texPos = clamp(in_texcoord.xy, vec2(0), vec2(1));
	// light = vec2(1);


	blockinfo = in_blockinfo;

	// gl_Position = in_matrix.mvp * (in_position+terroffset);
	gl_Position = in_matrix.mvp * in_position;
}
