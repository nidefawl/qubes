#version 150 core
 
#pragma include "ubo_scene.glsl"
#pragma attributes "shadow"
#pragma define "MATRIX"
in vec4 in_position; 
in vec4 in_texcoord; 
in uvec4 in_blockinfo;

uniform int shadowSplit;
uniform mat4 model_matrix;

void main() {
	gl_Position = MATRIX * in_position;
}
