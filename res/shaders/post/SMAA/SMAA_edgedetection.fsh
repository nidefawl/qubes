#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "SMAA_common.glsl"
#define SMAA_INCLUDE_VS 0
#define SMAA_INCLUDE_PS 1
#pragma include "SMAA.hlsl"

uniform sampler2D texColor;

in vec2 pass_texcoord;
in vec4 offset0;
in vec4 offset1;
in vec4 offset2;
 
out vec4 out_Color;


void main(void) {
    vec4 offsets[3];
    offsets[0] = offset0;
    offsets[1] = offset1;
    offsets[2] = offset2;
	out_Color = vec4(SMAAColorEdgeDetectionPS(pass_texcoord, offsets, texColor), 0.0, 0.0);
}