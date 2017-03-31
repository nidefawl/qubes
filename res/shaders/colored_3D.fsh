#version 150 core

#pragma include "tonemap.glsl"

in vec4 pass_Color;

out vec4 out_Color;

#ifdef VULKAN_GLSL
layout(push_constant) uniform PushConstantsColored3D {
  	vec4 color_uniform;
	float color_brightness;
} pushCColored3D;
#define BR pushCColored3D.color_brightness
#define COLOR pushCColored3D.color_uniform
#else
uniform float color_brightness;
uniform vec4 color_uniform;
#define BR color_brightness
#define COLOR color_uniform
#endif

void main(void) {
	vec4 color = pass_Color;
	linearizeInput2(color.rgb);
    out_Color = color*BR*COLOR;
}