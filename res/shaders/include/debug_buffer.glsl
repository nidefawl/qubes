
#extension GL_ARB_shader_storage_buffer_object : enable
#define MAX_VALS 1024
uniform vec2 DEBUG_FRAG_POS;
uniform vec2 VIEWPORT_SIZE;
#define IS_DEBUG_FRAG(fragcoord) (floor(fragcoord.t*VIEWPORT_SIZE.t)==DEBUG_FRAG_POS.t)

layout (std430) buffer debugBuffer
{
    float v_float[MAX_VALS];
    vec2 v_vec2[MAX_VALS];
    vec3 v_vec3[MAX_VALS];
    vec4 v_vec4[MAX_VALS];
} shaderDebugBuffer;
