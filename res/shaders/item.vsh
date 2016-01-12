#version 150 core


#pragma include "ubo_scene.glsl"
#pragma include "ubo_constants.glsl"
#pragma include "vertex_layout.glsl"

uniform float in_scale;

out vec4 color;
out vec4 texcoord;
flat out uint idx;

void main() {

	color = in_color;
	idx = in_blockinfo.x;

	vec4 pos = vec4(in_position.xyz + PX_OFFSET.xyz, 1);
	texcoord = in_texcoord;
	vec4 outpos = in_matrix_2D.mvp * pos;
	gl_Position = outpos;
}
