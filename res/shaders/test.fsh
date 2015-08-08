#version 120

uniform sampler2D depthSampler;
varying vec2 wpos;
varying vec4 color;
varying vec4 texcoord;
varying vec4 lmcoord;
varying vec3 normal;
varying vec3 globalNormal;


float getBrightness(vec2 b) {
	return (1-pow(1-b.x, 2))*(1-pow(1-b.y, 2));
}


void main() {	
    //gl_FragData[0] = texture2D(depthSampler, wpos);
	gl_FragData[0] = vec4(lmcoord.s, 0, 0, 1.0);
	//float brightness = getBrightness(wpos);
	//gl_FragData[0] = vec4(brightness, brightness, brightness, 1);
}