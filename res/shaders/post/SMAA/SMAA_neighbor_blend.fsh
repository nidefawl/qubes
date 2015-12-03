#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "SMAA_common.glsl"
#define SMAA_INCLUDE_VS 0
#define SMAA_INCLUDE_PS 1
#pragma include "SMAA.hlsl"

uniform sampler2D texColor;
uniform sampler2D blendTex;

in vec2 pass_texcoord;
in vec4 offset;
 
out vec4 out_Color;


void main(void) {
	out_Color = SMAANeighborhoodBlendingPS(pass_texcoord, offset, texColor, blendTex);
}