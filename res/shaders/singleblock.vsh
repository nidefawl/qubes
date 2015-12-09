#version 150 core


#pragma include "ubo_scene.glsl"
#pragma include "ubo_constants.glsl"
#pragma include "vertex_layout.glsl"
#pragma include "blockinfo.glsl"

uniform vec3 in_offset;
uniform mat4 in_modelMatrix;
uniform float in_scale;

out vec4 color;
out vec4 texcoord;
out vec3 normal;
flat out uvec4 blockinfo;
out float Idiff;
out vec2 texPos;


#define MAX_AO 3.0

const vec3 lightPos1 = normalize(vec3(1.2, 1.2, 0));
const vec3 lightPos2 = normalize(vec3(0, 0.6, -1.2));


const vec4 aoLevels = normalize(vec4(0, 0.2, 0.4, 0.6));
#define LIGHT_MASK 0xFu
#define AO_MASK 0x3u
#define DEBUG_MODE2
#define BR_0 0.4f
#define BR_1 0.7f
#define BR_2 0.9f
#define BR_3 1.0f
// #define BR_0 0.6f
// #define BR_1 0.8f
// #define BR_2 0.9f
// #define BR_3 1.0f
float blocksidebrightness[6] = float[6](BR_2, BR_2, BR_3, BR_0, BR_1, BR_1);
void main() {



	mat4 normalMat = transpose(inverse(in_modelMatrix));
	vec4 camNormal = normalMat * vec4(in_normal.xyz, 1);
	normal = normalize(camNormal.xyz);
	vec4 lightPos1V = vec4(lightPos1, 1);
	vec4 lightPos2V = vec4(lightPos2, 1);
	color = in_color;
	blockinfo = in_blockinfo;

	vec4 pos = in_position;
	texcoord = in_texcoord;
	texPos = clamp(in_texcoord.xy, vec2(0), vec2(1));
	float distCam = length(in_position.xyz - CAMERA_POS);

	// uint faceDir = BLOCK_FACEDIR(blockinfo);
	uint vertDir = BLOCK_VERTDIR(blockinfo);
	vec3 dir = vertexDir.dir[vertDir].xyz;

	vec3 L1 = -normalize(lightPos1V.xyz);
	vec3 L2 = -normalize(lightPos2V.xyz);
	Idiff = max(dot(normal,L1), 0.03)*1.3;
	Idiff+= max(dot(normal,L2), 0.03)*0.7;


	const float face_offset = 1/32.0;
	float distScale = face_offset*clamp(pow((distCam+8)/200, 1.35), 0.0008, 1);
	pos.x += dir.x*distScale;
	pos.y += dir.y*distScale;
	pos.z += dir.z*distScale;
	// pos.xyz *= 1/32.0;


	vec4 position = in_matrix_2D.mv3DOrtho * in_modelMatrix * pos;
	vec4 outpos = in_matrix_2D.p3DOrtho * position;
	gl_Position = outpos;
}
