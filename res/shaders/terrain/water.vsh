#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

out vec4 color;
out vec4 texcoord;
out vec3 normal;
flat out uvec4 blockinfo;

flat out uint faceDir; // duplicate data, see comment int terrain.fsh
flat out vec4 faceLight;
flat out vec4 faceLightSky;
out vec2 texPos;
out vec4 vpos;
out vec4 vwpos;
out float isWater;

#define LIGHT_MASK 0xFu

void main() {
	blockinfo = in_blockinfo;
	faceDir = blockinfo.w&0x7u;
    uint blockid = (in_blockinfo.y&0xFFFu);
	vec4 camNormal = in_matrix_3D.normal * vec4(in_normal.xyz, 1);
	camNormal.xyz/=camNormal.w;// not required? (3x3 does not w)
	normal = normalize(camNormal.xyz);
	texcoord = in_texcoord;

    isWater = float(blockid==4u);


	color = in_color;

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

	texPos = clamp(in_texcoord.xy, vec2(0), vec2(1));
	

	vpos = in_matrix_3D.mv * in_position;
	vwpos = in_position;
	// gl_Position = in_matrix_3D.mvp * (in_position+terroffset);
	gl_Position = in_matrix_3D.mvp * in_position;
}
