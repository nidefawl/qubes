#version 150 core


#pragma include "ubo_scene.glsl"
#pragma include "ubo_constants.glsl"
#pragma include "vertex_layout.glsl"

uniform vec3 in_offset;
uniform mat4 in_modelMatrix;
uniform float in_scale;

out vec4 color;
out vec4 texcoord;
flat out uint idx;

void main() {

	color = in_color;
	idx = in_blockinfo.x;

	vec4 pos = in_position;
	texcoord = in_texcoord;


	vec4 position = in_matrix_2D.mv3DOrtho * in_modelMatrix * pos;
	vec4 outpos = in_matrix_2D.p3DOrtho * position;
	gl_Position = outpos;
}
