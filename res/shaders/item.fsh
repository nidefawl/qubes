#version 150 core

#pragma define "ALPHA_TEST"

uniform sampler2DArray itemTextures;


in vec4 color;
in vec4 texcoord;
flat in uint idx;
 
out vec4 out_Color;
 
void main(void) {
	vec4 tex = texture(itemTextures, vec3(texcoord.st, 0));
    if (tex.a < 1.0)
    	discard;
    out_Color = tex*color;
}