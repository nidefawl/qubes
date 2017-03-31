#version 150 core
 
#pragma include "ubo_scene.glsl"
#pragma include "ubo_transform.glsl"
#pragma include "vertex_layout.glsl"


#ifdef VULKAN_GLSL
layout(push_constant) uniform PushConstantsWireFrame1 {
	mat4 model_matrix;
	vec4 linecolor;
	int num_vertex;
} pushCWireFrame1;
#define LINE_COLOR pushCWireFrame1.linecolor
#define NUM_VERTEX pushCWireFrame1.num_vertex
#define MODEL_MAT pushCWireFrame1.model_matrix
#define VERTEX_ID gl_VertexIndex
#else
uniform vec4 linecolor;
uniform int num_vertex;
uniform mat4 model_matrix;
#define LINE_COLOR linecolor
#define NUM_VERTEX num_vertex
#define MODEL_MAT model_matrix
#define VERTEX_ID gl_VertexID
#endif




out vec4 color;
noperspective out vec3 vposition;
noperspective out vec3 normal;
out vec3 triangle;

void main() {
	vec4 vPos = MODEL_MAT * vec4(in_position.xyz - RENDER_OFFSET + PX_OFFSET.xyz, in_position.w);
    gl_Position = in_matrix_3D.mvp * vPos;
	vposition = (in_matrix_3D.mv * vPos).xyz;
	normal  = in_normal.xyz;
	color = LINE_COLOR;
	int vertexID = int(mod(VERTEX_ID, NUM_VERTEX));
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
