#version 150 core
 
#pragma include "vertex_layout.glsl"

out Data{
    vec4 position;
} vdata;
void main() {
    vdata.position = in_position;
}
