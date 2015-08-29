#version 150 core
 
#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"


uniform int renderWireFrame;



out vec4 color;
out vec4 texcoord;
out vec3 normal;
out vec3 vposition;

out highp vec3 triangle;
out vec4 blockinfo;

void main() {
	texcoord = in_texcoord;
	mat3 normMat = mat3(transpose(inverse(in_matrix.mv)));
	normal = normalize(normMat * in_normal.xyz);
	color = in_color;

	blockinfo = in_blockinfo;
	vposition = (in_matrix.mv * in_position).xyz;
	
	gl_Position = in_matrix.mvp * in_position;

	if (renderWireFrame) {
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
}
