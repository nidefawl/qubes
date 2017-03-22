#version 450

#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable

#pragma include "ubo_scene.glsl"
#pragma include "ubo_shadow.glsl"
#pragma include "vertex_layout.glsl"

out vec2 pass_texcoord;
out float pass_LodBias;
out vec3 pass_normal;
out vec3 pass_ViewVec;
out vec3 pass_LightVec;
out vec4 pass_position;

const mat4 biasMat = mat4( 
	0.25, 0.0, 0.0, 0.0,
	0.0, 0.25, 0.0, 0.0,
	0.0, 0.0, 1.0, 0.0,
	0.5, 0.5, 0.0, 1.0 );

#ifdef VULKAN_GLSL
out gl_PerVertex 
{
    vec4 gl_Position;   
};
#endif 

void main() 
{
	pass_texcoord = in_texcoord.st;
	pass_LodBias = 0.0;


	gl_Position = in_matrix_3D.mvp * in_position;
    
    vec3 viewPos = -vec3(in_matrix_3D.mv*vec4(0.0, 0.0, 0.0, 1.0));

    vec4 pos = in_matrix_3D.mv * in_position;
    pass_position = in_position;
	pass_normal = mat3(inverse(transpose(in_matrix_3D.mv))) * in_normal.xyz;
	vec3 lightPos = vec3(0.0);
	vec3 lPos = mat3(in_matrix_3D.mv) * lightPos.xyz;
    pass_LightVec = lPos - pos.xyz;
    pass_ViewVec = viewPos.xyz - pos.xyz;		
}
