#version 150 core

#pragma include "SMAA_common.glsl"
#define SMAA_INCLUDE_VS 1
#define SMAA_INCLUDE_PS 0
#pragma include "SMAA.hlsl"

out vec2 pass_texcoord;
out vec4 offset;


void main(void) {
	
#ifdef VULKAN_GLSL
#define TRI_WINDING 1
#else
#define TRI_WINDING 0
#endif
#pragma include "fullscreen_triangle_vertex.glsl"

    offset = vec4(0.0, 0.0, 0.0, 0.0);
    SMAANeighborhoodBlendingVS(pass_texcoord, offset);
}