#version 150 core

#pragma include "ubo_transform.glsl"
#pragma include "ubo_scene.glsl"
#pragma include "util.glsl"

uniform sampler2D tex0;

in vec2 pass_texcoord;
 
out vec4 out_Color;
#define FTIME FRAME_TIME*0.1

void main(void) {
	vec4 tex = texture(tex0, pass_texcoord.st, 0);
	// if (tex.a < 1)
	// 	discard;
	float iterations = 50;
	vec3 color = vec3(0);
	float m = mod(FTIME, 10000.);
	vec2 p = pass_texcoord+vec2(sin(m), cos(m))*0.002;
    vec3 a = vec3(0.76,0.72,0.782);
    vec3 b = vec3(0.25,0.35, 0.4);
    vec3 c = vec3(0.8);
    vec3 d = vec3(0);
	for (int i = 0; i < iterations; i++) {
	    float s0 = snoise(p);
	    vec2 sd = vec2(sin(FTIME+i), cos(FTIME+i))*0.01;
	    float s1 = snoise(p+vec2(0,1)*sd);
	    float s2 = snoise(p+vec2(1,0)*sd);
	    float s6 = snoise(p+vec2(s1,s2)-vec2(0.5));
	    float s = (s0+s1+s2)/1.4;
	    vec3 col = pal( s*7.0, a, b, c,d );
	    color += pal( col.g*s6, vec3(1.14)*min(0.5,dot(col,vec3(0.333))), b*2.2, col,d );
	}
	color /= iterations;
    out_Color = vec4(pow(clamp(color, vec3(0), vec3(1)), vec3(2)), 1);
}