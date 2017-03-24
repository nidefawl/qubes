#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"
#pragma include "blockinfo.glsl"

#ifdef VULKAN_GLSL

layout(push_constant) uniform PushSingleBlock3D {
  mat4 model_matrix;
} pushSingleBlock3d;
#define MODEL_MATRIX pushSingleBlock3d.model_matrix
#else
uniform mat4 in_modelMatrix;
#define MODEL_MATRIX in_modelMatrix
#endif

out vec3 normal;
out vec4 color;
out vec2 texcoord;
flat out uvec4 blockinfo;

void main() {



	vec4 camNormal = in_matrix_3D.normal * vec4(in_normal.xyz, 1.0);
	normal = normalize(camNormal.xyz);
	color = in_color;
	texcoord = in_texcoord.st;
	blockinfo = in_blockinfo;
	gl_Position = in_matrix_3D.p * MODEL_MATRIX * vec4(in_position.xyz, 1.0);
}