#version 150 core

#pragma define "NORMAL_MAPPING"
#pragma define "FAR_BLOCKFACE"
#pragma define "MODEL_RENDER"
#pragma include "ubo_scene.glsl"
#pragma include "ubo_constants.glsl"
#pragma include "tonemap.glsl"
#pragma include "util.glsl"
#pragma include "blockinfo.glsl"

uniform sampler2DArray blockTextures;
uniform sampler2DArray normalTextures;


flat in uvec4 blockinfo;
// flat in vec4 faceAO;
// flat in vec4 faceLight;
// flat in vec4 faceLightSky;

in vec4 color;
in vec3 normal;
in vec2 texcoord;
// in vec4 position;
// in vec2 light;
// in float camDistance;
// in vec2 texPos;
// in float roughness;

out vec4 out_Color;
// out vec4 out_Normal;
// out uvec4 out_Material;
// out vec4 out_Light;

void main(void) {
#ifdef MODEL_RENDER
	vec4 tex = vec4(vec3(1), 1);
#else
	vec4 tex=texture(blockTextures, vec3(texcoord.st, BLOCK_TEX_SLOT(blockinfo)));
#ifndef FAR_BLOCKFACE
#endif
 //!MODEL_RENDER
#endif
	if (tex.a<1)
		discard;
	vec3 color_adj = tex.rgb;
	vec3 color_adj2 = color.rgb;
	linearizeInput(color_adj.rgb);
	linearizeInput2(color_adj2.rgb);
	color_adj *= color_adj2.rgb;


    out_Color = vec4(color_adj, tex.a);
}
