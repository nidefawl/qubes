#version 150 core


#pragma include "tonemap.glsl"

uniform sampler2D texColor;
uniform sampler2D texLight;

in vec2 pass_texcoord;

out vec4 out_Color;

#define RGB_TO_LUMINANCE (vec3(0.212671, 0.715160, 0.072169))

void main(void) 
{
    vec4 lightInfo = texture(texLight, pass_texcoord, 0);
    float skyLightLvl = lightInfo.x;
    float blockLightLvl = lightInfo.y;
    float occlusion = lightInfo.z;
    // vec4 colorsample = texture( texColor, pass_texcoord );
    // out_Color = vec4(colorsample.rgb*sum, colorsample.a);
    // vec2 vTexCoord = gl_FragCoord.xy * cRTPixelSize.zw;

    // need to use textureOffset here
    vec3 col0 = textureOffset(texColor, pass_texcoord, ivec2( -2,  0 ) ).xyz; //CORRECT?!
    vec3 col1 = textureOffset(texColor, pass_texcoord, ivec2(  2,  0 ) ).xyz;
    vec3 col2 = textureOffset(texColor, pass_texcoord, ivec2(  0, -2 ) ).xyz;
    vec3 col3 = textureOffset(texColor, pass_texcoord, ivec2(  0,  2 ) ).xyz;
    vec3 tolum = RGB_TO_LUMINANCE;

    // srgbToLin(tolum);
    vec3 col = (col0+col1+col2+col3) * 0.25;
    float sum = max(dot(tolum, col), 1e-10);
    // float sum = colorsample.r + colorsample.g + colorsample.b;
    sum -= 1.92f;
    sum += clamp(pow(blockLightLvl,4), 0.0, 1.0);
    sum = clamp(exp(sum*4), 0.0, 1.0);
    // out_Color = vec4( vec3(skyLightLvl), 1.0 );

    out_Color = vec4( col.xyz*sum, 1.0 );
}
