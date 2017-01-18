#version 150 core
#extension GL_ARB_shader_storage_buffer_object : enable
#extension GL_NV_gpu_shader5 : enable

#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"
#pragma attributes "staticmodel"
#define ATTR_SSBO_STRUCT 0
#define ATTR_VERTEX_ATTR 1
#pragma define "ATTR_MODE"
#pragma define "MAX_PARTICLES"



in vec4 in_position;
in vec4 in_normal;
in vec4 in_texcoord;


out vec3 normal;
out vec4 texcoord;
out vec4 position;
out vec3 color;

#if ATTR_MODE == ATTR_VERTEX_ATTR

in vec4 in_offset;
in vec4 in_color;

#endif


#if ATTR_MODE == ATTR_SSBO_STRUCT

flat out uint blockinfo32;

struct ParticleData {
    vec4 offset;
    vec4 color;
  	/*f16vec4 offset2;
  	f16vec4 offset4;*/
};

layout (std430) buffer ParticleCube_buffer
{
    ParticleData data[MAX_PARTICLES];
} particlecube;

#endif




 
void main(void) {
	texcoord = in_texcoord;

#if ATTR_MODE == ATTR_SSBO_STRUCT
	ParticleData data = particlecube.data[gl_InstanceID];
	vec4 in_offset = data.offset;
	float scale = data.offset.w;
	// color = vec3(float(data.color.r)/255.0, float(data.color.g)/255.0, float(data.color.b)/255.0);
	color = data.color.rgb;
#endif
#if ATTR_MODE == ATTR_VERTEX_ATTR
	float scale = in_offset.w;
	color = in_color.rgb;
#endif
	texcoord*=scale;
	normal = normalize(in_normal.xyz);
	// texPos = clamp(in_texcoord.xy, vec2(0), vec2(1));
	position = in_position;
	position.xyz *= scale;
	position.xyz += in_offset.xyz;
	gl_Position = in_matrix_3D.mvp * position;
}