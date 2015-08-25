#version 150 core

#headerdef

uniform vec3 in_offset;
out vec4 pass_Color;
 
void main(void) {
    pass_Color = in_color;
    gl_Position = in_matrix.mvp * vec4(in_position.xyz + in_offset, in_position.w);
}