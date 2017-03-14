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
    vec4 pxoffset;
    vec4 prevCameraPosition;
    vec4 sceneSettings;
} in_scene;

layout(set = 0, binding = 3, std140) uniform uboMatrixShadow
{
    mat4 shadow_split_mvp[4];
    vec4 shadow_split_depth;
} in_matrix_shadow;

#define FRAME_TIME in_scene.framePos.w
#define CAMERA_POS in_scene.cameraPosition.xyz
#define PREV_CAMERA_POS in_scene.prevCameraPosition.xyz
#define RENDER_OFFSET in_scene.framePos.xyz
#define PX_OFFSET in_scene.pxoffset
#define Z_FAR in_scene.viewport.w
#define Z_NEAR in_scene.viewport.z
#line 1 2
#ifdef VULKAN_GLSL
#define VULKAN_GLSL
layout (location = 0) in vec4 in_position;
layout (location = 3) in vec4 in_color;

#else 
in vec4 in_position; 
in vec4 in_normal; 
in vec4 in_texcoord; 
in vec4 in_color; 
in uvec4 in_blockinfo;
in uvec2 in_light;
#endif
#line 8 0

layout (location = 0) out vec4 pass_Color;
flat out uvec4 dummy;
 
void main(void) {
    pass_Color = in_color;
	dummy = uvec4(0u, 1u, 2u, 3u);
    gl_Position = in_matrix_2D.mvp * vec4(in_position.xyz + PX_OFFSET.xyz, in_position.w);
}