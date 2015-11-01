#version 150 core


in vec4 lum;
out vec4 out_Color;
 
void main(void) {
    out_Color = lum;
}