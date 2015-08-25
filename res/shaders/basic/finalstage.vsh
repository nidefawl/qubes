#version 150 core
 
#headerdef


out vec2 pass_texcoord;

void main() {
	pass_texcoord = in_texcoord.st;
	gl_Position = in_matrix.mvp * in_position;
}
