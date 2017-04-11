#version 150 core

#pragma attributes "shadow"
#pragma include "ubo_scene.glsl"
#pragma include "ubo_shadow.glsl"


#ifdef VULKAN_GLSL

layout(push_constant) uniform PushConsts {
  mat4 model_matrix;
  int shadowSplit;
} pushConsts;
#define MODEL_MATRIX pushConsts.model_matrix
#define SPLIT_IDX pushConsts.shadowSplit
#else
//OpenGL 
in vec4 in_position; 
in vec2 in_texcoord; 
in uvec2 in_blockinfo;
uniform mat4 model_matrix;
uniform int shadowSplit;
#define MODEL_MATRIX model_matrix
#define SPLIT_IDX shadowSplit

#endif

out vec2 texcoord;
flat out uvec2 blockinfo;
#ifdef VULKAN_GLSL
out gl_PerVertex 
{
    vec4 gl_Position;   
};
#endif 
void main() {
	blockinfo = in_blockinfo.xy;
	texcoord = in_texcoord.st;
	gl_Position = in_matrix_shadow.shadow_split_mvp[SPLIT_IDX] * MODEL_MATRIX * vec4(in_position.xyz, 1.0);
}
