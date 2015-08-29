#version 420
layout(lines_adjacency) in;
layout(line_strip, max_vertices = 2) out;

#pragma include "ubo_scene.glsl"


in Data{
    vec3 normal;
    vec4 position;
} vdata[4];

out Data{
    vec3 color;
} gdata;

void main(){
    vec4 middle = (vdata[0].position + vdata[1].position + vdata[2].position + vdata[3].position)/4;
    middle.w = 1.0;
    vec3 normal = normalize((vdata[0].normal + vdata[1].normal + vdata[2].normal + vdata[3].normal)/4);

    gl_Position = in_matrix.mvp * middle;
    gdata.color = vec3(0);
    EmitVertex();
    
    gl_Position = in_matrix.mvp * (middle + vec4(normal*0.4, 0));
    gdata.color = normal*0.5+0.5;
    
    EmitVertex();

    EndPrimitive();
}