#version 150 core

#pragma define "DO_BLOOM"
#pragma include "ubo_scene.glsl"

uniform sampler2D texColor;
#ifdef DO_BLOOM
uniform sampler2D texBlur;
#endif


in vec2 pass_texcoord;

out vec4 out_Color;

void main(void) {
	vec4 tex = texture(texColor, pass_texcoord.st, 0);
#ifdef DO_BLOOM
	vec4 texBloom = texture(texBlur, pass_texcoord.st, 0);
	out_Color = vec4(tex.rgb+texBloom.rgb*0.4, 1);
#else
	out_Color = tex;
#endif
}