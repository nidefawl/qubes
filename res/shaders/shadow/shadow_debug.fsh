#version 150 core

#pragma include "ubo_scene.glsl"

out vec4 out_Color;

uniform sampler2D texShadow;

void main() {
	vec2 texcoord = gl_FragCoord.xy/vec2(2048.0);
  	float z = texture(texShadow, texcoord).r;
  	float dist = gl_FragCoord.z - z;
	out_Color = vec4(gl_FragCoord.z);
}
