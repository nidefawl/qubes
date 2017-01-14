#version 150 core

#pragma attributes "shadow"
#pragma include "ubo_scene.glsl"
#pragma define "MATRIX"

in vec4 in_position; 
in vec4 in_texcoord; 
in uvec4 in_blockinfo;

uniform int shadowSplit;
uniform mat4 model_matrix;

flat out uvec4 blockinfo;
out vec2 texcoord;

void main() {
	blockinfo = in_blockinfo;
	texcoord = in_texcoord.st;
	gl_Position = MATRIX * in_position;
}
