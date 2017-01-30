#version 150 core
// #pragma include "ubo_scene.glsl"
// #pragma include "debug_buffer.glsl"

uniform sampler2D texColor;
uniform sampler2D texAO;
// uniform sampler2D texDepth;

// uniform mat4 vp_cur_inv;
// uniform mat4 vp_prev;

in vec2 pass_texcoord;
 
out vec4 out_Color;
// out vec4 out_FinalMaterial;
// out vec4 out_Velocity;

void main(void) {
    vec3 albedo = texture(texColor, pass_texcoord).rgb*0.02;
    float ao = texture(texAO, pass_texcoord).r;
//     float depth = texture(texDepth, pass_texcoord).r;
//     vec4 curScreenPos = vec4(pass_texcoord.s * 2.0f - 1.0f, pass_texcoord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
//    	vec4 curViewPos = vp_cur_inv * curScreenPos;
//     vec4 prevScreenPos = vp_prev * curViewPos;
//     vec2 velocity = (curScreenPos.xy - prevScreenPos.xy);
//     velocity *= 0.5;
//     velocity.y *= -1.0;
// #print vec2 velocity velocity

	vec4 tex1 = texture(texColor, pass_texcoord.st, 0)*0.02;
	vec4 tex2 = texture(texAO, pass_texcoord.st, 0);
	vec3 colorOut = albedo * ao;
	out_Color = vec4(colorOut, 1);
	// out_FinalMaterial = vec4(1);
	// out_Velocity = vec4(velocity, 1, 1);
}
