#version 330

in vec4 pass_texcoord;

out vec4 out_Color;

void main() 
{
  out_Color = vec4(pass_texcoord.st, 0.0, 1.0);
  out_Color.r *= 0.2;
  out_Color = vec4(pass_texcoord.st+out_Color.g, out_Color.a, out_Color.b);

}