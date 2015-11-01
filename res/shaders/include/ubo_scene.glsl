
layout(std140) uniform uboMatrix3D
{
    mat4 mvp;
    mat4 mv;
    mat4 view;
    mat4 vp;
    mat4 normal;
    mat4 mv_inv;
    mat4 proj_inv;
} in_matrix_3D;

layout(std140) uniform uboMatrix2D
{
    mat4 mvp;
    mat4 mvp3DOrtho;
} in_matrix_2D;

layout(std140) uniform uboMatrixShadow
{
    mat4 shadow_split_mvp[4];
    vec4 shadow_split_depth;
} in_matrix_shadow;

layout(std140) uniform uboSceneData
{
    vec4 cameraPosition;
    vec4 framePos;
    vec4 viewport;
} in_scene;

#define FRAME_TIME in_scene.framePos.w
#define CAMERA_POS in_scene.cameraPosition.xyz
#define RENDER_OFFSET in_scene.framePos.xyz