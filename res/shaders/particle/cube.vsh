#version 150 core
#extension GL_ARB_shader_storage_buffer_object : enable

#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"
#pragma attributes "staticmodel"

#define LIGHT_MASK 0xFu


#ifdef VULKAN_GLSL
#define INSTANCE_IDX gl_InstanceIndex
#else 
#define INSTANCE_IDX gl_InstanceID
in vec4 in_position;
in vec4 in_normal;
in vec4 in_texcoord;
#endif


out vec4 color;
out vec3 normal;
out vec2 texcoord;
out vec4 position;
flat out uint blockinfo32;
out float lightLevelSky;
out float lightLevelBlock;
out float debgufloat1;
out vec4 debug1;

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
layout (set = 3, binding = 0, std430) buffer ParticleCube_blockinfo
{
    ParticleData data[1024];
} particlecube_buffer_blockinfo;

layout (set = 3, binding = 1, std430) buffer ParticleCube_mat_model
{
    mat4 modelMatrix[1024];
} particlecube_buffer_mat;



 
void main(void) {
	texcoord = in_texcoord.st;
	float scale = 1.0f;
	mat4 modelMatrix = particlecube_buffer_mat.modelMatrix[INSTANCE_IDX];
	debgufloat1=float(INSTANCE_IDX);
	debug1=particlecube_buffer_mat.modelMatrix[0][0];
	blockinfo32 = particlecube_buffer_blockinfo.data[INSTANCE_IDX].blockinfo;
	
	#ifdef DBG
	lightLevelBlock = 1;
	lightLevelSky = 0;
	color = vec4(1.0);
	scale = 1.0;
	#else
	uint blockinfo32_2 = particlecube_buffer_blockinfo.data[INSTANCE_IDX].blockinfo2;
	lightLevelBlock = float(blockinfo32_2&LIGHT_MASK)/15.0;
	lightLevelSky = float((blockinfo32_2 >> 4u)&LIGHT_MASK)/15.0;

	texcoord.x += particlecube_buffer_blockinfo.data[INSTANCE_IDX].tx;
	texcoord.y += particlecube_buffer_blockinfo.data[INSTANCE_IDX].ty;
	color = particlecube_buffer_blockinfo.data[INSTANCE_IDX].color;
	scale = color.a;
	#endif
	color.a = 1.0;
	texcoord*=scale;
	mat4 normalMatrix1 = transpose(inverse(modelMatrix));
	normal = in_normal.xyz;
	normal = (normalMatrix1 * vec4(normal, 1)).xyz;
	normal = normalize(normal.xyz);
	position = modelMatrix*vec4(in_position.xyz, 1.0);
    gl_Position = in_matrix_3D.mvp * vec4(position.xyz - RENDER_OFFSET, position.w);
}