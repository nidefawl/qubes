#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

out vec2 inUV;
out float inLodBias;
out vec3 inNormal;
out vec3 inViewVec;
out vec3 inLightVec;

void main(void) {
	inLodBias = 0.0f;
	inUV = in_texcoord.st;
    vec4 modelPos = vec4(in_position.xyz - RENDER_OFFSET + PX_OFFSET.xyz, in_position.w);
    gl_Position = in_matrix_3D.mvp * modelPos;
	vec3 pos = vec3(in_matrix_3D.mv * modelPos);
	inNormal = mat3(inverse(transpose(in_matrix_3D.mv))) * in_normal.xyz;

	vec3 lightPos = vec3(0.0);
	vec3 lPos = mat3(in_matrix_3D.mv) * lightPos.xyz;
    inLightVec = lPos - pos.xyz;
    inViewVec = CAMERA_POS - pos.xyz;	
}