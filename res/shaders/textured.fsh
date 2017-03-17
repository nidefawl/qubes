#version 150 core

#pragma define "ALPHA_TEST"
#pragma define "SAMPLER_CONVERT_GAMMA"
#pragma include "tonemap.glsl"

uniform sampler2D tex0;

in vec4 pass_Color;
in vec2 pass_texcoord;
 
out vec4 out_Color;
 
void main(void) {
	vec4 tex = texture(tex0, pass_texcoord.st, 0);
#ifdef ALPHA_TEST
    if (tex.a < 0.04)
    	discard;
#endif
#ifdef SAMPLER_LIN_TO_SRGB
	linToSrgb(tex.rgb);
#elif defined SAMPLER_SRGB_TO_LIN
	srgbToLin(tex.rgb);
#endif
    out_Color = tex*pass_Color;
}