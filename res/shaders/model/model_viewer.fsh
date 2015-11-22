#version 150 core

#pragma include "tonemap.glsl"
#pragma include "blockinfo.glsl"

uniform sampler2D tex0;

in vec4 color;
in vec3 normal;
in vec4 texcoord;
in vec4 position;

out vec4 out_Color;
const vec3 lightPos = normalize(vec3(0, -1122, 440));
void main(void) {
	vec4 tex = texture(tex0, texcoord.st);
	// if (tex.a<1)
	// 	discard;
	 // tex = vec4(vec3(1),1);
	vec3 color_adj = tex.rgb;
	color_adj *= color.rgb;
	// srgbToLin(color_adj.rgb);

   vec3 L = normalize(lightPos);   
   vec3 Idiff = vec3(0.9, 0.8, 0.7) * max(dot(normal,L), 0.0);  
   Idiff = clamp(Idiff, vec3(0.4), vec3(1.3))*4; 
	float alpha = tex.a*1;
    out_Color = vec4(color_adj*Idiff, alpha);
}