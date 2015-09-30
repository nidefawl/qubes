#version 150 core

#pragma attributes "shadow"
#pragma include "ubo_scene.glsl"
in vec4 in_position; 
in vec4 in_texcoord; 
in uvec4 in_blockinfo;

uniform int shadowSplit;
flat out uvec4 blockinfo;
out vec2 texcoord;

void main() {
	blockinfo = in_blockinfo;
	texcoord = in_texcoord.st;
	gl_Position = in_matrix_shadow.shadow_split_mvp[shadowSplit] * in_position;
}
