#version 150 core
 
#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

uniform vec4 linecolor;
out vec4 color;
out vec3 vposition;
out vec3 normal;
out float ftime;
out highp vec3 triangle;
uniform vec3 in_offset;

void main() {
	vec4 vPos = in_position;
	vPos.xyz += in_offset;
	gl_Position = in_matrix.mvp * vPos;
	vposition = (in_matrix.mv * vPos).xyz;
	ftime = in_matrix.frameTime*0.05;
	normal  = in_normal.xyz;
	color = linecolor;
	int vertexID = int(mod(gl_VertexID, 4));
	triangle = vec3(0, 0, 255);
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
