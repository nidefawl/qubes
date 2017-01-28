#version 150 core

uniform sampler2D texColor;

in vec4 pass_texcoord;

out vec4 out_Color;
 
void main(void) {
    out_Color = vec4(texture(texColor, pass_texcoord.st).rgb, 1.0);
}