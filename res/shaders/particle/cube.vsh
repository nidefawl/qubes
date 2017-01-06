#version 150 core
#extension GL_ARB_shader_storage_buffer_object : enable

#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"
#pragma attributes "staticmodel"

#define LIGHT_MASK 0xFu

in vec4 in_position;
in vec4 in_normal;
in vec4 in_texcoord;


out vec4 color;
out vec3 normal;
out vec4 texcoord;
out vec4 position;
flat out uint blockinfo32;
out float lightLevelSky;
out float lightLevelBlock;

// #define DBG
	#ifdef DBG
struct ParticleData {
    uint blockinfo;
};
	#else
struct ParticleData {
    vec4 color;
    uint blockinfo;
    uint blockinfo2;
    float tx;
    float ty;
};
	#endif
layout (std430) buffer ParticleCube_blockinfo
{
    ParticleData data[];
} particlecube_buffer_blockinfo;

layout (std430) buffer ParticleCube_mat_model
{
    mat4 modelMatrix[];
} particlecube_buffer_mat;



 
void main(void) {
	texcoord = in_texcoord;
	float scale = 1.0f;
	mat4 modelMatrix = particlecube_buffer_mat.modelMatrix[gl_InstanceID];

	blockinfo32 = particlecube_buffer_blockinfo.data[gl_InstanceID].blockinfo;
	
	#ifdef DBG
	lightLevelBlock = 1;
	lightLevelSky = 0;
	color = vec4(1.0);
	scale = 1.0;
	#else
	uint blockinfo32_2 = particlecube_buffer_blockinfo.data[gl_InstanceID].blockinfo2;
	lightLevelBlock = float(blockinfo32_2&LIGHT_MASK)/15.0;
	lightLevelSky = float((blockinfo32_2 >> 4u)&LIGHT_MASK)/15.0;

	texcoord.x += particlecube_buffer_blockinfo.data[gl_InstanceID].tx;
	texcoord.y += particlecube_buffer_blockinfo.data[gl_InstanceID].ty;
	color = particlecube_buffer_blockinfo.data[gl_InstanceID].color;
	scale = color.a;
	#endif
	color.a = 1.0;
	texcoord*=scale;
	mat4 normalMatrix1 = transpose(inverse(modelMatrix));
	normal = in_normal.xyz;
	normal = (normalMatrix1 * vec4(normal, 1)).xyz;
	// camNormal.xyz/=camNormal.w;
	normal = normalize(normal.xyz);
	// texPos = clamp(in_texcoord.xy, vec2(0), vec2(1));
	position = in_position;
	// position.xyz *= scale;
	position = modelMatrix*position;
	// gl_Position = in_matrix_3D.mvp * position;
    gl_Position = in_matrix_3D.mvp * vec4(position.xyz - RENDER_OFFSET + PX_OFFSET.xyz, position.w);
}