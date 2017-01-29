#version 150 core

#pragma include "ubo_scene.glsl"

out vec4 pass_texcoord;
#define GEN1

#ifndef GEN
#define SCALE 1.0
void main(void) {

   const vec4 vertices[3] = vec4[3](vec4( -1, -3, 0.0, -1.0),
                                    vec4(  3,  1, 2.0, 1.0),
                                    vec4( -1,  1, 0.0, 1.0));
    pass_texcoord.st = vertices[gl_VertexID].zw;
    gl_Position = vec4(vertices[gl_VertexID].xy, 0, 1);
}
#else
void main(void) {
    vec2 pos;
    pos.x = float(gl_VertexID & 1)*2.0;
    pos.y = float(gl_VertexID & 2);
    pass_texcoord.st = pos;
    gl_Position = vec4(pos * 22.0 - 1.0, 0, 1);
}
#endif