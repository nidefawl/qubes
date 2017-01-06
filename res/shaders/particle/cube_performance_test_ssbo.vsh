#version 150 core
#extension GL_ARB_shader_storage_buffer_object : enable

#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"
#pragma attributes "staticmodel"
#pragma define "PARTICLE_TYPE"
#pragma define "USE_STRUCT_BUFFER"
#pragma define "MAX_PARTICLES"

in vec4 in_position;
in vec4 in_normal;
in vec4 in_texcoord;


out vec3 normal;
out vec4 texcoord;
out vec4 position;

#ifdef PARTICLE_TYPE_BLOCK

	flat out uint blockinfo32;


  #ifdef USE_STRUCT_BUFFER

	struct ParticleData {
	    mat4 modelMatrix;
	    uint blockinfo;
	};
	layout (std430) buffer ParticleCube_data
	{
	    ParticleData data[];
	} particlecube_buffer_data;

  #elif defined USE_ARRAYS_BUFFER

	layout (std430) buffer ParticleCube_data_arrays
	{
	    mat4 modelMatrix[MAX_PARTICLES];
	    uint blockinfo[MAX_PARTICLES];
	} particlecube_buffer_data_arrays;
  #else // SEPERATE BUFFERS
#define WRAPDATA

	struct ParticleData {
	    uint blockinfo;
	    float scale;
	    float tx;
	    float ty;
	};
	layout (std430) buffer ParticleCube_blockinfo
	{
#ifdef WRAPDATA
	    ParticleData data[];
#else
	    uint blockinfo[];
#endif
	} particlecube_buffer_blockinfo;

	layout (std430) buffer ParticleCube_mat_model
	{
	    mat4 modelMatrix[];
	} particlecube_buffer_mat;

  #endif


#endif



 
void main(void) {
	texcoord = in_texcoord;
	float scale = 1.0f;
#ifdef USE_STRUCT_BUFFER
	ParticleData data = particlecube_buffer_data.data[gl_InstanceID];
	mat4 modelMatrix = data.modelMatrix;
	#ifdef PARTICLE_TYPE_BLOCK
		blockinfo32 = data.blockinfo;
	#endif
#elif defined USE_ARRAYS_BUFFER
	mat4 modelMatrix = particlecube_buffer_data_arrays.modelMatrix[gl_InstanceID];
	#ifdef PARTICLE_TYPE_BLOCK
		blockinfo32 = particlecube_buffer_data_arrays.blockinfo[gl_InstanceID];
	#endif
#else
	mat4 modelMatrix = particlecube_buffer_mat.modelMatrix[gl_InstanceID];
	#ifdef PARTICLE_TYPE_BLOCK
		#ifdef WRAPDATA
			blockinfo32 = particlecube_buffer_blockinfo.data[gl_InstanceID].blockinfo;
			scale = particlecube_buffer_blockinfo.data[gl_InstanceID].scale;
			texcoord.x += particlecube_buffer_blockinfo.data[gl_InstanceID].tx;
			texcoord.y += particlecube_buffer_blockinfo.data[gl_InstanceID].ty;
			texcoord*=scale;
		#else
		blockinfo32 = particlecube_buffer_blockinfo.blockinfo[gl_InstanceID];
		#endif
	#endif
#endif
	mat4 normalMatrix1 = transpose(inverse(modelMatrix));
	normal = in_normal.xyz;
	normal = (normalMatrix1 * vec4(normal, 1)).xyz;
	// camNormal.xyz/=camNormal.w;
	normal = normalize(normal.xyz);
	// texPos = clamp(in_texcoord.xy, vec2(0), vec2(1));
	position = in_position;
	// position.xyz *= scale;
	position = modelMatrix*position;
	gl_Position = in_matrix_3D.mvp * position;
}