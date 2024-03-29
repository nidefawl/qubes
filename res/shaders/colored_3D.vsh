#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "ubo_transform.glsl"
#pragma include "vertex_layout.glsl"

out vec4 pass_Color;
 
void main(void) {
    pass_Color = in_color;
    gl_Position = in_matrix_3D.mvp * vec4(in_position.xyz - RENDER_OFFSET + PX_OFFSET.xyz, in_position.w);
    // gl_Position = matortho * vec4(in_position.xyz + PX_OFFSET.xyz, in_position.w);
}