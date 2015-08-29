#version 150 core
 
#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

uniform int shadowSplit;

void main() {
	if (shadowSplit == 0) {
		gl_Position = in_matrix.shadow_split0_mvp * in_position;
	} else if (shadowSplit == 1) {
		gl_Position = in_matrix.shadow_split1_mvp * in_position;
	} else {
		gl_Position = in_matrix.shadow_split2_mvp * in_position;
	}
}
