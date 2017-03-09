#version 150 core

#pragma define "ALPHA_TEST"
#pragma define "EXPLICIT_LOD"
#pragma include "tonemap.glsl"

uniform sampler2D tex0;
uniform float color_brightness;

in vec4 pass_Color;
in vec2 pass_texcoord;
 
out vec4 out_Color;
 
void main(void) {
	#ifndef EXPLICIT_LOD
		vec4 tex = texture(tex0, pass_texcoord.st);
	#else
		vec4 tex = textureLod(tex0, pass_texcoord.st, EXPLICIT_LOD);
	#endif
#ifdef ALPHA_TEST
    if (tex.a < 1.0)
    	discard;
#endif
	vec4 color = tex*pass_Color;
	linearizeInput(color.rgb);
    out_Color = vec4(color.rgb*color_brightness, tex.a*pass_Color.a);
}
