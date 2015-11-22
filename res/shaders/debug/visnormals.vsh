#version 150

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"


out Data{
    vec3 normal;
    vec4 position;
} vdata;

    
void main(){
    vdata.position = in_position;
	// vdata.normal = normalize(gl_NormalMatrix * in_normal.xyz);
	vec4 camNormal = in_matrix_3D.normal * vec4(in_normal.xyz, 1);
	camNormal.xyz/=camNormal.w;
	vdata.normal = (camNormal.xyz);
    // gl_Position = gl_ModelViewProjectionMatrix * in_position;
}