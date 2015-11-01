#version 150 core

#pragma define "TERRAIN_DRAW_MODE"

#pragma include "ubo_scene.glsl"
#pragma include "ubo_constants.glsl"
#pragma include "vertex_layout.glsl"

out vec4 texcoord;
out vec4 position;
flat out uvec4 blockinfo;

void main() {
	vec4 pos = in_position;
	texcoord = in_texcoord;
	blockinfo = in_blockinfo;

	float distCam = length(in_position.xyz - CAMERA_POS);
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


	position = pos;
	gl_Position = in_matrix_3D.mvp * pos;
}
