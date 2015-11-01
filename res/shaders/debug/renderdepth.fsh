#version 150 core


uniform sampler2D tex0;
uniform vec2 zbufparam;

in vec4 pass_Color;
in vec2 pass_texcoord;
 
out vec4 out_Color;
 
float LinearizeDepth(vec2 uv)
{
  float z = texture2D(tex0, uv).x;
  // x == near, y == far
  return (2.0 * zbufparam.x) / (zbufparam.y + zbufparam.x - z * (zbufparam.y - zbufparam.x));	
}

void main(void) {
  float d = LinearizeDepth(pass_texcoord);
  out_Color = vec4(d, d, d, 1);
}