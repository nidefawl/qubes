#version 150 core

#pragma include "ubo_scene.glsl"
in vec4 in_position; 
in vec4 in_normal; 
in vec4 in_texcoord; 

uniform mat4 model_matrix;
uniform mat4 normal_matrix;

out vec3 pass_normal;
out vec2 pass_texcoord;

void main(void) {
    pass_texcoord = in_texcoord.st;
	// pass_normal = in_normal.xyz;
	// pass_normal = (normal_matrix * vec4(in_normal.xyz, 1));
	vec4 camNormal = normal_matrix * vec4(in_normal.xyz, 1);
	pass_normal = normalize(camNormal.xyz);
	
	vec4 pos = model_matrix * in_position;
	
	pos.xyz = pos.xyz - RENDER_OFFSET + PX_OFFSET.xyz;
	
    gl_Position = in_matrix_3D.mvp * pos;
}