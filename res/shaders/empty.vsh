#version 120

varying vec2 wpos;



void main() {
	wpos = gl_MultiTexCoord0.xy;
	gl_Position = gl_ProjectionMatrix * gl_Vertex;
}