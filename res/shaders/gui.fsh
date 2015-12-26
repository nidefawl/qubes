#version 150 core

#pragma include "ubo_scene.glsl"


// A standard gaussian function, used for weighting samples
float gaussian(float x, float sigma) {
  const float pi = 3.141592653589793;
  return exp(-(x * x) / (2.0 * sigma * sigma)) / (sqrt(2.0 * pi) * sigma);
}

// This approximates the error function, needed for the gaussian integral
vec2 erf(vec2 x) {
  vec2 s = sign(x), a = abs(x);
  x = 1.0 + (0.278393 + (0.230389 + 0.078108 * (a * a)) * a) * a;
  x *= x;
  return s - s / (x * x);
}

// Return the blurred mask along the x dimension
float roundedBoxShadowX(float x, float y, float sigma, float corner, vec2 halfSize) {
  float delta = min(halfSize.y - corner - abs(y), 0.0);
  float curved = halfSize.x - corner + sqrt(max(0.0, corner * corner - delta * delta));
  vec2 integral = 0.5 + 0.5 * erf((x + vec2(-curved, curved)) * (sqrt(0.5) / sigma));
  return integral.y - integral.x;
}
float isInside(vec2 pos, vec2 bottomLeft, vec2 topRight) {
    vec2 v = step(bottomLeft, pos)-step(topRight, pos);
    return v.x*v.y;
}
// Return the mask for the shadow of a box from lower to upper
float roundedBoxShadow(vec2 lower, vec2 upper, vec2 point, float sigma, float corner) {
  // Center everything to make the math easier
  vec2 center = (lower + upper) * 0.5;
  vec2 halfSize = (upper - lower) * 0.5;
  point -= center;

  // The signal is only non-zero in a limited range, so don't waste samples
  float low = point.y - halfSize.y;
  float high = point.y + halfSize.y;
  float start = clamp(-3.0 * sigma, low, high);
  float end = clamp(3.0 * sigma, low, high);

  // Accumulate samples (we can get away with surprisingly few samples)
  float step = (end - start) / 4.0;
  float y = start + step * 0.5;
  float value = 0.0;
  for (int i = 0; i < 4; i++) {
    value += roundedBoxShadowX(point.x, point.y - y, sigma, corner, halfSize) * gaussian(y, sigma) * step;
    y += step;
  }

  return value;
}

uniform vec4 box;
uniform vec4 color;
uniform float sigma;
uniform float corner;
in vec2 vertex;
in vec2 texcoord;

out vec4 out_Color;
 
void main(void) {
	// float inside = isInside(pass_texcoord, rect_min, rect_max);
	// vec2 b = vec2(0.01);
	// float f = roundedBoxShadow(rect_min-b, rect_max+b, pass_texcoord, 0.005, 0.04);
	// vec4 rect = vec4(vec3(0.5), 1);
	// vec4 shadow = vec4(0,0,0,1)*f;
 //    out_Color = mix(shadow, rect, inside);

	out_Color = color;
	out_Color.rgb *= 0.7+texcoord.y*0.3; 
    out_Color.a *= roundedBoxShadow(box.xy+PX_OFFSET.xy, box.zw+PX_OFFSET.xy, vertex, sigma, corner);
    if (out_Color.a<0.01)
      discard;
}
