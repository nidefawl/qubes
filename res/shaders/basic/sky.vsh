#version 150 core
 
#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"


out vec4 color;
out vec3 vposition;

void main() {
	color = in_color;
	vposition = (in_matrix.view * in_position).xyz;

	gl_Position = in_matrix.proj * in_matrix.view * in_position;
}
