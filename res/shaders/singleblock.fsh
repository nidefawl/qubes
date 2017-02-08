#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "util.glsl"
#pragma include "blockinfo.glsl"
#pragma include "tonemap.glsl"


uniform sampler2DArray blockTextures;
uniform sampler2D noisetex;


in vec4 color;
in vec3 normal;
in vec4 texcoord;
in vec2 texPos;
flat in uvec4 blockinfo;
in float Idiff;

out vec4 out_Color;
 
void main(void) {
	vec4 tex=texture(blockTextures, vec3(texcoord.st, BLOCK_TEX_SLOT(blockinfo)), -100);
	if (tex.a<1.0)
		discard;
	vec3 color_adj = tex.rgb;
	vec3 color_adj2 = color.rgb;
	// srgbToLin(color_adj.rgb);
	srgbToLin(color_adj2.rgb);
	color_adj *= color_adj2.rgb;
	color_adj *= Idiff * 1.8;
	vec3 toneMapped = ToneMap(color_adj, 2.0f);
	out_Color = vec4(toneMapped, tex.a);
}