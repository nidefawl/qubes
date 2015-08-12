#version 130

uniform sampler2DArray blockTextures;

varying vec4 color;
varying vec4 lmcoord;
varying vec3 normal;
varying vec3 globalNormal;
varying vec4 texcoord;
flat varying int blockid;


float getBrightness(vec2 b) {
	return (1-pow(1-b.x, 2))*(1-pow(1-b.y, 2));
}


void main() {
	vec4 tex = texture(blockTextures, vec3(texcoord.st, blockid-1));
    gl_FragData[0] = tex;
	// gl_FragData[0] = vec4(1, 1, 0, 1.0);
	//float brightness = getBrightness(wpos);
	//gl_FragData[0] = vec4(brightness, brightness, brightness, 1);
}