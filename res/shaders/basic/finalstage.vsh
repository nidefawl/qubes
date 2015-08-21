#version 130


varying vec4 texcoord;

void main() {
	texcoord = gl_TextureMatrix[0] * gl_MultiTexCoord0;
	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
