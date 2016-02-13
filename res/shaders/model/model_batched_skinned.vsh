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


in vec4 in_position; 
in vec4 in_normal; 
in vec4 in_texcoord; 
in ivec4 in_bones1;
in ivec4 in_bones2;
in vec4 in_weights1;
in vec4 in_weights2;

#if RENDERER == RENDERER_WORLD_SHADOW
uniform int shadowSplit;
#elif RENDERER == RENDERER_WORLD_MAIN
#define MAT_MVP in_matrix_3D.mvp
#elif RENDERER == RENDERER_MODELVIEWER
#define MAT_MVP in_matrix_3D.mvp
#elif RENDERER == RENDERER_SCREEN
uniform mat4 mvp;
#define MAT_MVP mvp
#endif


layout (std430) buffer QModel_mat_model
{
    mat4 modelMatrix[];
} qmodelmatbuffer_model;

layout (std430) buffer QModel_mat_normal
{
    mat4 normalMatrix[];
} qmodelmatbuffer_normal;

layout (std430) buffer QModel_mat_bone
{
    mat4 boneMatrices[];
} qmodelmatbuffer_bones;

out vec4 pass_color;
out vec3 pass_normal;
out vec4 pass_texcoord;
out vec4 pass_position;
 
void main(void) {
	pass_color = vec4(vec3(0.6), 1.0);
	vec3 normal1 = in_normal.xyz;
	vec4 pos = in_position;
	// QModelData data = qmodelmatbuffer.models[gl_InstanceID];
	mat4 normalMatrix = qmodelmatbuffer_normal.normalMatrix[gl_InstanceID];
	mat4 modelMatrix = qmodelmatbuffer_model.modelMatrix[gl_InstanceID];
#if RENDERER == RENDERER_WORLD_SHADOW
	if (shadowSplit < 2) {
#endif
	mat4 boneMatrix = mat4(0);
	int nBones = 0;
	int baseOffset = gl_InstanceID*64;
	for (int j = 0; j < 2; j++) {
		vec4 weight4 = j == 0 ? in_weights1 : in_weights2;
		ivec4 bones4 = j == 0 ? in_bones1 : in_bones2;
		for (int i = 0; i < 4; i++) {
			int idx = bones4.x;
			float weight = weight4.x;
			if (idx < 64 && weight > 0) {
				mat4 boneMat = qmodelmatbuffer_bones.boneMatrices[baseOffset+idx];
				boneMatrix += boneMat * weight;
				nBones++;
			}
			weight4 = weight4.yzwx;
			bones4 = bones4.yzwx;
		}
	}
	if (nBones > 0) {
		pos = boneMatrix * pos;
		normal1 = mat3(boneMatrix)*normal1;
	}
#if RENDERER == RENDERER_WORLD_SHADOW
	}
#endif
	pos = modelMatrix * pos;
#if RENDERER != RENDERER_WORLD_SHADOW
	pass_texcoord = in_texcoord;
	pass_position = pos;
	pass_normal = normalize(normalMatrix * vec4(normal1, 1.0)).xyz;
    gl_Position = MAT_MVP * pos;
#else
	gl_Position = in_matrix_shadow.shadow_split_mvp[shadowSplit] * pos;
#endif
}