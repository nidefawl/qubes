#version 150 core

#pragma include "blockinfo.glsl"

uniform sampler2D texColor;
uniform sampler2D texSSRBlurred;
uniform usampler2D texMaterial;

in vec2 pass_texcoord;

out vec4 out_Color;

void main(void) 
{
    vec4 albedo = texture(texColor, pass_texcoord, 0);
    vec4 blurred = texture(texSSRBlurred, pass_texcoord, 0);
    uvec4 blockinfo = texture(texMaterial, pass_texcoord, 0);
    uint blockid = BLOCK_ID(blockinfo);
    float isWater = IS_WATER(blockid);
    float a = blurred.a*0.5*isWater;
    out_Color = vec4( mix(albedo.rgb, blurred.rgb, a), 1.0 );
}
