#version 150 core


layout (set = 0, binding = 0) uniform sampler2D texPrev;
layout (set = 1, binding = 0) uniform sampler2D texNew;

#ifdef VULKAN_GLSL
	layout(push_constant) uniform PushConstantsLumInterp {
	  float elapsedTime;
	} pushCLumInterp;
	#define ELAPSED_TIME pushCLumInterp.elapsedTime
#else
	uniform float elapsedTime;
	#define ELAPSED_TIME elapsedTime
#endif

out float lum;

void main(void) {
	float prevLum = texelFetch(texPrev, ivec2(0,0), 0).r;
	float curLum = texelFetch(texNew, ivec2(0,0), 0).r;
	float newLum = prevLum + (curLum - prevLum) * ( 1.0 - pow( 0.98f, 30.0 * ELAPSED_TIME ) );
	lum = newLum;
	
	vec2 pass_texcoord;
#pragma include "fullscreen_triangle_vertex.glsl"
}