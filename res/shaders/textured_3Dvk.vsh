#version 450

#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable

layout (location = 0) in vec3 inPos;
layout (location = 1) in vec3 inNormal;
layout (location = 2) in vec2 inUV;

layout(std140, set = 0, binding = 0) uniform uboMatrix3D
{
    mat4 mvp;
    mat4 mv;
    mat4 view;
    mat4 vp;
    mat4 p;
    mat4 normal;
    mat4 mv_inv;
    mat4 proj_inv;
    mat4 mvp_inv;
} in_matrix_3D;

layout (location = 0) out vec2 outUV;
layout (location = 1) out float outLodBias;
layout (location = 2) out vec3 outNormal;
layout (location = 3) out vec3 outViewVec;
layout (location = 4) out vec3 outLightVec;

out gl_PerVertex 
{
    vec4 gl_Position;   
};

void main() 
{
	outUV = inUV;
	outLodBias = 0.0;


	gl_Position = in_matrix_3D.mvp * vec4(inPos.xyz, 1.0);
    
    vec3 viewPos = -vec3(in_matrix_3D.mv*vec4(0.0, 0.0, 0.0, 1.0));

    vec4 pos = in_matrix_3D.mv * vec4(inPos, 1.0);
	outNormal = mat3(inverse(transpose(in_matrix_3D.mv))) * inNormal;
	vec3 lightPos = vec3(0.0);
	vec3 lPos = mat3(in_matrix_3D.mv) * lightPos.xyz;
    outLightVec = lPos - pos.xyz;
    outViewVec = viewPos.xyz - pos.xyz;		
}
