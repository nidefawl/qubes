#version 150 core

#pragma define "ALPHA_TEST"
#pragma define "EXPLICIT_LOD"
#pragma include "tonemap.glsl"

uniform sampler2D tex0;

in vec4 pass_Color;
in vec2 pass_texcoord;
in vec4 cur_Position;
in vec4 prev_Position;
 
layout(location = 0) out vec4 output0;
layout(location = 1) out vec4 output1;

void main(void) {
	vec3 curPos = cur_Position.xyz/cur_Position.w;
	vec3 prevPos = prev_Position.xyz/prev_Position.w;

    // Calculate velocity in non-homogeneous projection space:
    vec2 velocity = curPos.xy - prevPos.xy;

	vec4 tex = texture(tex0, pass_texcoord.st);
	vec4 color = tex*pass_Color;
	srgbToLin(color.rgb);
    output0 = vec4(color.rgb, tex.a*pass_Color.a);
    // output1 = vec4(color.bgr, tex.a*pass_Color.a);
    output1 = vec4(velocity.xy, 0, 0);
}