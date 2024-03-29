#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "blockinfo.glsl"
#pragma include "tonemap.glsl"


uniform sampler2DArray blockTextures;
uniform sampler2DArray normalTextures; 

in vec3 normal;
in vec4 color;
in vec2 texcoord;
flat in uvec4 blockinfo;

out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;
 
 
void main(void) {
	vec4 tex=texture(blockTextures, vec3(texcoord.st, BLOCK_TEX_SLOT(blockinfo)), -100);
	if (tex.a<1.0)
		discard;
	vec3 color_adj = tex.rgb;
	vec3 color_adj2 = color.rgb;
	linearizeInput(color_adj.rgb);
	linearizeInput2(color_adj2.rgb);
	color_adj *= color_adj2.rgb;
	out_Color = vec4(color_adj.rgb, tex.a);
    out_Normal = vec4((normal) * 0.5f + 0.5f, 1.0f);
    out_Material = uvec4(0u,1u+ENCODE_RENDERPASS(2),0u,1u);
    out_Light = vec4(0.0f, 0.1f, 1.0f, 1.0f);
}