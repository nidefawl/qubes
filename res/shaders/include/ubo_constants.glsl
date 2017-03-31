
layout(set = 2, binding = 0, std140) uniform VertexDirections
{
    vec4 dir[64];
} vertexDir;

layout(set = 2, binding = 1, std140) uniform TBNMatrix
{
    mat4 mat[6];
} matrix_tbn;
