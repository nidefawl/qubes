#version 450

layout (location = 0) in vec4 pass_Color;
flat in uvec4 dummy;

layout (location = 0) out vec4 out_Color;
 
void main(void) {
	vec3 color_adj = vec3(0.0);
	if (dummy.r == 0u)
		color_adj.r = 1.0;
	if (dummy.g == 1u)
		color_adj.g = 1.0;
	if (dummy.b == 2u)
		color_adj.b = 1.0;
	if (dummy.a == 3u) 
		color_adj.rgb = vec3(0.5);
    out_Color = vec4(color_adj.rgb, 1.0);
}