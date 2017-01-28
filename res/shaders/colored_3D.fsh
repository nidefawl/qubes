#version 150 core

#pragma include "tonemap.glsl"

in vec4 pass_Color;

out vec4 out_Color;

uniform float color_brightness;
uniform vec4 color_uniform;

void main(void) {
	vec4 color = pass_Color;
	srgbToLin(color.rgb);
    out_Color = color*color_brightness*color_uniform;
}