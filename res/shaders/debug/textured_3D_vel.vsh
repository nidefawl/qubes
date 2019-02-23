#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "ubo_transform.glsl"
#pragma include "vertex_layout.glsl"

out vec4 pass_Color;
out vec2 pass_texcoord;
out vec4 prev_Position;
out vec4 cur_Position;

uniform mat4 mvp_prev;

void main(void) {
    pass_Color = in_color;
    pass_texcoord = in_texcoord.st;
    vec4 modelPos = vec4(in_position.xyz - RENDER_OFFSET + PX_OFFSET.xyz, in_position.w);
    gl_Position = in_matrix_3D.mvp * modelPos;
    cur_Position = gl_Position.xyzw;
    prev_Position = (mvp_prev * modelPos).xyzw;


    vec2 scale = vec2(0.5, 0.5);

    cur_Position.xy *= scale;
    prev_Position.xy *= scale;
}