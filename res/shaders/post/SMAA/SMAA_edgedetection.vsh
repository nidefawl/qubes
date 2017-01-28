#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "SMAA_common.glsl"
#define SMAA_INCLUDE_VS 1
#define SMAA_INCLUDE_PS 0
#pragma include "SMAA.hlsl"

out vec2 pass_texcoord;
out vec4 offset0;
out vec4 offset1;
out vec4 offset2;


void main(void) {
	
    vec2 pos;
    pos.x = float(gl_VertexID & 1)*2.0;
    pos.y = float(gl_VertexID & 2);
    pass_texcoord.st = pos;
    gl_Position = vec4(pos * 2.0 - 1.0, 0, 1);

    vec4 offsets[3];
    offsets[0] = vec4(0.0, 0.0, 0.0, 0.0);
    offsets[1] = vec4(0.0, 0.0, 0.0, 0.0);
    offsets[2] = vec4(0.0, 0.0, 0.0, 0.0);
    SMAAEdgeDetectionVS(pass_texcoord, offsets);
    offset0 = offsets[0];
    offset1 = offsets[1];
    offset2 = offsets[2];
}