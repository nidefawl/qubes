#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

out vec4 position;
 
void main(void) {
    position = in_position;
    gl_Position = in_matrix_3D.mvp * in_position;
}