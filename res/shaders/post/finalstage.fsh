#version 150 core

#pragma define "DO_AUTOEXPOSURE"
#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"

uniform sampler2D texColor;
#ifdef DO_AUTOEXPOSURE
uniform sampler2D texLum;
const float constexposure = 130;
#else
uniform float constexposure;
#endif


in vec2 pass_texcoord;

out vec4 out_Color;
void main(void) {
	vec4 tex = texture(texColor, pass_texcoord.st, 0);
#ifdef DO_AUTOEXPOSURE
	float brightness = 1-pow(1-texelFetch(texLum, ivec2(0,0), 0).r, 1.4);
	float fDyn = 0.7;
	float autoExposure = ((brightness-fDyn)) * -constexposure;
	autoExposure = constexposure*(1.0-fDyn) + autoExposure;
	autoExposure = clamp(autoExposure, 10, 160);
	vec3 toneMapped = ToneMap(tex.rgb, autoExposure);
#else 
	vec3 toneMapped = ToneMap(tex.rgb, constexposure);
#endif
	out_Color = vec4(toneMapped, 1);
}