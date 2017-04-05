#version 150 core

#pragma include "SMAA_common.glsl"
#define SMAA_INCLUDE_VS 0
#define SMAA_INCLUDE_PS 1
#pragma include "SMAA.hlsl"

layout (set = 1, binding = 0) uniform sampler2D edgesTex;
layout (set = 1, binding = 1) uniform sampler2D areaTex;
layout (set = 1, binding = 2) uniform sampler2D searchTex;

#if SMAA_REPROJECTION
	#ifdef VULKAN_GLSL
		layout(push_constant) uniform PushConstsBlendWeight {
		  vec4 jitterOffset;
		} pushCBlendWeight;
		#define JITTTER_OFFS pushCBlendWeight.jitterOffset
	#else
		//OpenGL 
		uniform vec4 jitterOffset;
		#define JITTTER_OFFS jitterOffset
	#endif
#else
	const vec4 jitterOffset = vec4(0);
	#define JITTTER_OFFS jitterOffset
#endif



in vec2 pass_texcoord;
in vec2 pixcoord;
in vec4 offset0;
in vec4 offset1;
in vec4 offset2;
 
out vec4 out_Color;


void main(void) {
    vec4 offsets[3];
    offsets[0] = offset0;
    offsets[1] = offset1;
    offsets[2] = offset2;
    out_Color = SMAABlendingWeightCalculationPS(pass_texcoord, pixcoord, offsets, edgesTex, areaTex, searchTex, JITTTER_OFFS);
    // gl_FragDepth = gl_FragCoord.z+1;
}