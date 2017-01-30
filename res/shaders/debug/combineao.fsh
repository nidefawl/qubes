#version 150 core
#pragma include "ubo_scene.glsl"
#pragma include "debug_buffer.glsl"

uniform sampler2D texColor;
uniform sampler2D texAO;
uniform sampler2D texDepth;

uniform mat4 mvp_prev;

in vec2 pass_texcoord;
 
layout(location = 0) out vec4 output0;
layout(location = 1) out vec4 output1;

void main(void) {
    vec4 albedo = texture(texColor, pass_texcoord).rgba;
    float ao = texture(texAO, pass_texcoord).r;
    float depth = texture(texDepth, pass_texcoord).r;
    vec4 curScreenPos = vec4(pass_texcoord.s * 2.0f - 1.0f, pass_texcoord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
    vec4 curViewPos = in_matrix_3D.mvp_inv * curScreenPos;
    vec4 prevScreenPos = mvp_prev * curViewPos;
    float isFragment = float(albedo.a > 0);

    vec2 scale = vec2(0.5, 0.5);
    curScreenPos.xy *= scale;
    prevScreenPos.xy *= scale;
    prevScreenPos /= prevScreenPos.w;

    vec2 velocity = (curScreenPos.xy - prevScreenPos.xy);



	vec4 tex1 = texture(texColor, pass_texcoord.st, 0)*0.02;
	vec4 tex2 = texture(texAO, pass_texcoord.st, 0);
	vec3 colorOut = albedo.rgb * ao* ao;
	output0 = vec4(colorOut*0.02, 1);
    output1 = vec4(velocity*isFragment, 0, 1);
}
