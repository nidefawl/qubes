#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"
#pragma include "tonemap.glsl"

uniform mat4 model_matrix;
out vec4 pass_Color;
 
void main(void) {
    pass_Color = in_color;
	srgbToLin(pass_Color.rgb);
    gl_Position = in_matrix_3D.mvp * model_matrix * in_position;
}