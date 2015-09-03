#version 150 core
 
#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

uniform vec4 linecolor;
out vec4 color;
out vec3 vposition;
out highp vec3 triangle;

void main() {
	gl_Position = in_matrix.mvp * in_position;
	vposition = (in_matrix.mv * in_position).xyz;
	color = linecolor;
	if (in_blockinfo.y == 0) {
    	triangle = vec3(0, 0, 255);
	}
	if (in_blockinfo.y == 1) {
    	triangle = vec3(0, 255, 0);
	}
	if (in_blockinfo.y == 2) {
    	triangle = vec3(255, 0, 0);
	}
	if (in_blockinfo.y == 3) {
    	triangle = vec3(0, 0, 255);
	}
}
