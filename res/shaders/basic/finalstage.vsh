#version 150 core
 
in vec4 in_position;
in vec3 in_normal;
in vec2 in_texcoord;
in vec4 in_color;
in vec4 in_brightness;
in vec3 in_blockinfo;

out vec2 pass_texcoord;

void main() {
	pass_texcoord = in_texcoord;
	gl_Position = gl_ModelViewProjectionMatrix * in_position;
}
