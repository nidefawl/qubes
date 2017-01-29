#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "SMAA_common.glsl"
#define SMAA_INCLUDE_VS 1
#define SMAA_INCLUDE_PS 0
#pragma include "SMAA.hlsl"

out vec2 pass_texcoord;
out vec4 offset;


void main(void) {
	
#define TRI_WINDING 0
#pragma include "fullscreen_triangle_vertex.glsl"

    offset = vec4(0.0, 0.0, 0.0, 0.0);
    SMAANeighborhoodBlendingVS(pass_texcoord, offset);
}