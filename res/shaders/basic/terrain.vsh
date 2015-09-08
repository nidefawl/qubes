#version 150 core
 
#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

// uniform vec4 terroffset;
out vec4 color;
out vec4 texcoord;
out vec3 normal;
out uvec4 blockinfo;
out vec4 faceAO;
out vec4 faceLight;
out vec4 faceLightSky;
out vec2 texPos;
#define MAX_AO 3.0

const vec4 aoLevels = vec4(0, 0.2, 0.4, 0.6)*0.85;

void main() {
	texcoord = in_texcoord;
	vec4 camNormal = in_matrix.normal * vec4(in_normal.xyz, 1);
	camNormal.xyz/=camNormal.w;
	normal = normalize(camNormal.xyz);
	color = in_color;


	faceAO = vec4(
		aoLevels[in_blockinfo.z&0x3],
		aoLevels[(in_blockinfo.z>>2)&0x3],
		aoLevels[(in_blockinfo.z>>4)&0x3],
		aoLevels[(in_blockinfo.z>>6)&0x3]
		);
	// faceAO = vec4(0, 0, 0, 0);
	faceLightSky = vec4(
		float(in_light.y&0xF)/15.0,
		float((in_light.y>>4)&0xF)/15.0,
		float((in_light.y>>8)&0xF)/15.0,
		float((in_light.y>>12)&0xF)/15.0
		);
	faceLight = vec4(
		float(in_light.x&0xF)/15.0,
		float((in_light.x>>4)&0xF)/15.0,
		float((in_light.x>>8)&0xF)/15.0,
		float((in_light.x>>12)&0xF)/15.0
		);
	// faceLight = vec4(1,1,1,1)*0;
	// faceLightSky = vec4(1,1,1,1)*0.8;
	texPos = clamp(in_texcoord.xy, vec2(0), vec2(1));
	// light = vec2(1);


	blockinfo = in_blockinfo;

	// gl_Position = in_matrix.mvp * (in_position+terroffset);
	gl_Position = in_matrix.mvp * in_position;
}
