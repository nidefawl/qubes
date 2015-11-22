#version 150 core

#pragma define "ALPHA_TEST"

uniform sampler2D tex0;

in vec4 pass_Color;
in vec2 pass_texcoord;
 
out vec4 out_Color;
 
void main(void) {
	vec4 tex = texture(tex0, pass_texcoord.st);
// #ifdef ALPHA_TEST
//     if (tex.a < 1.0)
//     	discard;
// #endif
    out_Color = tex*pass_Color;
}