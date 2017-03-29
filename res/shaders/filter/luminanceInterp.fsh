#version 150 core


in float lum;
out vec4 out_Color;
 
void main(void) {
    out_Color = vec4(vec3(lum), 1.0);
}