#version 150 core
 
#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

uniform int shadowSplit;

void main() {
	gl_Position = in_matrix.shadow_split_mvp[shadowSplit] * in_position;
}
