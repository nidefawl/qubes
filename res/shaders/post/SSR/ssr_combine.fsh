#version 150 core

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
    uint blockid = (blockinfo.y&0xFFFu);
    float isWater = float(blockid==4u);
    float a = blurred.a*0.5*isWater;
    out_Color = vec4( mix(albedo.rgb, blurred.rgb, a), 1.0 );
}
