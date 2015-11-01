#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

out vec2 TexCoord1;
out vec2 TexCoord2;
out vec2 TexCoord3;
out vec2 TexCoord4;
 
uniform vec2 twoTexelSize;

void main(void) {
	TexCoord1 = in_texcoord.st;
	TexCoord2 = in_texcoord.st + vec2(twoTexelSize.x, 0);
	TexCoord3 = in_texcoord.st + vec2(twoTexelSize.x, twoTexelSize.y);
	TexCoord4 = in_texcoord.st + vec2(0, twoTexelSize.y);
    vec4 pos = vec4(in_position.xyz, in_position.w);
    gl_Position = in_matrix_2D.mvp * pos;
}

