#version 150 core

#pragma define "FAR_BLOCKFACE"
#pragma define "MODEL_RENDER"

#pragma include "ubo_scene.glsl"
#pragma include "ubo_constants.glsl"
#pragma include "vertex_layout.glsl"
#pragma include "blockinfo.glsl"
#pragma include "util.glsl"

#ifdef MODEL_RENDER
uniform mat4 model_matrix;
#endif


out vec4 color;
out vec3 normal;
out vec4 texcoord;
out vec4 position;
out vec2 light;
flat out vec4 faceAO;
flat out vec4 faceLight;
flat out vec4 faceLightSky;
flat out uvec4 blockinfo;
flat out float blockid; // duplicate data, not sure if faster than per fragment bitmasking (on blockinfo.y)
flat out uint faceDir; // also duplicate, I really need to check if passing varyings costs more than bitmasking
out float blockside;
out vec2 texPos;

#define MAX_AO 3.0

const vec4 aoLevels = normalize(vec4(0, 0.2, 0.4, 0.6));
#define LIGHT_MASK 0xFu
#define AO_MASK 0x3u
#define DEBUG_MODE2
#define BR_0 0.27f
#define BR_1 0.38f
#define BR_2 0.56f
#define BR_3 1.0f
// #define BR_0 0.6f
// #define BR_1 0.8f
// #define BR_2 0.9f
// #define BR_3 1.0f
float blocksidebrightness[6] = float[6](BR_2, BR_2, BR_3, BR_0, BR_1, BR_1);

void main() {
	vec4 camNormal = in_matrix_3D.normal * vec4(in_normal.xyz, 1);
	camNormal.xyz/=camNormal.w;
	normal = normalize(camNormal.xyz);
	color = in_color;
	blockinfo = in_blockinfo;
	blockid = BLOCK_ID(blockinfo);

	vec4 pos = in_position;
	texcoord = in_texcoord;
	texPos = clamp(in_texcoord.xy, vec2(0), vec2(1));
	float distCam = length(in_position.xyz - CAMERA_POS);

	faceDir = BLOCK_FACEDIR(blockinfo);
	uint vertDir = BLOCK_VERTDIR(blockinfo);
	vec3 dir = vertexDir.dir[vertDir].xyz;

	if (IS_WAVING(blockid)) {

		float f = mod(gl_VertexID+1.0, 4.0f);
		f = step(2, f);
		if (f != 0) {
		    float sampleDist = 4.4;
		    vec2 p0 = vec2(0);//floor(pos.xz*0.1f);
		    vec2 floord = pos.xz+0.5;
		    float aniSpeed = FRAME_TIME;
		    aniSpeed+=floord.x*1+floord.x*1;
		    aniSpeed*=0.0015;
		    vec2 p2 = p0 + vec2(aniSpeed, 33)*sampleDist;
		    vec2 p1 = p0 + vec2(123, aniSpeed)*sampleDist;
		    
		    float s0 = snoise(p2);
		    float s1 = snoise(p1);
		    float s2 = s0*s1;
		    const float wd = 0.83;
		 //    // float frand = (s0+s1+s2)/16.0;
			pos.x += s0*wd*s2;
			pos.z += s1*wd*s2;
			// pos.z += f*2*cos(aniSpeed)*frand;
		}
	}
	const float face_offset = 1/32.0;
	float distScale = face_offset*clamp(pow((distCam+8)/200, 1.35), 0.0008, 1);
	pos.x += dir.x*distScale;
	pos.y += dir.y*distScale;
	pos.z += dir.z*distScale;


	blockside = blocksidebrightness[faceDir];
// #ifndef FAR_BLOCKFACE
	faceAO = vec4(
		aoLevels[BLOCK_AO_IDX_0(in_blockinfo)],
		aoLevels[BLOCK_AO_IDX_1(in_blockinfo)],
		aoLevels[BLOCK_AO_IDX_2(in_blockinfo)],
		aoLevels[BLOCK_AO_IDX_3(in_blockinfo)]
		);
// #endif

	faceLightSky = vec4(
		float(in_light.y&LIGHT_MASK)/15.0,
		float((in_light.y>>4u)&LIGHT_MASK)/15.0,
		float((in_light.y>>8u)&LIGHT_MASK)/15.0,
		float((in_light.y>>12u)&LIGHT_MASK)/15.0
		);

	faceLight = vec4(
		float(in_light.x&LIGHT_MASK)/15.0,
		float((in_light.x>>4u)&LIGHT_MASK)/15.0,
		float((in_light.x>>8u)&LIGHT_MASK)/15.0,
		float((in_light.x>>12u)&LIGHT_MASK)/15.0
		);
	

#ifdef MODEL_RENDER
	position = model_matrix * pos;
    gl_Position = in_matrix_3D.mvp * model_matrix * in_position;
#else
	position = pos;
	gl_Position = in_matrix_3D.mvp * pos;
#endif
}