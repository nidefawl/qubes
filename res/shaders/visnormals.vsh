#version 150
 
in vec4 in_position;
in vec3 in_normal;
in vec2 in_texcoord;
in vec4 in_color;
in vec2 in_brightness;
in vec3 in_blockinfo;



out Data{
    vec3 normal;
    vec4 position;
} vdata;

    
void main(){
    vdata.position = in_position;
    vdata.normal = in_normal;
    // gl_Position = gl_ModelViewProjectionMatrix * in_position;
}