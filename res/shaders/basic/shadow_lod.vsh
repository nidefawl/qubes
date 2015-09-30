#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"


uniform int shadowSplit;
flat out uvec4 blockinfo;
out vec2 texcoord;

void main() {
	blockinfo = in_blockinfo;
	texcoord = in_texcoord.st;
	gl_Position = in_matrix_shadow.shadow_split_mvp[shadowSplit] * in_position;
}
