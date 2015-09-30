#version 440 core
//#extension GL_AMD_vertex_shader_viewport_index : enable //CATALYST DRIVER BUG: shows warning, we can write gl_Layer/viewport anyways

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

uniform int shadowSplit;

void main() {
	gl_Position = in_matrix_shadow.shadow_split_mvp[gl_InstanceID] * in_position;
	gl_ViewportIndex = gl_InstanceID;
}
