#version 150 core

#pragma define "TERRAIN_DRAW_MODE"

#pragma include "ubo_scene.glsl"
#pragma include "ubo_constants.glsl"
#pragma include "vertex_layout.glsl"

uniform vec3 in_offset;
uniform float in_scale;

out vec4 color;
out vec4 texcoord;
out vec3 normal;
out vec4 position;
flat out uvec4 blockinfo;
// flat out vec4 faceAO;
// flat out vec4 faceLight;
// flat out vec4 faceLightSky;
out float blockside;
out vec2 texPos;


#define MAX_AO 3.0

const vec4 aoLevels = normalize(vec4(0, 0.2, 0.4, 0.6));
#define LIGHT_MASK 0xFu
#define AO_MASK 0x3u
#define DEBUG_MODE2
#define BR_0 0.4f
#define BR_1 0.7f
#define BR_2 0.9f
#define BR_3 1.0f
// #define BR_0 0.6f
// #define BR_1 0.8f
// #define BR_2 0.9f
// #define BR_3 1.0f
float blocksidebrightness[6] = float[6](BR_2, BR_2, BR_3, BR_0, BR_1, BR_1);
void main() {
	vec4 camNormal = in_matrix_3D.normal * vec4(in_normal.xyz, 1);
	camNormal.xyz/=camNormal.w;
	normal = normalize(camNormal.xyz);
	color = in_color;
	blockinfo = in_blockinfo;

	vec4 pos = in_position;
	texcoord = in_texcoord;
	texPos = clamp(in_texcoord.xy, vec2(0), vec2(1));
	float distCam = length(in_position-in_scene.cameraPosition);
#if TERRAIN_DRAW_MODE == 0
	uint faceDir = blockinfo.w&0x7u;
	uint vertDir = (blockinfo.w >> 3u) & 0x3Fu;
	vec3 dir = vertexDir.dir[vertDir].xyz;
#else 
	uint faceDir = blockinfo.w;
	vec3 dir = in_direction.xyz*in_direction.w;
#endif
	const float face_offset = 1/32.0;
	float distScale = face_offset*clamp(pow((distCam+8)/200, 1.35), 0.0008, 1);
	pos.x += dir.x*distScale;
	pos.y += dir.y*distScale;
	pos.z += dir.z*distScale;
	// pos.xyz *= 1/32.0;

	blockside = blocksidebrightness[faceDir];
// // #ifndef FAR_BLOCKFACE
// 	faceAO = vec4(
// 		aoLevels[in_blockinfo.z&AO_MASK],
// 		aoLevels[(in_blockinfo.z>>2)&AO_MASK],
// 		aoLevels[(in_blockinfo.z>>4)&AO_MASK],
// 		aoLevels[(in_blockinfo.z>>6)&AO_MASK]
// 		);
// // #endif

// 	faceLightSky = vec4(
// 		float(in_light.y&LIGHT_MASK)/15.0,
// 		float((in_light.y>>4)&LIGHT_MASK)/15.0,
// 		float((in_light.y>>8)&LIGHT_MASK)/15.0,
// 		float((in_light.y>>12)&LIGHT_MASK)/15.0
// 		);

// 	faceLight = vec4(
// 		float(in_light.x&LIGHT_MASK)/15.0,
// 		float((in_light.x>>4)&LIGHT_MASK)/15.0,
// 		float((in_light.x>>8)&LIGHT_MASK)/15.0,
// 		float((in_light.x>>12)&LIGHT_MASK)/15.0
// 		);
	

	position = pos;
	vec4 outpos = in_matrix_2D.mvp3DOrtho * pos;
	vec2 off = in_offset.xy/in_scene.viewport.xy*2;
	off.x -= 1;
	off.y -= 1;
	outpos.xyz *= in_scale;
	outpos.x += off.x;
	outpos.y -= off.y;
	gl_Position = outpos;
}
