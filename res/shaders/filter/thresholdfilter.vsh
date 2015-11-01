#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"
 
out vec2 pass_texcoord;

void main(void) {
    gl_Position = in_matrix_2D.mvp * in_position;
    pass_texcoord = in_texcoord.st;
}