#version 150 core

#pragma include "SMAA_common.glsl"
#define SMAA_INCLUDE_VS 0
#define SMAA_INCLUDE_PS 1
#pragma include "SMAA.hlsl"

layout (set = 0, binding = 0) uniform sampler2D texColor;
layout (set = 1, binding = 0) uniform sampler2D blendTex;
#if SMAA_REPROJECTION
layout (set = 2, binding = 0) uniform sampler2D velocityTex;
#endif

in vec2 pass_texcoord;
in vec4 offset;
 
out vec4 out_Color;


void main(void) {
#if SMAA_REPROJECTION
	out_Color = SMAANeighborhoodBlendingPS(pass_texcoord, offset, texColor, blendTex, velocityTex);
// out_Color=vec4(texture(blendTex, pass_texcoord).rgb, 1.0);
#else
	out_Color = SMAANeighborhoodBlendingPS(pass_texcoord, offset, texColor, blendTex);
#endif
// out_Color=texture(texColor, pass_texcoord);
}