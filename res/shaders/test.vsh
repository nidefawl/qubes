#version 120

varying vec2 wpos;

varying vec4 color;
varying vec4 lmcoord;
varying vec4 texcoord;
varying vec3 normal;
varying vec3 globalNormal;


void main() {
	wpos = gl_MultiTexCoord0.xy;
	texcoord = gl_TextureMatrix[0] * gl_MultiTexCoord0;
	lmcoord = gl_TextureMatrix[1] * gl_MultiTexCoord1*0.77f;
	normal = normalize(gl_NormalMatrix * gl_Normal);
	globalNormal = normalize(gl_Normal);
	
	gl_FogFragCoord = gl_Position.z;
	
	gl_Position = gl_ProjectionMatrix * gl_Vertex;
}