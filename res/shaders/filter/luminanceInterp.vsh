#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

uniform sampler2D texPrev;
uniform sampler2D texNew;

uniform float elapsedTime;

out vec4 lum;

void main(void) {
	float prevLum = texelFetch(texPrev, ivec2(0,0), 0).r;
	float curLum = texelFetch(texNew, ivec2(0,0), 0).r;
	float newLum = prevLum + (curLum - prevLum) * ( 1.0 - pow( 0.98f, 30.0 * elapsedTime ) );
	lum = vec4(vec3(newLum), 1);
    vec4 pos = vec4(in_position.xyz, in_position.w);
    gl_Position = in_matrix_2D.mvp * pos;
}