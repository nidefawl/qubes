#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "ubo_constants.glsl"
#pragma include "vertex_layout.glsl"
#pragma include "blockinfo.glsl"

out vec4 texcoord;
out vec4 position;
flat out uvec4 blockinfo;

void main() {
	vec4 pos = in_position;
	texcoord = in_texcoord;
	blockinfo = in_blockinfo;

	float distCam = length(in_position.xyz - CAMERA_POS);

	uint faceDir = BLOCK_FACEDIR(blockinfo);
	uint vertDir = BLOCK_VERTDIR(blockinfo);
	vec3 dir = vertexDir.dir[vertDir].xyz;

	const float face_offset = 1/32.0;
	float distScale = face_offset*clamp(pow((distCam+8)/200, 1.35), 0.0008, 1);
	pos.x += dir.x*distScale;
	pos.y += dir.y*distScale;
	pos.z += dir.z*distScale;


	position = pos;
	gl_Position = in_matrix_3D.mvp * pos;
}
