#version 330

out vec4 pass_texcoord;


void main() 
{
   const vec4 vertices[3] = vec4[3](
                                    vec4( -1,  -1, 0.0, 0.0),
                                    vec4(  3,  -1, 2.0, 0.0),
                                    vec4( -1,  3, 0.0,  2.0));
    pass_texcoord = vec4(vertices[gl_VertexID].zw, 0, 0);
    gl_Position = vec4(vertices[gl_VertexID].xy, 0, 1);
}
