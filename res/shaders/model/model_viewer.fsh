#version 150 core

#pragma include "tonemap.glsl"
#pragma include "blockinfo.glsl"

uniform sampler2D tex0;

in vec4 color;
in vec3 normal;
in vec4 texcoord;
in vec4 position;

out vec4 out_Color;
const vec3 lightPos = vec3(0, 30, 0);
#define LIGHT_CUTOFF 0.005
#define LIGHT_LIN 0.0012
#define LIGHT_EXP 0.0012

void main(void) {
	vec4 tex = texture(tex0, texcoord.st);
	// if (tex.a<1)
	// 	discard;
	 // tex = vec4(vec3(1),1);
	vec3 color_adj = tex.rgb;
	color_adj *= color.rgb;
	// srgbToLin(color_adj.rgb);

	vec3 lRay = position.xyz-lightPos;
	float fDist = length(lRay);
    float attenuation = 1.0 / (1 + LIGHT_LIN * fDist + LIGHT_EXP * fDist * fDist);
    attenuation = (attenuation - LIGHT_CUTOFF) / (1 - LIGHT_CUTOFF);
    attenuation = max(attenuation, 0);
   vec3 lDir = normalize(lRay);   
   float light = max(dot(normal, lDir), 0.0); 
   // light += max(dot(normal,vec3(0, -1, -1)), 0.0);
   vec3 Idiff = vec3(0.9, 0.8, 0.7) * max(attenuation*0.05, light) * max(0, attenuation);
	float alpha = tex.a*1;
	color_adj*=Idiff;
    out_Color = vec4(color_adj*0.01, alpha);
}