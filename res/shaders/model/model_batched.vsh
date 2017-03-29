#version 150 core
#extension GL_ARB_shader_storage_buffer_object : enable

#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"

#pragma define "RENDERER"
#pragma attributes "model"
#define RENDERER_WORLD_MAIN 0
#define RENDERER_WORLD_SHADOW 1
#define RENDERER_MODELVIEWER 2
#define RENDERER_SCREEN 3

#ifdef VULKAN_GLSL
#define INSTANCE_IDX gl_InstanceIndex
#else 
#define INSTANCE_IDX gl_InstanceID
#endif

#if RENDERER == RENDERER_WORLD_SHADOW
//TODO: conditional includes
layout(set = 4, binding = 0, std140) uniform uboMatrixShadow
{
    mat4 shadow_split_mvp[4];
    vec4 shadow_split_depth;
} in_matrix_shadow;
#endif

#ifndef VULKAN_GLSL
in vec4 in_position; 
in vec4 in_normal; 
in vec4 in_texcoord; 
in uvec4 in_bones1;
in uvec4 in_bones2;
in vec4 in_weights1;
in vec4 in_weights2;
#endif

#if RENDERER == RENDERER_WORLD_SHADOW
	#ifdef VULKAN_GLSL
		layout(push_constant) uniform PushConstantsModelShadowSplit {
		  int shadowSplit;
		} pushCModelShadowSplit;
		#define SHADOW_SPLIT pushCModelShadowSplit.shadowSplit
	#else
		uniform int shadowSplit;
		#define SHADOW_SPLIT shadowSplit
	#endif
#elif RENDERER == RENDERER_WORLD_MAIN
#define MAT_MVP in_matrix_3D.mvp
#elif RENDERER == RENDERER_MODELVIEWER
#define MAT_MVP in_matrix_3D.mvp
#elif RENDERER == RENDERER_SCREEN
uniform mat4 mvp;
#define MAT_MVP mvp
#endif


layout (set = 3, binding = 0, std430) buffer QModel_mat_model
{
    mat4 modelMatrix[];
} qmodelmatbuffer_model;

layout (set = 3, binding = 1, std430) buffer QModel_mat_normal
{
    mat4 normalMatrix[];
} qmodelmatbuffer_normal;



#if RENDERER != RENDERER_WORLD_SHADOW
out vec4 pass_color;
out vec3 pass_normal;
out vec2 pass_texcoord;
out vec4 pass_position;
#endif
 
void main(void) {
	vec3 normal1 = in_normal.xyz;
	vec4 pos = vec4(in_position.xyz, 1.0);
	// QModelData data = qmodelmatbuffer.models[INSTANCE_IDX];
	mat4 normalMatrix = qmodelmatbuffer_normal.normalMatrix[INSTANCE_IDX];
	mat4 modelMatrix = qmodelmatbuffer_model.modelMatrix[INSTANCE_IDX];

	pos = modelMatrix * pos;
#if RENDERER != RENDERER_WORLD_SHADOW
	pass_color = vec4(vec3(0.6), 1.0);
	pass_texcoord = in_texcoord.st;
	pass_position = pos;
	pass_normal = normalize(normalMatrix * vec4(normal1, 1.0)).xyz;
    gl_Position = MAT_MVP * pos;
#else
	gl_Position = in_matrix_shadow.shadow_split_mvp[SHADOW_SPLIT] * pos;
#endif
}