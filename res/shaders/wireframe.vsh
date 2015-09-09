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
	int vertexID = int(mod(gl_VertexID, 4));
	if (vertexID == 0) {
    	triangle = vec3(0, 0, 255);
	}
	if (vertexID == 1) {
    	triangle = vec3(0, 255, 0);
	}
	if (vertexID == 2) {
    	triangle = vec3(255, 0, 0);
	}
	if (vertexID == 3) {
    	triangle = vec3(0, 255, 0);
	}
}
