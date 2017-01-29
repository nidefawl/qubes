
#ifndef TRI_WINDING
#define TRI_WINDING 1
#endif

#if TRI_WINDING == 1
   const vec4 vertices[3] = vec4[3](
                                    vec4( -1,  -1, 0.0, 0.0),
                                    vec4(  3,  -1, 2.0, 0.0),
                                    vec4( -1,  3, 0.0,  2.0));
    pass_texcoord.st = vertices[gl_VertexID].zw;
    gl_Position = vec4(vertices[gl_VertexID].xy, 0, 1);
#else
   const vec4 vertices[3] = vec4[3](vec4( -1, -3, 0.0, 2.0),
                                    vec4(  3,  1, 2.0, 0.0),
                                    vec4( -1,  1, 0.0, 0.0));
    pass_texcoord.st = vertices[gl_VertexID].zw;
    gl_Position = vec4(vertices[gl_VertexID].xy, 0, 1);
#endif