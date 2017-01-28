#version 150 core

out vec2 TexCoord1;
out vec2 TexCoord2;
out vec2 TexCoord3;
out vec2 TexCoord4;
 
uniform vec2 twoTexelSize;

void main(void) {
	vec2 pass_texcoord;
#pragma include "fullscreen_triangle_vertex.glsl"
	TexCoord1 = pass_texcoord.st;
	TexCoord2 = pass_texcoord.st + vec2(twoTexelSize.x, 0);
	TexCoord3 = pass_texcoord.st + vec2(twoTexelSize.x, twoTexelSize.y);
	TexCoord4 = pass_texcoord.st + vec2(0, twoTexelSize.y);
}

