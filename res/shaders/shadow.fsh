#version 130

uniform sampler2DArray blockTextures;

varying vec4 color;
varying vec4 texcoord;
flat varying int blockTexture;


void main() {
	vec4 tex = texture(blockTextures, vec3(texcoord.st, blockTexture)) * color;
    gl_FragData[0] = tex;
	// gl_FragData[0] = vec4(1, 1, 0, 1.0);
	//float brightness = getBrightness(wpos);
	//gl_FragData[0] = vec4(brightness, brightness, brightness, 1);
}