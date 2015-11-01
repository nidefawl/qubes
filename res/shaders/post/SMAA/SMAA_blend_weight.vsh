#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"
#pragma include "SMAA_common.glsl"
#define SMAA_INCLUDE_VS 1
#define SMAA_INCLUDE_PS 0
#pragma include "SMAA.hlsl"

out vec2 pass_texcoord;
out vec2 pixcoord;
out vec4 offset0;
out vec4 offset1;
out vec4 offset2;


void main(void) {
    pass_texcoord = in_texcoord.st;
    vec4 offsets[3];
    offsets[0] = vec4(0.0, 0.0, 0.0, 0.0);
    offsets[1] = vec4(0.0, 0.0, 0.0, 0.0);
    offsets[2] = vec4(0.0, 0.0, 0.0, 0.0);
    pixcoord = vec2(0.0, 0.0);
    SMAABlendingWeightCalculationVS(pass_texcoord, pixcoord, offsets);
    offset0 = offsets[0];
    offset1 = offsets[1];
    offset2 = offsets[2];
    vec4 pos = vec4(in_position.xyz, in_position.w);
    gl_Position = in_matrix_2D.mvp * pos;
}