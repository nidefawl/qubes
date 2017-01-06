#version 150 core

#pragma include "tonemap.glsl"
#pragma include "blockinfo.glsl"
#pragma include "ubo_constants.glsl"
#pragma define "PARTICLE_TYPE"


in vec3 normal;
in vec4 texcoord;
in vec4 position;

#ifdef PARTICLE_TYPE_BLOCK
uniform sampler2DArray blockTextures;
uniform sampler2D noisetex;
uniform sampler2DArray normalTextures; // needs to be another array later one (I guess)
flat in uint blockinfo32;
#else

uniform sampler2D tex0;

#endif


out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;


void main(void) {

#ifdef PARTICLE_TYPE_BLOCK
	vec4 tex=texture(blockTextures, vec3(texcoord.st, BLOCK_TEX_SLOT_u32(blockinfo32)));
	if (tex.a<1)
		discard;
#else
	vec4 tex = texture(tex0, texcoord.st);
#endif
	vec3 color_adj = tex.rgb;
	srgbToLin(color_adj.rgb);
	float alpha = tex.a*1;
	vec3 outNormal = normal;


	out_Color = vec4(color_adj, alpha);
#ifdef PARTICLE_TYPE_BLOCK


	// float indexNormalMap = BLOCK_NORMAL_SLOT_u32(blockinfo32);
	// if (indexNormalMap > 0 && faceDir > 0u) { //figure out something better, this skips normal mapping for all non (greedy)meshed faces
	// 	mat3 tbnMat = mat3(matrix_tbn.mat[faceDir-1u]);

	// 	vec3 normalMapTex=texture(normalTextures, vec3(texcoord.st, indexNormalMap)).xzy * 2.0 - 1.0; // swizzling is important here
	// 	outNormal = normalize((tbnMat * normalMapTex));
	// }	

	out_Material = uvec4(blockinfo32 & 0xFFFFu, ( blockinfo32 >> 16u) & 0xFFFFu,0u,0u);
#else
	uint renderData = 0u;
	renderData = ENCODE_RENDERPASS(5);
	out_Material = uvec4(0u,0u+renderData,0u,1u);
#endif
	out_Normal = vec4((outNormal) * 0.5f + 0.5f, 1f);
	out_Light = vec4(0.16, 0.01, 0.8, 1);
}