#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "blockinfo.glsl"
#pragma include "vertex_layout.glsl"

out vec4 color;
out vec2 texcoord;
out vec3 normal;
flat out uvec4 blockinfo;
flat out uint faceDir; // duplicate data, see comment int terrain.fsh
flat out vec4 faceLight;
flat out vec4 faceLightSky;
out vec2 texPos;
out vec4 vpos;
out vec3 vwpos;
out float isWater;
out float roughness;

#define LIGHT_MASK 0xFu

void main() {
	blockinfo = in_blockinfo;
	
	faceDir = BLOCK_FACEDIR(blockinfo);
    uint blockid = BLOCK_ID(blockinfo);
	
	vec4 camNormal = in_matrix_3D.normal * vec4(in_normal.xyz, 1);
	camNormal.xyz/=camNormal.w;// not required? (3x3 does not w)
	
	normal = normalize(camNormal.xyz);
	texcoord = in_texcoord.st;
    isWater = IS_WATER(blockid);
	color = in_color;
	roughness = (in_normal.w < 0 ? 255+in_normal.w : in_normal.w) / 255.0f;

	faceLightSky = vec4(
		float(in_light.y&LIGHT_MASK)/15.0,
		float((in_light.y>>4u)&LIGHT_MASK)/15.0,
		float((in_light.y>>8u)&LIGHT_MASK)/15.0,
		float((in_light.y>>12u)&LIGHT_MASK)/15.0
		);
	faceLight = vec4(
		float(in_light.x&LIGHT_MASK)/15.0,
		float((in_light.x>>4u)&LIGHT_MASK)/15.0,
		float((in_light.x>>8u)&LIGHT_MASK)/15.0,
		float((in_light.x>>12u)&LIGHT_MASK)/15.0
		);

	texPos = clamp(in_texcoord.xy, vec2(0), vec2(1));
	

	vpos = in_matrix_3D.mv * vec4(in_position.xyz, 1.0);
	vwpos = in_position.xyz;
	// gl_Position = in_matrix_3D.mvp * (in_position+terroffset);
	gl_Position = in_matrix_3D.mvp * vec4(in_position.xyz, 1.0);
}
