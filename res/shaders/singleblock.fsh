#version 150 core

#pragma include "util.glsl"
#pragma include "blockinfo.glsl"
#pragma include "ubo_constants.glsl"
#pragma include "tonemap.glsl"


uniform sampler2DArray blockTextures;


in vec4 color;
in vec3 normal;
in vec2 texcoord;
in vec2 texPos;
in float Idiff;
flat in uvec4 blockinfo;

out vec4 out_Color;
 
void main(void) {
	vec4 tex=texture(blockTextures, vec3(texcoord.st, BLOCK_TEX_SLOT(blockinfo)), -100);
	if (tex.a<1.0)
		discard;
	vec3 color_adj = tex.rgb;
	vec3 color_adj2 = color.rgb;
	linearizeInput(color_adj.rgb);
	linearizeInput2(color_adj2.rgb);
	color_adj *= color_adj2.rgb;
	color_adj *= Idiff * 1.8;
	vec3 toneMapped = ToneMap(color_adj, 2.0f);
	out_Color = vec4(toneMapped, tex.a);
}