#version 420
layout(triangles) in;
layout(line_strip, max_vertices = 2) out;

#pragma include "ubo_scene.glsl"

uniform mat4 model_matrix;

in Data{
    vec3 normal;
    vec4 position;
} vdata[3];

out Data{
    vec3 color;
} gdata;

void main(){
    vec4 middle = (vdata[0].position + vdata[1].position + vdata[2].position)/3.0;
    middle.w = 1.0;
    vec3 normal = normalize((vdata[0].normal + vdata[1].normal + vdata[2].normal)/3.0);
    vec4 pos1 = model_matrix * vec4(middle.xyz - RENDER_OFFSET + PX_OFFSET.xyz, middle.w);
    gl_Position = in_matrix_3D.mvp * pos1;
    gdata.color = vec3(0);
    EmitVertex();
    vec4 normalSS4 = model_matrix *vec4(normal, 1.0);
    normalSS4 /= normalSS4.w;
    pos1.xyz+=normalize(normalSS4.xyz)*0.05;
    // middle+=vec4(normal*0.01, 0);
    gl_Position = in_matrix_3D.mvp * pos1;
    gdata.color = normal*0.5+0.5;
    
    EmitVertex();

    EndPrimitive();
}