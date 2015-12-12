#version 150 core

#pragma define "DO_BLOOM"
#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"

uniform sampler2D texColor;
#ifdef DO_BLOOM
uniform sampler2D texBlur;
uniform sampler2D texLum;
#endif

uniform float exposure;

in vec2 pass_texcoord;

out vec4 out_Color;

void main(void) {
	vec4 tex = texture(texColor, pass_texcoord.st, 0);
#ifdef DO_BLOOM
	vec4 texBloom = texture(texBlur, pass_texcoord.st, 0);
	float brightness = texelFetch(texLum, ivec2(0,0), 0).r;
   float exposure = 100;
   float autoExposure = ((brightness-0.7f)) * -exposure;
   autoExposure = exposure*0.3f + autoExposure ;
   autoExposure = clamp(autoExposure, 10, 160);
	vec3 toneMapped = ToneMap(tex.rgb+texBloom.rgb*0.4, autoExposure);

#else
	vec3 toneMapped = ToneMap(tex.rgb, 32.0f);
#endif
	out_Color = vec4(toneMapped, tex.a);
	// out_Color = vec4(tex.rgb, tex.a);
}