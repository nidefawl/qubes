#version 150 core
 
#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

uniform vec4 linecolor;
uniform int num_vertex;

out vec4 color;
noperspective out vec3 vposition;
noperspective out vec3 normal;
 out vec3 triangle;

void main() {
	vec4 vPos = vec4(in_position.xyz - RENDER_OFFSET + PX_OFFSET.xyz, in_position.w);
    gl_Position = in_matrix_3D.mvp * vPos;
	vposition = (in_matrix_3D.mv * vPos).xyz;
	normal  = in_normal.xyz;
	color = linecolor;
	int vertexID = int(mod(gl_VertexID, num_vertex));
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
