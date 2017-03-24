#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"
#pragma include "tonemap.glsl"

#ifdef VULKAN_GLSL

layout(push_constant) uniform PushSingleBlock3D {
  mat4 model_matrix;
} pushSingleBlock3d;
#define MODEL_MATRIX pushSingleBlock3d.model_matrix
#else
uniform mat4 model_matrix;
#define MODEL_MATRIX model_matrix
#endif

out vec3 normal;
out vec4 color;
out vec2 texcoord;
 
void main(void) {
	vec4 camNormal = in_matrix_3D.normal * vec4(in_normal.xyz, 1);
	normal = normalize(camNormal.xyz);
	color = in_color;
	texcoord = in_texcoord.st;
    gl_Position = in_matrix_3D.p * MODEL_MATRIX * vec4(in_position.xyz, 1.0);
}