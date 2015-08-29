#version 150

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"


out Data{
    vec3 normal;
    vec4 position;
} vdata;

    
void main(){
    vdata.position = in_position;
	vdata.normal = normalize(gl_NormalMatrix * in_normal.xyz);
    // gl_Position = gl_ModelViewProjectionMatrix * in_position;
}