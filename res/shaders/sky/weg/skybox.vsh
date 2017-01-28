#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

out vec4 pass_Color;
out vec2 pass_texcoord;
 
out vec3 vposition;

void main() {
	pass_Color = in_color;
	vposition = (in_matrix_3D.view * in_position).xyz;
	gl_Position = in_matrix_3D.vp * in_position;
}