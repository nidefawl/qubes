#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

out vec4 pass_Color;
out vec2 pass_texcoord;

void main(void) {
    pass_Color = in_color;
    pass_texcoord = in_texcoord.st;
    vec4 pos = vec4(in_position.xyz+PX_OFFSET.xyz, in_position.w);
    gl_Position = in_matrix_2D.mvp * pos;
}