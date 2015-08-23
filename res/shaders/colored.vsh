#version 150 core
 
in vec4 in_position;
in vec3 in_normal;
in vec2 in_texcoord;
in vec4 in_color;
in vec2 in_brightness;
in vec3 in_blockinfo;

out vec4 pass_Color;
 
void main(void) {
    pass_Color = in_color;
    gl_Position = gl_ModelViewProjectionMatrix * in_position;
}