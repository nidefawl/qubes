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
uniform float valueH;
uniform float valueS;
uniform float valueL;
uniform int colorwheel;
uniform float sigma;
uniform float corner;
uniform float fade;
in vec2 vertex;
in vec2 texcoord;

out vec4 out_Color;
vec3 color_palette( in float t, in vec3 a, in vec3 b, in vec3 c, in vec3 d )
{
    return a + b*cos( 6.28318*(c*t+d) );
}
  vec3 a = vec3(0.5, 0.5, 0.5);
  vec3 b = vec3(0.5, 0.5, 0.5);
  vec3 c = vec3(1.0, 1.0, 1.0);
  vec3 d = vec3(0.00, 0.33, 0.67);

void main(void) {
	// float inside = isInside(pass_texcoord, rect_min, rect_max);
	// vec2 b = vec2(0.01);
	// float f = roundedBoxShadow(rect_min-b, rect_max+b, pass_texcoord, 0.005, 0.04);
	// vec4 rect = vec4(vec3(0.5), 1);
	// vec4 shadow = vec4(0,0,0,1)*f;
 //    out_Color = mix(shadow, rect, inside);
  if (colorwheel == 1) {
        
    out_Color = color;
    float range = -0.004;
    vec3 g = color_palette(range+(1-texcoord.x)*(1-range*2), a, b, c, d);
    float l1 = valueL/0.5f;
    float l2 = max(0, l1-1.0f);
    g = mix(vec3(valueL), g, valueS);
    vec3 h = mix(mix(vec3(0), g, l1), vec3(1), l2);
    out_Color.rgb = h; 
  } else if (colorwheel == 2) {
    vec3 e = vec3(0.5, 0.5, 0.5);
    vec3 f = color_palette(valueH, a, b, c, d);
    out_Color = color;
    vec3 g = mix(e, f, texcoord.x);
    float l1 = valueL/0.5f;
    float l2 = max(0, l1-1.0f);
    vec3 h = mix(mix(vec3(0), g, l1), vec3(1), l2);
    out_Color.rgb = h; 
  } else if (colorwheel == 3) {
    vec3 e = vec3(0);
    vec3 f = color_palette(valueH, a, b, c, d);
    f = mix(vec3(0.5f), f, valueS);
    vec3 g = vec3(1);
    float l1 = texcoord.x/0.5f;
    float l2 = max(0, l1-1.0f);
    out_Color = color;
    out_Color.rgb = mix(mix(e, f, l1), g, l2); 
  } else if (colorwheel == 4) {
    out_Color = color;
  } else {
    out_Color = color;
    out_Color.rgb *= (1-fade)+texcoord.y*fade;
  }

    
  out_Color.a *= roundedBoxShadow(box.xy+PX_OFFSET.xy, box.zw+PX_OFFSET.xy, vertex, sigma, corner);
  if (out_Color.a<0.01)
    discard;
}
