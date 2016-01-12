#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

uniform vec4 box;
uniform float sigma;
uniform float zpos;
out vec2 vertex;
out vec2 texcoord;
 
void main(void) {
	float padding = 2.0 * sigma;
	texcoord = in_texcoord.st;
	vec2 topleft = box.xy +PX_OFFSET.xy - padding;
	vec2 bottomright = box.zw+PX_OFFSET.xy + padding;
    vertex = mix(topleft, bottomright, in_texcoord.st);
	vec2 glPos = vertex / in_scene.viewport.xy * 2.0 - 1.0;
    // gl_Position = vec4(glPos.x, glPos.y, zpos, 1.0);
    gl_Position = in_matrix_2D.mvp * vec4(vertex.x, vertex.y, zpos+PX_OFFSET.z, 1.0);
}