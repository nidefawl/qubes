
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



#define FRAME_TIME in_scene.framePos.w
#define CAMERA_POS in_scene.cameraPosition.xyz
#define PREV_CAMERA_POS in_scene.prevCameraPosition.xyz
#define RENDER_OFFSET in_scene.framePos.xyz
#define PX_OFFSET in_transform_stack.pxoffset
#define Z_FAR in_scene.viewport.w
#define Z_NEAR in_scene.viewport.z