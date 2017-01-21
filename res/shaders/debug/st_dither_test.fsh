/*

This shader tests how the Valve fullscreen dithering
shader affects color and banding.

The function is adapted from slide 49 of Alex Vlachos's
GDC2015 talk: "Advanced VR Rendering".
http://alex.vlachos.com/graphics/Alex_Vlachos_Advanced_VR_Rendering_GDC2015.pdf

--
Zavie

*/


#define gamma 2.2
#define steps 32.0

// Dither type
// Valve's dither straight from the pdf
// #define DITHERTYPE 0

// Valve's dither with * 0.375; removed
#define DITHERTYPE 1

// Valve's dither with +1.5 to -0.5 range
//#define DITHERTYPE 2


// ---8<----------------------------------------------------------------------

#if DITHERTYPE == 0
vec3 ScreenSpaceDither(vec2 vScreenPos)
{
    vec3 vDither = vec3(dot(vec2(131.0, 312.0), vScreenPos.xy + iGlobalTime));
    vDither.rgb = fract(vDither.rgb / vec3(103.0, 71.0, 97.0)) - vec3(0.5, 0.5, 0.5);
    return (vDither.rgb / steps) * 0.375;
}
#elif DITHERTYPE == 1
vec3 ScreenSpaceDither(vec2 vScreenPos)
{
    vec3 vDither = vec3(dot(vec2(131.0, 312.0), vScreenPos.xy + iGlobalTime));
    vDither.rgb = fract(vDither.rgb / vec3(103.0, 71.0, 97.0)) - vec3(0.5, 0.5, 0.5);
    return (vDither.rgb / steps);
}
#else
vec3 ScreenSpaceDither(vec2 vScreenPos)
{
    vec3 vDither = vec3(dot(vec2(131.0, 312.0), vScreenPos.xy + iGlobalTime));
    vDither.rgb = fract(vDither.rgb / vec3(103.0, 71.0, 97.0)) * vec3(2.0,2.0,2.0) - vec3(0.5, 0.5, 0.5);
    return (vDither.rgb / steps);
}
#endif

vec3 dither(vec2 vScreenPos)
{
    vec3 vDither = vec3(dot(vec2(131.0, 312.0), vScreenPos.xy + iGlobalTime));
    return fract(vDither.rgb / vec3(103.0, 71.0, 97.0)) - vec3(0.5, 0.5, 0.5);
}
// ---8<----------------------------------------------------------------------

// The functions that follow are only used to generate
// the color gradients for demonstrating dithering effect.

float h00(float x) { return 2.*x*x*x - 3.*x*x + 1.; }
float h10(float x) { return x*x*x - 2.*x*x + x; }
float h01(float x) { return 3.*x*x - 2.*x*x*x; }
float h11(float x) { return x*x*x - x*x; }
float Hermite(float p0, float p1, float m0, float m1, float x)
{
	return p0*h00(x) + m0*h10(x) + p1*h01(x) + m1*h11(x);
}

// Source:
// http://lolengine.net/blog/2013/07/27/rgb-to-hsv-in-glsl
vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

vec3 generateColor(vec2 uv)
{
	float a = sin(iGlobalTime * 0.5)*0.5 + 0.5;
	float b = sin(iGlobalTime * 0.75)*0.5 + 0.5;
	float c = sin(iGlobalTime * 1.0)*0.5 + 0.5;
	float d = sin(iGlobalTime * 1.25)*0.5 + 0.5;
    
    float mirrorx = abs(uv.x * 2.0 - 1.0) * 0.5;
	
	float y0 = mix(a, b, mirrorx);
	float y1 = mix(c, d, mirrorx);
	float x0 = mix(a, c, uv.y);
	float x1 = mix(b, d, uv.y);
    
    float h = fract(mix(0., 0.1, Hermite(0., 1., 4.*x0, 4.*x1, mirrorx)) + iGlobalTime * 0.05);
    float s = Hermite(0., 1., 5.*y0, 5.*y1, 1. - uv.y);
    float v = Hermite(0., 1., 5.*y0, 5.*y1, uv.y);

	return hsv2rgb(vec3(h, s, v));
}

void mainImageSteps( out vec4 fragColor, in vec2 fragCoord )
{
	vec2 uv = fragCoord.xy / iResolution.xy;
   
	// vec3 color = pow(fract(pass_texcoord.x*2.0)*0.5*vec3(0.2), vec3(1./1));
	vec2 border = vec2(0.1);
	vec2 tc = pass_texcoord;
	vec2 tc2 = (pass_texcoord-vec2(border))/(vec2(1.0)-border*2.0);
	// pass_texcoord*=1.2f;
	// float brGradient = fract(pass_texcoord.x*4.0);
	float br1 = 0.2;
	float br2 = 0.1;
	float b = step(tc.x, border.x)+step(1.0-border.x, tc.x);
	float br = mix(fract(tc2.x*2.0)*br2, br1, b);
	vec3 color = vec3(br);
	
    vec3 ditheredColor = color + ScreenSpaceDither(fragCoord.xy);
    
    float separatorHalfWidth = 2.0 / iResolution.x;

    float separator = 1. - smoothstep(0.5 - separatorHalfWidth, 0.5, uv.x) * smoothstep(0.5 + separatorHalfWidth, 0.5, uv.x);
    vec3 finalColor = mix(color, ditheredColor, clamp(floor(uv.x * 2.0), 0.0, 1.0)) * separator;
	fragColor = vec4(floor(finalColor * steps) / steps, 1.0);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
	vec2 uv = fragCoord.xy / iResolution.xy;
   
	// vec3 color = pow(fract(pass_texcoord.x*2.0)*0.5*vec3(0.2), vec3(1./1));
	vec2 border = vec2(0.1);
	vec2 tc = pass_texcoord;
	vec2 tc2 = (pass_texcoord-vec2(border))/(vec2(1.0)-border*2.0);
	// pass_texcoord*=1.2f;
	// float brGradient = fract(pass_texcoord.x*4.0);
	float br1 = 0.2;
	float br2 = 0.1;
	float b = step(tc.x, border.x)+step(1.0-border.x, tc.x);
	float br = mix(fract(tc2.x*2.0)*br2, br1, b);
	vec3 color = vec3(br);
	
    vec3 ditheredColor = color + dither(fragCoord.xy)/255.0;
    
    float separatorHalfWidth = 2.0 / iResolution.x;
    float separator = 1. - smoothstep(0.5 - separatorHalfWidth, 0.5, uv.x) * smoothstep(0.5 + separatorHalfWidth, 0.5, uv.x);
    vec3 finalColor = mix(color, ditheredColor, clamp(floor(uv.x * 2.0), 0.0, 1.0)) * separator;
	fragColor = vec4(finalColor, 1.0);
}
