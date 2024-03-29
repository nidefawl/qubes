#version 150 core
#pragma include "ubo_scene.glsl"
#pragma include "ubo_transform.glsl"
#pragma include "dither.glsl"


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
  float stepScale = (end - start) / 4.0;
  float y = start + stepScale * 0.5;
  float value = 0.0;
  for (int i = 0; i < 4; i++) {
    value += roundedBoxShadowX(point.x, point.y - y, sigma, corner, halfSize) * gaussian(y, sigma) * stepScale;
    y += stepScale;
  }

  return value;
}

#ifdef VULKAN_GLSL
layout(push_constant) uniform PushConsts {
  vec4 box;
  vec4 color;
  float sigma;
  float corner;
  float fade;
  float zpos;
  int colorwheel;
  float valueH;
  float valueS;
  float valueL;
} pushConsts;
#define G_BOX pushConsts.box
#define G_COLOR pushConsts.color
#define G_VALUE_H pushConsts.valueH
#define G_VALUE_S pushConsts.valueS
#define G_VALUE_L pushConsts.valueL
#define G_COLORWHEEL pushConsts.colorwheel
#define G_SIGMA pushConsts.sigma
#define G_CORNER pushConsts.corner
#define G_FADE pushConsts.fade
#else
uniform vec4 box;
uniform vec4 color;
uniform float valueH;
uniform float valueS;
uniform float valueL;
uniform int colorwheel;
uniform float sigma;
uniform float corner;
uniform float fade;
#define G_BOX box
#define G_COLOR color
#define G_VALUE_H valueH
#define G_VALUE_S valueS
#define G_VALUE_L valueL
#define G_COLORWHEEL colorwheel
#define G_SIGMA sigma
#define G_CORNER corner
#define G_FADE fade
#endif
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
 //    finalColor = mix(shadow, rect, inside);
 vec4 finalColor = vec4(1.0);
  if (G_COLORWHEEL == 1) {
        
    finalColor = G_COLOR;
    float range = -0.004;
    vec3 g = color_palette(range+(1-texcoord.x)*(1-range*2), a, b, c, d);
    float l1 = G_VALUE_L/0.5f;
    float l2 = max(0, l1-1.0f);
    g = mix(vec3(G_VALUE_L), g, G_VALUE_S);
    vec3 h = mix(mix(vec3(0), g, l1), vec3(1), l2);
    finalColor.rgb = h; 
  } else if (G_COLORWHEEL == 2) {
    vec3 e = vec3(0.5, 0.5, 0.5);
    vec3 f = color_palette(G_VALUE_H, a, b, c, d);
    finalColor = G_COLOR;
    vec3 g = mix(e, f, texcoord.x);
    float l1 = G_VALUE_L/0.5f;
    float l2 = max(0, l1-1.0f);
    vec3 h = mix(mix(vec3(0), g, l1), vec3(1), l2);
    finalColor.rgb = h; 
  } else if (G_COLORWHEEL == 3) {
    vec3 e = vec3(0);
    vec3 f = color_palette(G_VALUE_H, a, b, c, d);
    f = mix(vec3(0.5f), f, G_VALUE_S);
    vec3 g = vec3(1);
    float l1 = texcoord.x/0.5f;
    float l2 = max(0, l1-1.0f);
    finalColor = G_COLOR;
    finalColor.rgb = mix(mix(e, f, l1), g, l2); 
  } else if (G_COLORWHEEL == 4) {
    finalColor = G_COLOR;
  } else {
    finalColor = G_COLOR;
    finalColor.rgb *= (1-G_FADE)+texcoord.y*G_FADE;
  }
  finalColor.rgb+=dither8BitSS();

    
  finalColor.a *= roundedBoxShadow(G_BOX.xy+PX_OFFSET.xy, G_BOX.zw+PX_OFFSET.xy, vertex, G_SIGMA, G_CORNER);
  if (finalColor.a<0.01)
    discard;
  out_Color = finalColor;
}
