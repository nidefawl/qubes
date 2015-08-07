#version 120


uniform sampler2D depthSampler;
uniform vec2 zbufparam;
varying vec2 wpos;

float getBrightness(vec2 b) {
	return (1-pow(1-b.x, 2))*(1-pow(1-b.y, 2));
}
float LinearizeDepth(vec2 uv)
{
  float z = texture2D(depthSampler, uv).x;
  return (2.0 * zbufparam.x) / (zbufparam.y + zbufparam.x - z * (zbufparam.y - zbufparam.x));	
}
void main() {
  vec2 uv = wpos;
  float d = LinearizeDepth(uv);
  gl_FragData[0] = vec4(d, d, d, 1);
}
