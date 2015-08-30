#version 150 core


uniform sampler2D texColor;

in vec2 pass_texcoord;

out vec4 out_Color;


void main()
{
    vec4 colorsample = texture( texColor, pass_texcoord );
    float sum = colorsample.r + colorsample.g + colorsample.b;
    sum -= 2.9f;
    sum = clamp(exp(sum*4), 0, 1);
    // sum = sum >= 2.8 ? 1 : 0;
    out_Color = vec4(colorsample.rgb*sum, colorsample.a);
}

