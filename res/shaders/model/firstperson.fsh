#version 150 core

#pragma include "tonemap.glsl"
#pragma include "blockinfo.glsl"

uniform sampler2D tex0;

in vec4 color;
in vec3 normal;
in vec4 texcoord;
in vec4 position;

out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;
 
void main(void) {
	vec4 tex = texture(tex0, texcoord.st);
	// if (tex.a<1)
	// 	discard;
	 // tex = vec4(vec3(1),1);
	vec3 color_adj = tex.rgb;
	color_adj *= color.rgb;
	srgbToLin(color_adj.rgb);

	float alpha = tex.a*1;
    out_Color = vec4(color_adj*0.1, alpha);
    out_Normal = vec4((normal) * 0.5f + 0.5f, 0.05f);
    out_Material = uvec4(0u,1u+ENCODE_RENDERPASS(2),0u,1u);
    out_Light = vec4(1, 0,  1, 1);
}