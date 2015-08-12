#version 130


attribute vec4 blockinfo;

varying vec4 color;
varying vec4 lmcoord;
varying vec4 texcoord;
varying vec3 normal;
varying vec3 globalNormal;
flat varying int blockid;


void main() {
	texcoord = gl_TextureMatrix[0] * gl_MultiTexCoord0;
	lmcoord = gl_TextureMatrix[1] * gl_MultiTexCoord1;
	normal = normalize(gl_NormalMatrix * gl_Normal);
	globalNormal = normalize(gl_Normal);
	color = gl_Color;
	// color = vec4(0,0,0,1);
	// color.r = blockinfo.x/255.0f;
	blockid = int(blockinfo.x);
	vec4 v = gl_Vertex;
	gl_FogFragCoord = gl_Position.z;
	gl_Position = gl_ModelViewProjectionMatrix * v;
}