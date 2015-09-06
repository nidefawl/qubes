#version 150 core
 
#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

uniform int shadowSplit;
out vec4 color;
out vec4 texcoord;
out vec3 normal;
out vec4 blockinfo;

void main() {
	texcoord = in_texcoord;
	vec4 camNormal = in_matrix.normal * vec4(in_normal.xyz, 1);
	camNormal.xyz/=camNormal.w;
	normal = normalize(camNormal.xyz);
	color = in_color;

	blockinfo = in_blockinfo;
	gl_Position = in_matrix.shadow_split_mvp[shadowSplit] * in_position;
}
