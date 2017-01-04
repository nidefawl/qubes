#version 150 core

#pragma include "tonemap.glsl"
#pragma include "blockinfo.glsl"

uniform sampler2D tex0;
uniform float transparency;
uniform float spritebrightness;

in vec3 normal;
in vec4 color;
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
  float dist = length(position);
  // float fogFactor = clamp( (dist - 220.0f) / 150.0f, 0.0f, 0.5f );
    float hM = clamp(position.y/1220.0, 0.0002, 0.25);//+clamp((position.y-180)/80.0, 0.0, 1.0)*3;
   // float fogAmount = clamp(1.0 - exp( -dist*0.00001*222 ), 0.0, 1.0);
  // vec3 skycolor = mix(color.rgb, fogColor, dist*0.003);
  out_Color = vec4(tex.rgb*color.rgb*spritebrightness, tex.a*transparency); //vec4(skycolor*0.41, clamp(tex.a*(1-fogAmount)*color.a*0.2, 0, 1));
  out_Normal = vec4(vec3(0.5), 1.0);
    uint renderData = 0u;
  renderData = ENCODE_RENDERPASS(8);
  out_Material = uvec4(0u,0u+renderData,0u,0u);
}