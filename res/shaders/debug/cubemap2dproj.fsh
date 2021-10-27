#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "unproject.glsl"
#pragma include "tonemap.glsl"

uniform samplerCube tex0;

in vec2 pass_texcoord;
 
out vec4 out_Color;


void main(void) {
    float phi=pass_texcoord.s*3.1415*2;
    float theta=(-pass_texcoord.t+0.5)*3.1415;
    vec3 dir = vec3(cos(phi)*cos(theta),sin(theta),sin(phi)*cos(theta));
    vec4 sample = texture( tex0, dir );
	float constexposure = 60.0;
	sample.rgb = ToneMap(sample.rgb*0.02, constexposure);
    out_Color = vec4(sample.rgb, 1.0);
}