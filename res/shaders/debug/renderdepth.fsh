#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "unproject.glsl"

uniform sampler2D tex0;

in vec2 pass_texcoord;
 
out vec4 out_Color;


float Linear01Depth(float depth) {
	float clipSpaceZ= (depth-Z_NEAR) / (1000-Z_NEAR);
	return clipSpaceZ;
}

void main(void) {
  float z = texture2D(tex0, pass_texcoord).r;
  float eyeZ = linearizeDepth(z);
  float lind = Linear01Depth(eyeZ);
  float d = pow(1-lind, 16);
  // float r = clamp(lind, 0, 1);
  out_Color = vec4(d, lind, eyeZ, 1);
}