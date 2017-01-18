#version 150 core

#pragma include "tonemap.glsl"
#pragma include "blockinfo.glsl"
#pragma include "ubo_constants.glsl"
#define ATTR_SSBO_STRUCT 0
#define ATTR_VERTEX_ATTR 1
#pragma define "ATTR_MODE"




in vec3 normal;
in vec4 texcoord;
in vec4 position;
in vec3 color;




out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;


void main(void) {

	vec3 color_adj = color;
	srgbToLin(color_adj.rgb);
	float alpha = 1;
	vec3 outNormal = normal;


	out_Color = vec4(color_adj, alpha);
	uint renderData = 0u;
	renderData = ENCODE_RENDERPASS(5);
	out_Material = uvec4(0u,0u+renderData,0u,1u);
	out_Normal = vec4((outNormal) * 0.5f + 0.5f, 1f);
	out_Light = vec4(0.16, 0.01, 0.8, 1);
}