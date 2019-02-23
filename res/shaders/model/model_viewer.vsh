#version 150 core

#pragma include "ubo_transform.glsl"
#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"
#pragma include "tonemap.glsl"

uniform mat4 model_matrix;
uniform mat4 normal_matrix;

out vec4 pass_color;
out vec4 pass_normal;
out vec4 pass_texcoord;
out vec4 pass_position;
 
void main(void) {
	pass_normal = normal_matrix * vec4(in_normal.xyz, 1);
	// camNormal.xyz/=camNormal.w;
	// pass_normal = normalize(camNormal.xyz);
	pass_color = in_color;
	vec4 pos = in_position;
	pass_texcoord = in_texcoord;
	pass_position = model_matrix * vec4(in_position.xyz - RENDER_OFFSET + PX_OFFSET.xyz, in_position.w);
	
    gl_Position = in_matrix_3D.mvp * pass_position;

}