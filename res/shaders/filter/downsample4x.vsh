#version 150 core

out vec2 TexCoord1;
out vec2 TexCoord2;
out vec2 TexCoord3;
out vec2 TexCoord4;

#ifdef VULKAN_GLSL
	layout(push_constant) uniform PushConstantsDownsample4 {
	  vec2 twoTexelSize;
	} pushCDownsample4;
	#define TWO_TEXEL pushCDownsample4.twoTexelSize
#else
	uniform vec2 twoTexelSize;
	#define TWO_TEXEL twoTexelSize
#endif

void main(void) {
	vec2 pass_texcoord;
#pragma include "fullscreen_triangle_vertex.glsl"
	TexCoord1 = pass_texcoord.st;
	TexCoord2 = pass_texcoord.st + vec2(TWO_TEXEL.x, 0);
	TexCoord3 = pass_texcoord.st + vec2(TWO_TEXEL.x, TWO_TEXEL.y);
	TexCoord4 = pass_texcoord.st + vec2(0, TWO_TEXEL.y);
}

