#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"
#pragma include "SMAA_common.glsl"
#define SMAA_INCLUDE_VS 1
#define SMAA_INCLUDE_PS 0
#pragma include "SMAA.hlsl"

out vec2 pass_texcoord;
out vec4 offset;


void main(void) {
    pass_texcoord = in_texcoord.st;
    offset = vec4(0.0, 0.0, 0.0, 0.0);
    SMAANeighborhoodBlendingVS(pass_texcoord, offset);
    vec4 pos = vec4(in_position.xyz, in_position.w);
    gl_Position = in_matrix_2D.mvp * pos;
}