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
	// if (tex.r > 0)
	// 	tex = vec4(1.0);
	// else 
	// 	tex = vec4(0.0);
#ifdef SAMPLER_LIN_TO_SRGB
	// vec3 color_adj = tex.rgb;
	// vec3 color_adj2 = pass_Color.rgb;
	// color_adj *= color_adj2.rgb;
	// linToSrgb(color_adj.rgb);
	// out_Color = vec4(color_adj.rgb, tex.a);
	linToSrgb(tex.rgb);
	out_Color = vec4(tex.rgb, tex.a);
#elif defined SAMPLER_SRGB_TO_LIN
	// vec3 color_adj = tex.rgb;
	// vec3 color_adj2 = pass_Color.rgb;
	// srgbToLin(color_adj.rgb);
	// srgbToLin(color_adj2.rgb);
	// color_adj *= color_adj2.rgb;
	// out_Color = vec4(color_adj.rgb, tex.a);
	srgbToLin(tex.rgb);
	out_Color = vec4(tex.rgb, tex.a);
#else
    out_Color = tex*pass_Color;
#endif
}