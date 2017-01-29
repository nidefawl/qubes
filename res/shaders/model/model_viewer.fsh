#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"
#pragma include "blockinfo.glsl"

uniform sampler2D tex0;

in vec4 pass_color;
in vec4 pass_normal;
in vec4 pass_texcoord;
in vec4 pass_position;

out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;

#define MODEL_SPEC_ROUGHNESS 0.9f


void main(void) {
	vec4 tex = texture(tex0, pass_texcoord.st);
	// if (tex.a<1)
	// 	discard;
	 // tex = vec4(vec3(1),1);
	vec4 color_adj1 = tex;
	vec4 color_adj2 = pass_color;
	srgbToLin(color_adj1.rgb);
	srgbToLin(color_adj2.rgb);
	vec4 color_adj = color_adj1*color_adj2;
	
	float alpha = tex.a*1;
    out_Color = vec4(color_adj.rgb, alpha);
    out_Normal = vec4(pass_normal.xyz * 0.5f + 0.5f, MODEL_SPEC_ROUGHNESS);
    out_Material = uvec4(0u,1u+ENCODE_RENDERPASS(5),0u,0u);
    out_Light = vec4(1, 0,  1, 1);
}