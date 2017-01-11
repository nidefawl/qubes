#version 150 core

#pragma include "tonemap.glsl"
#pragma include "blockinfo.glsl"

#pragma define "ALPHA_TEST"

uniform sampler2D tex0;

in vec3 pass_normal;
in vec2 pass_texcoord;
 
out vec4 out_Color;
 
void main(void) {
	vec4 tex = texture(tex0, pass_texcoord.st);
#ifdef ALPHA_TEST
    if (tex.a < 1.0)
    	discard;
#endif
	vec4 color = tex;
	srgbToLin(color.rgb);
    out_Color = vec4(color.rgb*4, tex.a);
}