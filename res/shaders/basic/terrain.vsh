#version 150 core
 
in vec4 in_position;
in vec3 in_normal;
in vec4 in_texcoord;
in vec4 in_color;
in vec4 in_brightness;
in vec3 in_blockinfo;

attribute vec4 blockinfo;
uniform int renderWireFrame;

out vec4 color;
out vec4 lmcoord;
out vec4 texcoord;
out vec3 normal;
out vec3 globalNormal;
out vec3 vposition;
out highp vec3 triangle;
flat out int blockTexture;


void main() {
	texcoord = gl_TextureMatrix[0] * in_texcoord;
	lmcoord = gl_TextureMatrix[1] * in_brightness;
	normal = normalize(gl_NormalMatrix * in_normal);
	globalNormal = normalize(in_normal);
	color = in_color;
	// color = vec4(0,0,0,1);
	// color.r = blockinfo.x/255.0f;
	blockTexture = int(in_blockinfo.x);
	vposition = (gl_ModelViewMatrix * in_position).xyz;
	// gl_FogFragCoord = vposition.z;
	gl_Position = gl_ModelViewProjectionMatrix * in_position;
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
