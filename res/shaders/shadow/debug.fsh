#version 150 core

#pragma define "ALPHA_TEST"
#pragma define "SAMPLER_CONVERT_GAMMA"
#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"
#pragma include "unproject.glsl"

uniform sampler2D texShadow;

in vec4 pass_Color;
in vec2 pass_texcoord;
 
out vec4 out_Color;
 
float Linear01Depth(float depth) {
	float clipSpaceZ= (depth-Z_NEAR) / (10000-Z_NEAR);
	return min(1, clipSpaceZ);
}
void main(void) {
  float z = texture(texShadow, pass_texcoord).r;
  float eyeZ = linearizeDepth(z);
  float lind = Linear01Depth(eyeZ);
  float d = pow(lind, 1.0f);
  // float r = clamp(lind, 0, 1);
  out_Color = vec4(1.0-pow(1.0-z, 600.0f));
  out_Color = vec4(1.0-pow(lind, 1024));
}