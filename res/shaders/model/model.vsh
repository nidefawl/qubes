#version 150 core
#extension GL_ARB_shader_storage_buffer_object : enable

#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"
#pragma define "SHADOW_PASS"
#pragma attributes "model"


in vec4 in_position; 
in vec4 in_normal; 
in vec4 in_texcoord; 
in ivec4 in_bones1;
in ivec4 in_bones2;
in vec4 in_weights1;
in vec4 in_weights2;

#ifdef SHADOW_PASS
uniform int shadowSplit;
#endif
/** Slow layout (single ssbo)
struct QModelData {
    mat4 modelMatrix;
    mat4 normalMatrix;
    mat4 jointMatrices[32];
};
layout (std430) buffer QModelMat
{
	QModelData models[];
} qmodelmatbuffer;
*/

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

out vec4 color;
out vec3 normal;
out vec4 texcoord;
out vec4 position;
 
void main(void) {
	color = vec4(vec3(0.6), 1.0);
	vec3 normal1 = in_normal.xyz;
	vec4 pos = in_position;
	// QModelData data = qmodelmatbuffer.models[gl_InstanceID];
	mat4 normalMatrix = qmodelmatbuffer_normal.normalMatrix[gl_InstanceID];
	mat4 modelMatrix = qmodelmatbuffer_model.modelMatrix[gl_InstanceID];
#ifdef SHADOW_PASS
	if (shadowSplit < 2) {
#endif
	mat4 boneMatrix = mat4(0);
	int nBones = 0;
	int baseOffset = gl_InstanceID*32;
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
#ifdef SHADOW_PASS
	}
#endif
	pos = modelMatrix * pos;
#ifndef SHADOW_PASS
	texcoord = in_texcoord;
	position = pos;
	normal = normalize(normalMatrix * vec4(normal1, 1.0)).xyz;
    gl_Position = in_matrix_3D.mvp * pos;
#else
	gl_Position = in_matrix_shadow.shadow_split_mvp[shadowSplit] * pos;
#endif
}