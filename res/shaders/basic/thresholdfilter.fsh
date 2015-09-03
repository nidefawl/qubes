#version 150 core


uniform sampler2D texColor;

in vec2 pass_texcoord;

out vec4 out_Color;

#define RGB_TO_LUMINANCE vec3(0.212671, 0.715160, 0.072169)

void main()
{
    // vec4 colorsample = texture( texColor, pass_texcoord );
    // out_Color = vec4(colorsample.rgb*sum, colorsample.a);
    // vec2 vTexCoord = gl_FragCoord.xy * cRTPixelSize.zw;

    // need to use textureOffset here
    vec3 col0 = textureOffset(texColor, pass_texcoord, ivec2( -2,  0 ) ).xyz;
    vec3 col1 = textureOffset(texColor, pass_texcoord, ivec2(  2,  0 ) ).xyz;
    vec3 col2 = textureOffset(texColor, pass_texcoord, ivec2(  0, -2 ) ).xyz;
    vec3 col3 = textureOffset(texColor, pass_texcoord, ivec2(  0,  2 ) ).xyz;

    vec3 col = (col0+col1+col2+col3) * 0.25;
    float sum = max(dot(RGB_TO_LUMINANCE, col), 1e-10);
    // float sum = colorsample.r + colorsample.g + colorsample.b;
    sum -= 0.9f;
    sum = clamp(exp(sum*4), 0, 1);

    out_Color = vec4( col.xyz*sum, 1.0 );
}

