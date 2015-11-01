#version 150 core

#pragma include "ubo_scene.glsl"
#pragma attributes "shadow"
in vec4 in_position; 
in vec4 in_texcoord; 
in uvec4 in_blockinfo;



void main() {
	gl_Position = in_matrix_3D.mvp * in_position;
}
