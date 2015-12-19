#version 150 core

#pragma define "DO_BLOOM"
#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"

uniform sampler2D texColor;
uniform sampler2D texLum;

uniform float exposure;

in vec2 pass_texcoord;

out vec4 out_Color;

void main(void) {
	vec4 tex = texture(texColor, pass_texcoord.st, 0);
	float brightness = texelFetch(texLum, ivec2(0,0), 0).r;
	float exposure = 100;
	float autoExposure = ((brightness-0.7f)) * -exposure;
	autoExposure = exposure*0.3f + autoExposure ;
	autoExposure = clamp(autoExposure, 10, 160);
	vec3 toneMapped = ToneMap(tex.rgb, autoExposure);
	out_Color = vec4(toneMapped, 1);
}