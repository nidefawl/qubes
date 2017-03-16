#version 150 core

#pragma define "ALPHA_TEST"

uniform sampler2DArray itemTextures;


in vec4 color;
in vec2 texcoord;
flat in uint idx;
 
out vec4 out_Color;
 
void main(void) {
	vec4 tex = textureLod(itemTextures, vec3(texcoord.st, idx), 0);
	// vec4 tex = texture(itemTextures, vec3(texcoord.st, idx));
    if (tex.a < 0.0)
    	discard;
    out_Color = vec4(tex);
}