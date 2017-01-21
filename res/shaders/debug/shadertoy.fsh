#version 150 core

#pragma include "debug_buffer.glsl"
#pragma include "ubo_scene.glsl"
#pragma include "util.glsl"

uniform sampler2D iChannel0;

uniform sampler2D iChannel1;

in vec2 pass_texcoord;

in vec3 pass_rayDir;
in vec3 pass_rayOrigin;
 
out vec4 out_Color;

uniform vec4 iMouse;

#define iGlobalTime FRAME_TIME*0.01
#define iResolution in_scene.viewport.xy
#define texture2D lookupTex
vec4 lookupTex(sampler2D sampler0, vec2 texcoord) {
  return texture(sampler0, vec2(texcoord.x, 1-texcoord.y));
}
vec4 lookupTex(sampler2D sampler0, vec2 texcoord, float offset) {
  return texture(sampler0, vec2(texcoord.x, 1-texcoord.y), offset);
}

#pragma include "st_dither_test.fsh"

void main() {
	// out_Color=vec4(1);
	mainImage(out_Color, pass_texcoord*iResolution);
  out_Color.a=1.0;
}