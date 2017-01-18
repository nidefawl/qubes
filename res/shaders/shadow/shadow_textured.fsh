#version 150 core

#pragma include "blockinfo.glsl"

uniform sampler2DArray blockTextures;

in vec2 texcoord;
flat in uvec4 blockinfo;

out vec4 out_Color;

void main() {
	/*vec4 tex=texture(blockTextures, vec3(texcoord.st, BLOCK_TEX_SLOT(blockinfo)), -4); //ULTRA SLOW
	if (tex.a<1.0)
		discard;*/
	vec4 tex=texture(blockTextures, vec3(texcoord.st, BLOCK_TEX_SLOT(blockinfo)));
	if (tex.a<1.0)
		discard;
    out_Color = vec4(vec3(0), 1.0);
}
