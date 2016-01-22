#version 150 core

#pragma include "tonemap.glsl"
#pragma include "blockinfo.glsl"

uniform sampler2D tex0;

in vec3 normal;
in vec3 color;
in vec4 texcoord;
in vec4 position;

out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
const vec3 lightPos = vec3(0, 30, 0);
#define LIGHT_CUTOFF 0.0001
#define LIGHT_LIN 0.00072
#define LIGHT_EXP 0.000032
const vec3 fogColor=vec3(0.54f, 0.74f, 0.96f)*1.1f;


void main(void) {
	vec4 tex = texture(tex0, texcoord.st);
	// tex = vec4(vec3(1),1);
	// vec3 color_adj = tex.rgb;
	// // color_adj *= color.rgb;
	// // srgbToLin(color_adj.rgb);

	// vec3 lRay = position.xyz-lightPos;
	// float fDist = length(lRay);
	// float attenuation = 1.0 / (1 + LIGHT_LIN * fDist + LIGHT_EXP * fDist * fDist);
	// attenuation = (attenuation - LIGHT_CUTOFF) / (1 - LIGHT_CUTOFF);
	// attenuation = max(attenuation, 0);
	// vec3 lDir = normalize(lRay);   
	// float light = max(dot(normal, lDir), 0.01); 
	// // light += max(dot(normal, lDir), 0.0); 
	// // light += max(dot(normal,vec3(0, -1, -1)), 0.0);
	// vec3 Idiff = vec3(0.9, 0.8, 0.7) * max(attenuation*0.05, light) * max(0.01, attenuation);
	// // color_adj*=Idiff;
	// out_Color = vec4((normal*0.5+0.5)*0.001, alpha);
	// if (tex.a > 1)
	// out_Color = vec4(1,0,0,1);
	// else out_Color=vec4(0,1,0,1);
  float dist = length(position);
  float fogFactor = clamp( (dist - 20.0f) / 150.0f, 0.0f, 0.5f );
  // fogFactor += clamp( (dist - 20.0f) / 420.0f, 0.0f, 0.06f );
  vec3 skycolor = mix(color, fogColor, fogFactor);
  out_Color = vec4(skycolor, clamp(tex.a*(1-fogFactor), 0, 1));
  out_Normal = vec4(0.5);
  out_Material = uvec4(0);
}