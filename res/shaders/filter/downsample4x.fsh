#version 150 core

#pragma define "LUMINANCE"
#pragma include "tonemap.glsl"

uniform sampler2D texColor;

in vec2 TexCoord1;
in vec2 TexCoord2;
in vec2 TexCoord3;
in vec2 TexCoord4;
 
out vec4 out_Color;
 
void main(void) {
	vec4 sample0 = texture(texColor, TexCoord1, 0);
	vec4 sample1 = texture(texColor, TexCoord2, 0);
	vec4 sample2 = texture(texColor, TexCoord3, 0);
	vec4 sample3 = texture(texColor, TexCoord4, 0);
#ifdef LUMINANCE
	vec3 LUMINANCE_VECTOR  = vec3(0.2125, 0.7154, 0.0721);
	vec3 sample = (sample0.rgb+sample1.rgb+sample2.rgb+sample3.rgb)*0.25;
	sample = ToneMap(sample, 16);
	vec3 lum = vec3(max(dot(sample, LUMINANCE_VECTOR), 0));
    out_Color = vec4(lum, 1);
#else

    out_Color = (sample0+sample1+sample2+sample3)*0.25;
#endif
}
