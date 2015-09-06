#version 150 core
 
#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

// uniform vec4 terroffset;
out vec4 color;
out vec4 texcoord;
out vec3 normal;
out uvec4 blockinfo;
out vec4 faceAO;
out vec2 texPos;
#define MAX_AO 3.0

vec4 aoLevels = vec4(0, 0.2, 0.4, 0.6)*0.75;

void main() {
	texcoord = in_texcoord;
	vec4 camNormal = in_matrix.normal * vec4(in_normal.xyz, 1);
	camNormal.xyz/=camNormal.w;
	normal = normalize(camNormal.xyz);
	color = in_color;


	faceAO = vec4(
		aoLevels[in_blockinfo.z&0x3],
		aoLevels[(in_blockinfo.z>>2)&0x3],
		aoLevels[(in_blockinfo.z>>4)&0x3],
		aoLevels[(in_blockinfo.z>>6)&0x3]
		);
	texPos = clamp(in_texcoord.xy, vec2(0), vec2(1));


	blockinfo = in_blockinfo;

	// gl_Position = in_matrix.mvp * (in_position+terroffset);
	gl_Position = in_matrix.mvp * in_position;
}
