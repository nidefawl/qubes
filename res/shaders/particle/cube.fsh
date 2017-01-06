#version 150 core

#pragma include "tonemap.glsl"
#pragma include "blockinfo.glsl"
#pragma include "ubo_constants.glsl"
#pragma define "PARTICLE_TYPE"

uniform sampler2DArray blockTextures;
uniform sampler2D noisetex;
uniform sampler2DArray normalTextures; 

in vec3 normal;
in vec4 color;
in vec4 texcoord;
in vec4 position;
flat in uint blockinfo32;
in float lightLevelSky;
in float lightLevelBlock;


out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;


void main(void) {

	vec4 tex=texture(blockTextures, vec3(texcoord.st, BLOCK_TEX_SLOT_u32(blockinfo32)));
	if (tex.a<1)
		discard;
	vec3 color_adj = tex.rgb;
	vec3 color_adj2 = color.rgb;
	srgbToLin(color_adj.rgb);
	srgbToLin(color_adj2.rgb);
	color_adj *= color_adj2.rgb;
	float alpha = tex.a*1;
	vec3 outNormal = normal;


	out_Color = vec4(color_adj, alpha);

	// float indexNormalMap = BLOCK_NORMAL_SLOT_u32(blockinfo32);
	// if (indexNormalMap > 0 && faceDir > 0u) { //figure out something better, this skips normal mapping for all non (greedy)meshed faces
	// 	mat3 tbnMat = mat3(matrix_tbn.mat[faceDir-1u]);

	// 	vec3 normalMapTex=texture(normalTextures, vec3(texcoord.st, indexNormalMap)).xzy * 2.0 - 1.0; // swizzling is important here
	// 	outNormal = normalize((tbnMat * normalMapTex));
	// }	

	out_Material = uvec4(blockinfo32 & 0xFFFFu, ( blockinfo32 >> 16u) & 0xFFFFu,0u,0u);
	out_Normal = vec4((outNormal) * 0.5f + 0.5f, 1f);
	out_Light = vec4(lightLevelSky, lightLevelBlock, 0.8, 1);
}