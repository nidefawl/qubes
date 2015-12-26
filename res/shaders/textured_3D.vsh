#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

out vec4 pass_Color;
out vec2 pass_texcoord;

void main(void) {
    pass_Color = in_color;
    pass_texcoord = in_texcoord.st;
    gl_Position = in_matrix_3D.mvp * vec4(in_position.xyz - RENDER_OFFSET + PX_OFFSET.xyz, in_position.w);
}