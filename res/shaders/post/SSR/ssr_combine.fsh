#version 150 core

#pragma include "blockinfo.glsl"

layout (set = 1, binding = 0) uniform sampler2D texColor;
layout (set = 1, binding = 1) uniform usampler2D texMaterial;
layout (set = 2, binding = 0) uniform sampler2D texSSRBlurred;

in vec2 pass_texcoord;

out vec4 out_Color;

void main(void) 
{
    vec4 albedo = texture(texColor, pass_texcoord, 0);
    vec4 blurred = texture(texSSRBlurred, pass_texcoord, 0);
    uvec4 blockinfo = texture(texMaterial, pass_texcoord, 0);
    uint blockid = BLOCK_ID(blockinfo);
    float isWater = IS_WATER(blockid);
	// vec3 grayXfer = vec3(0.3, 0.59, 0.11);
	// vec3 gray = vec3(dot(grayXfer, blurred.rgb));
	// blurred.rgb = mix(blurred.rgb, gray, 1);
    // float a = blurred.a*0.2*isWater;
    // out_Color = vec4( mix(albedo.rgb, blurred.rgb, a), 1.0 );
    vec3 color = mix(albedo.rgb, blurred.rgb, blurred.a*isWater*0.15);
    out_Color = vec4(color, 1.0);
}
