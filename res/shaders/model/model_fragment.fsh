#version 150 core

#pragma include "tonemap.glsl"
#pragma include "blockinfo.glsl"
#pragma include "ubo_scene.glsl"
#pragma define "RENDERER"
#define RENDERER_WORLD_MAIN 0
#define RENDERER_WORLD_SHADOW 1
#define RENDERER_MODELVIEWER 2
#define RENDERER_SCREEN 3

uniform sampler2D tex0;
in vec4 pass_color;
in vec4 pass_normal;
in vec4 pass_texcoord;
in vec4 pass_position;
out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;
#if RENDERER >= RENDERER_MODELVIEWER
const vec3 lightPos = 1*vec3(-1, -8.1, 3);
const vec3 lightPos2 = -1*vec3(-7, -2.1, -1);


void main(void) {
	vec4 tex = texture(tex0, pass_texcoord.st);
	// if (tex.a<1)
	// 	discard;
	 // tex = vec4(vec3(1),1);
	vec3 color_adj = tex.rgb;
	color_adj *= pass_color.rgb;
	srgbToLin(color_adj.rgb);

    vec3 n1 = normalize(pass_normal.xyz);
    float attenuation = 0.001;
	vec3 lDir1 = -normalize(lightPos2);  
	vec3 lDir2 = -normalize(lightPos);    
	vec3 Idiff = vec3(0);

	vec3 lDir;
	for (int i = 0; i < 2; i++) {
		lDir = i == 0 ? lDir1 : lDir2;
		float light = 0;
		light+=max(dot(n1, lDir), 0.0); 
		vec3 R = normalize(-reflect(lDir,n1)); 
		vec3 E = normalize(CAMERA_POS-pass_position.xyz);
		float spec = pow(max(dot(R,E),0.0),2.3)*3;
		light += spec;
		Idiff += vec3(0.9, 0.8, 0.7) * max(0.1, light) * max(0, attenuation) * 1500; 
	}
	float alpha = tex.a*1;
	color_adj*=Idiff;
#if RENDERER == RENDERER_MODELVIEWER
    out_Color = vec4(color_adj*0.1, alpha);
    out_Normal = vec4((n1) * 0.5f + 0.5f, 0.05f);
    out_Material = uvec4(0u,1u+ENCODE_RENDERPASS(5),0u,1u);
    out_Light = vec4(1, 0,  1, 1);
#else
	vec3 toneMapped = ToneMap(color_adj*0.1, 130);
    out_Color = vec4(toneMapped, alpha);
#endif
}
#else
#if RENDERER == RENDERER_WORLD_SHADOW
void main(void) {
  out_Color = vec4(0, 0, 0, 1);
}
#else

 
void main(void) {
	vec4 tex = texture(tex0, pass_texcoord.st);
	// if (tex.a<1)
	// 	discard;
	 // tex = vec4(vec3(1),1);
	vec3 color_adj = tex.rgb;
	color_adj *= pass_color.rgb;
	srgbToLin(color_adj.rgb);

	float alpha = tex.a*1;
    out_Color = vec4(color_adj, alpha);
    out_Normal = vec4((pass_normal.xyz) * 0.5f + 0.5f, 0.05f);
    out_Material = uvec4(0u,1u+ENCODE_RENDERPASS(5),0u,1u);
    out_Light = vec4(1, 0,  1, 1);
}

#endif
#endif