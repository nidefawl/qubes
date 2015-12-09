#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"
#pragma include "tonemap.glsl"

uniform mat4 model_matrix;

out vec4 color;
out vec3 normal;
out vec4 texcoord;
out vec4 position;
 
void main(void) {
	vec4 camNormal = in_matrix_3D.normal * vec4(in_normal.xyz, 1);
	normal = normalize(camNormal.xyz);
	color = in_color;
	vec4 pos = in_position;
	texcoord = in_texcoord;
	position = model_matrix * pos;
    gl_Position = in_matrix_3D.mvp * model_matrix * in_position;
}