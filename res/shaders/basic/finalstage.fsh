#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"

uniform sampler2D texColor;
uniform sampler2D texBlur;

uniform float near;
uniform float far;

in vec2 pass_texcoord;

out vec4 out_Color;

void main(void) {
	vec4 tex = texture(texColor, pass_texcoord.st, 0);
	vec4 texBloom = texture(texBlur, pass_texcoord.st, 0);
	out_Color = vec4(ToneMap(tex.rgb+texBloom.rgb*0.1f), tex.a);
}