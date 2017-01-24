#version 150 core

#pragma define "ALPHA_TEST"
#pragma define "SAMPLER_CONVERT_GAMMA"
#pragma include "tonemap.glsl"
#pragma include "ubo_scene.glsl"

uniform sampler2DArray texArray;
uniform int texSlot;


in vec4 pass_Color;
in vec2 pass_texcoord;
 
out vec4 out_Color;
 
void main(void) {
	ivec3 texSize = textureSize(texArray, 0);
	vec2 pixelSize = vec2(in_scene.viewport.xy)/vec2(texSize.xy);
	vec4 tex = texture(texArray, vec3(pass_texcoord*pixelSize, texSlot), 0);
	tex = vec4(vec3(tex.r), 1.0);


#ifdef ALPHA_TEST
    if (tex.a < 0.04)
    	discard;
#endif
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