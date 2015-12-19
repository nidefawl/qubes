#version 150 core

#pragma include "tonemap.glsl"

in vec4 pass_Color;

out vec4 out_Color;
 
void main(void) {
	vec4 color = pass_Color;
	srgbToLin(color.rgb);
    out_Color = color*0.1;
}