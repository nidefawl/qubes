#version 150 core

layout (set = 1, binding = 0) uniform sampler2D tex0;

in vec2 pass_texcoord;
 
out vec4 out_Color;
 
void main(void) {
	vec4 tex = texture(tex0, pass_texcoord.st, 0);
    if (tex.a < 1.0)
    	discard;
    out_Color = vec4(1.0);
}