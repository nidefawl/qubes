#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

out vec2 pass_texcoord;

out vec3 pass_rayDir;
out vec3 pass_rayOrigin;
 

void main(void) {
    pass_texcoord = in_texcoord.st;
    
    vec4 pos = vec4(in_position.xyz+PX_OFFSET.xyz, in_position.w);
    

    pass_rayOrigin = normalize(mat3(in_matrix_3D.mv_inv)*(in_matrix_3D.proj_inv * vec4(in_texcoord.s * 2.0f - 1.0f, in_texcoord.t * 2.0f - 1.0f, 0.0f, 1.0f)).xyz);
    pass_rayOrigin.xyz += CAMERA_POS.xyz;
    pass_rayDir = normalize(mat3(in_matrix_3D.mv_inv)*(in_matrix_3D.proj_inv * vec4(in_texcoord.s * 2.0f - 1.0f, in_texcoord.t * 2.0f - 1.0f, 1.0f, 1.0f)).xyz);

    gl_Position = in_matrix_2D.mvp * pos;
}