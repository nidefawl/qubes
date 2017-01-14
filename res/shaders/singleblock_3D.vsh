#version 150 core


#pragma include "ubo_scene.glsl"
#pragma include "ubo_constants.glsl"
#pragma include "vertex_layout.glsl"
#pragma include "blockinfo.glsl"

uniform mat4 in_modelMatrix;

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



	vec4 camNormal = in_matrix_3D.normal * vec4(in_normal.xyz, 1);
	normal = normalize(camNormal.xyz);
	vec4 lightPos1V = vec4(lightPos1, 1);
	vec4 lightPos2V = vec4(lightPos2, 1);
	color = in_color;
	blockinfo = in_blockinfo;

	vec4 pos = in_position;
	texcoord = in_texcoord;
	texPos = clamp(in_texcoord.xy, vec2(0), vec2(1));



	vec4 position = in_matrix_3D.p * in_modelMatrix * in_position;
	gl_Position = position;
}


// void main(void) {
// 	vec4 camNormal = in_matrix_3D.normal * vec4(in_normal.xyz, 1);
// 	normal = normalize(camNormal.xyz);
// 	color = in_color;
// 	vec4 pos = in_position;
// 	texcoord = in_texcoord;
// 	position = model_matrix * pos;
//     gl_Position = in_matrix_3D.p * model_matrix * in_position;
// }