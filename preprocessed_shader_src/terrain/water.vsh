#version 450
#line 2 0
#define VULKAN_GLSL
#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable

#line 1 1
#define VULKAN_GLSL

layout(set = 0, binding = 0, std140) uniform uboMatrix3D
{
    mat4 mvp;
    mat4 mv;
    mat4 view;
    mat4 vp;
    mat4 p;
    mat4 normal;
    mat4 mv_inv;
    mat4 proj_inv;
    mat4 mvp_inv;
} in_matrix_3D;

layout(set = 0, binding = 1, std140) uniform uboMatrix2D
{
    mat4 mvp;
    mat4 p3DOrtho;
    mat4 mv3DOrtho;
} in_matrix_2D;

layout(set = 0, binding = 2, std140) uniform uboSceneData
{
    vec4 cameraPosition;
    vec4 framePos;
    vec4 viewport;
    vec4 prevCameraPosition;
    vec4 sceneSettings;
} in_scene;

layout(set = 1, binding = 0, std140) uniform uboTransformStack
{
    vec4 pxoffset;
} in_transform_stack;



#define FRAME_TIME in_scene.framePos.w
#define CAMERA_POS in_scene.cameraPosition.xyz
#define PREV_CAMERA_POS in_scene.prevCameraPosition.xyz
#define RENDER_OFFSET in_scene.framePos.xyz
#define PX_OFFSET in_transform_stack.pxoffset
#define Z_FAR in_scene.viewport.w
#define Z_NEAR in_scene.viewport.z
#line 1 2
#define VULKAN_GLSL

#define BLOCK_ID_u32(blockinfo32) ((blockinfo32 >> 16u) & 0xFFFu)
#define BLOCK_RENDERPASS_u32(blockinfo32) ((blockinfo32 >> 28u) & 0xFu)
#define BLOCK_TEX_SLOT_u32(blockinfo32) float(blockinfo32&0xFFFu)
#define BLOCK_NORMAL_SLOT_u32(blockinfo32) float((blockinfo32&0xF000u)>>12u)

#define BLOCK_ID(blockinfo) (blockinfo.y&0xFFFu)
#define BLOCK_RENDERPASS(blockinfo) float((blockinfo.y&0xF000u)>>12u)
#define BLOCK_TEX_SLOT(blockinfo) float(blockinfo.x&0xFFFu)
#define BLOCK_NORMAL_SLOT(blockinfo) float((blockinfo.x&0xF000u)>>12u)
#define BLOCK_FACEDIR(blockinfo) (blockinfo.w&0x7u)
#define BLOCK_VERTDIR(blockinfo) ((blockinfo.w >> 3u) & 0x3Fu)
#define BLOCK_AO_IDX_0(blockinfo) (in_blockinfo.z)&AO_MASK
#define BLOCK_AO_IDX_1(blockinfo) (in_blockinfo.z>>2u)&AO_MASK
#define BLOCK_AO_IDX_2(blockinfo) (in_blockinfo.z>>4u)&AO_MASK
#define BLOCK_AO_IDX_3(blockinfo) (in_blockinfo.z>>6u)&AO_MASK
#define IS_SKY(blockid) float(blockid==0u)
#define IS_WATER(blockid) float(blockid==10u)

#define IS_LIGHT(blockid) float(blockid==2222u)

#define IS_ILLUM(renderpass) float(renderpass==4)
#define IS_BACKFACE(renderpass) float(renderpass==3)

#define ENCODE_RENDERPASS(renderpass) ((uint(renderpass)&0xFu)<<12u)
#line 1 3
#define VULKAN_GLSL
#ifdef VULKAN_GLSL
layout (location = 0) in vec3 in_position;
layout (location = 1) in vec4 in_normal;
layout (location = 2) in vec2 in_texcoord;
layout (location = 3) in vec4 in_color;
layout (location = 4) in uvec4 in_blockinfo;
layout (location = 5) in uvec2 in_light;

#else 
in vec4 in_position; 
in vec4 in_normal; 
in vec4 in_texcoord; 
in vec4 in_color; 
in uvec4 in_blockinfo;
in uvec2 in_light;
#endif
#line 9 0

layout (location = 0)  out vec4 color;
layout (location = 1)  out vec2 texcoord;
layout (location = 2)  out vec3 normal;
layout (location = 3) flat  out uvec4 blockinfo;
layout (location = 4) flat  out uint faceDir;
layout (location = 5) flat  out vec4 faceLight;
layout (location = 6) flat  out vec4 faceLightSky;
layout (location = 7)  out vec2 texPos;
layout (location = 8)  out vec4 vpos;
layout (location = 9)  out vec3 vwpos;
layout (location = 10)  out float isWater;
layout (location = 11)  out float roughness;

#define LIGHT_MASK 0xFu

void main() {
	blockinfo = in_blockinfo;
	
	faceDir = BLOCK_FACEDIR(blockinfo);
    uint blockid = BLOCK_ID(blockinfo);
	
	vec4 camNormal = in_matrix_3D.normal * vec4(in_normal.xyz, 1);
	camNormal.xyz/=camNormal.w;// not required? (3x3 does not w)
	
	normal = normalize(camNormal.xyz);
	texcoord = in_texcoord.st;
    isWater = IS_WATER(blockid);
	color = in_color;
	roughness = (in_normal.w < 0 ? 255+in_normal.w : in_normal.w) / 255.0f;

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

	texPos = clamp(in_texcoord.xy, vec2(0), vec2(1));
	

	vpos = in_matrix_3D.mv * vec4(in_position.xyz, 1.0);
	vwpos = in_position.xyz;
	// gl_Position = in_matrix_3D.mvp * (in_position+terroffset);
	gl_Position = in_matrix_3D.mvp * vec4(in_position.xyz, 1.0);
}
