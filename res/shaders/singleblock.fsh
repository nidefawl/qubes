#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "util.glsl"
#pragma include "blockinfo.glsl"


uniform sampler2DArray blockTextures;
uniform sampler2D noisetex;


in vec4 color;
in vec3 normal;
in vec4 texcoord;
in vec4 position;
// in vec2 light;
// flat in vec4 faceAO;
// flat in vec4 faceLight;
// flat in vec4 faceLightSky;
in float blockside;

in vec2 texPos;
flat in uvec4 blockinfo;


out vec4 out_Color;
 
void main(void) {
	vec4 tex=texture(blockTextures, vec3(texcoord.st, BLOCK_TEX_SLOT(blockinfo)));
	if (tex.a<1)
		discard;
	vec3 color_adj = tex.rgb;
	color_adj *= color.rgb;
	//MINECRAFTISH BLOCK LIGHTING
	color_adj *= blockside;


	float f = BLOCK_ID(blockinfo);

	//TODO: figure out something better 
	if (IS_LEAVES(f)) { //EXPENSIVE LEAVE
		colorizeLeaves(color_adj, position.xyz);
	}

	float alpha = tex.a*1;
    out_Color = vec4(color_adj, alpha);
}