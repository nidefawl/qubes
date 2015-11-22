#version 150 core


#pragma define "FAR_BLOCKFACE"
#pragma define "MODEL_RENDER"
#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"
#pragma include "util.glsl"
#pragma include "blockinfo.glsl"

uniform sampler2DArray blockTextures;


in vec4 texcoord;
in vec4 position;
flat in uvec4 blockinfo;


out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;

void main(void) {
	vec4 tex=texture(blockTextures, vec3(texcoord.st, BLOCK_TEX_SLOT(blockinfo)));
	if (tex.a<1)
		discard;
    out_Color = vec4(vec3(1), 1);
    out_Normal = vec4(0);
    out_Material = uvec4(0);
    out_Light = vec4(0);
}
