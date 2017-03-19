
#ifndef TRI_WINDING
#define TRI_WINDING 1
#endif
#ifdef VULKAN_GLSL
#define VERTEX_IDX gl_VertexIndex
#else
#define VERTEX_IDX gl_VertexID
#endif

#if TRI_WINDING == 1
   const vec4 vertices[3] = vec4[3](
                                    vec4( -1,  -1, 0.0, 0.0),
                                    vec4(  3,  -1, 2.0, 0.0),
                                    vec4( -1,  3, 0.0,  2.0));
    pass_texcoord.st = vertices[VERTEX_IDX].zw;
    gl_Position = vec4(vertices[VERTEX_IDX].xy, 0, 1);
#else
   const vec4 vertices[3] = vec4[3](vec4( -1, -3, 0.0, 2.0),
                                    vec4(  3,  1, 2.0, 0.0),
                                    vec4( -1,  1, 0.0, 0.0));
    pass_texcoord.st = vertices[VERTEX_IDX].zw;
    gl_Position = vec4(vertices[VERTEX_IDX].xy, 0, 1);
#endif