#version 120

attribute vec4 in_vertex;
attribute vec3 in_normal;

varying vec3 vnormal;
varying vec4 v;
    
uniform float waveTime;
uniform float waveWidth;
uniform float waveHeight;
 
uniform vec3 camerapos;
uniform mat4 projmatrix;
uniform mat4 mvmatrix;

void main(void)
{
	v = vec4(in_vertex);
	v.y += sin(waveWidth * v.x + waveTime) * cos(waveWidth * v.z + waveTime) * waveHeight;
	v.z += sin(waveWidth * v.y + waveTime) * cos(waveWidth * v.x + waveTime) * waveHeight;
	//vnormal = projmatrix * mvmatrix * in_normal;
 	gl_Position = projmatrix * mvmatrix * v;
}