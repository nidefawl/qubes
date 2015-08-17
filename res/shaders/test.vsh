#version 130


attribute vec4 blockinfo;
uniform int renderWireFrame;

varying vec4 color;
varying vec4 lmcoord;
varying vec4 texcoord;
varying vec3 normal;
varying vec3 globalNormal;
varying vec3 vposition;
varying highp vec3 triangle;
flat varying int blockTexture;


void main() {
	texcoord = gl_TextureMatrix[0] * gl_MultiTexCoord0;
	lmcoord = gl_TextureMatrix[1] * gl_MultiTexCoord1;
	normal = normalize(gl_NormalMatrix * gl_Normal);
	globalNormal = normalize(gl_Normal);
	color = gl_Color;
	// color = vec4(0,0,0,1);
	// color.r = blockinfo.x/255.0f;
	blockTexture = int(blockinfo.x);
	vec4 v = gl_Vertex;
	vposition = (gl_ModelViewMatrix * v).xyz;
	gl_FogFragCoord = vposition.z;
	gl_Position = gl_ModelViewProjectionMatrix * v;
	if (renderWireFrame) {
		if (blockinfo.y == 0) {
	    	triangle = vec3(0, 0, 255);
		}
		if (blockinfo.y == 1) {
	    	triangle = vec3(0, 255, 0);
		}
		if (blockinfo.y == 2) {
	    	triangle = vec3(255, 0, 0);
		}
		if (blockinfo.y == 3) {
	    	triangle = vec3(0, 0, 255);
		}
	}
}