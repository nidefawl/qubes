#version 150 core
 
#pragma include "ubo_scene.glsl"
#pragma attributes "shadow"
in vec4 in_position; 
in vec4 in_texcoord; 
in uvec4 in_blockinfo;

uniform int shadowSplit;

void main() {
	gl_Position = in_matrix_shadow.shadow_split_mvp[shadowSplit] * in_position;
}
