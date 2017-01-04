#version 150 core
 
#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"
#pragma include "blockinfo.glsl"
#pragma include "atmosphere.glsl"
#pragma include "sky_scatter.glsl"

layout(std140) uniform LightInfo {
  vec4 dayLightTime; 
  vec4 posSun; // Light position in world space
  vec4 lightDir; // Light dir in world space
  vec4 La; // Ambient light intensity
  vec4 Ld; // Diffuse light intensity
  vec4 Ls; // Specular light intensity
} SkyLight;

in float dayNoon;
in float nightNoon;
in float dayLightIntens;
in float lightAngleUp;
in float moonSunFlip;

uniform sampler2D tex0;
in vec4 pass_texcoord;

out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;

vec4 unprojectPos(in vec2 coord, in float depth) { 
    // vec4 fragposition = in_matrix_3D.proj_inv * vec4(coord.s * 2.0f - 1.0f, coord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
    vec4 fragposition = inverse(in_matrix_3D.vp) * vec4(coord.s * 2.0f - 1.0f, coord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
    fragposition /= fragposition.w;
    return fragposition;
}
vec4 unprojectPosWS(in vec2 coord, in float depth) { 
    // vec4 fragposition = in_matrix_3D.proj_inv * vec4(coord.s * 2.0f - 1.0f, coord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
    vec4 fragposition = inverse(in_matrix_3D.mvp) * vec4(coord.s * 2.0f - 1.0f, coord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
    fragposition /= fragposition.w;
    return fragposition;
}

float filter(float f, float a)
{
   f = clamp(f - a, 0.0, 1.0);
   f /= (1.0 - a);    
   return clamp(f, 0.0, 1.0);
}

float fbm(vec2 uv)
{
    float f = (texture2D(tex0, uv * 2.0).r - 0.5) * 0.2;
    f += (texture2D(tex0, uv * 4.0).r - 0.5) * 0.125;
    f += (texture2D(tex0, uv * 8.0).r - 0.5) * 0.125 * 0.5;
    f += (texture2D(tex0, uv * 16.0).r - 0.5) * 0.125 * 0.25;
    f += (texture2D(tex0, uv * 32.0).r - 0.5) * 0.125 * 0.24;
    f += (texture2D(tex0, uv * 64.0).r - 0.5) * 0.125 * 0.22;
    f += (texture2D(tex0, uv * 128.0).r - 0.5) * 0.125 * 0.12;
    f += (texture2D(tex0, uv * 256.0).r - 0.5) * 0.125 * 0.1;
    f += 0.5;
    return clamp(f, 0.0, 1.0);
}



// otaviogood's noise from https://www.shadertoy.com/view/ld2SzK
//--------------------------------------------------------------
// This spiral noise works by successively adding and rotating sin waves while increasing frequency.
// It should work the same on all computers since it's not based on a hash function like some other noises.
// It can be much faster than other noise functions if you're ok with some repetition.
const float nudge = 0.739513;	// size of perpendicular vector
float normalizer = 1.0 / sqrt(1.0 + nudge*nudge);	// pythagorean theorem on that perpendicular to maintain scale
float SpiralNoiseC(vec3 p)
{
    float n = 0.0;	// noise amount
    float iter = 1.0;
    for (int i = 0; i < 4; i++)
    {
        // add sin and cos scaled inverse with the frequency
        n += -abs(sin(p.y*iter) + cos(p.x*iter)) / iter;	// abs for a ridged look
        // rotate by adding perpendicular and scaling down
        p.xy += vec2(p.y, -p.x) * nudge;
        p.xy *= normalizer;
        // rotate on other axis
        p.xz += vec2(p.z, -p.x) * nudge;
        p.xz *= normalizer;
        // increase the frequency
        iter *= 1.733733;
    }
    return n;
}

float SpiralNoise3D(vec3 p)
{
    float n = 0.0;
    float iter = 1.0;
    for (int i = 0; i < 5; i++)
    {
        n += (sin(p.y*iter) + cos(p.x*iter)) / iter;
        //p.xy += vec2(p.y, -p.x) * nudge;
        //p.xy *= normalizer;
        p.xz += vec2(p.z, -p.x) * nudge;
        p.xz *= normalizer;
        iter *= 1.33733;
    }
    return n;
}

float Clouds(vec3 p)
{
	float final = 0;
    //final -= SpiralNoiseC(p.xyz);	// mid-range noise
    // final += SpiralNoiseC(p*0.323+100.0);	// large scale terrain features
    final += SpiralNoise3D(p);	// more large scale features, but 3d, so not just a height map.
    //final -= SpiralNoise3D(p*49.0)*0.0625*0.125;	// small scale noise for variation

    return final;
}

// maybe something between 0.5 and 3.0
const float CLOUD_HEIGHT = 1.3;

// scale of clouds
const float UV_FREQ = 0.0032;

// cloudiness, bigger number = less clouds
 float CLOUD_FILTER = 0.24;//0.2;
 float CLOUD_FILTER2 = 0.34;//0.2;

// parallax layers
const int PARALLAX_LAYERS = 4;

const float dens = 0.4;
const float cov = 0.5;

const float ff = 0.95/PARALLAX_LAYERS;
// cloud rendering

vec2 getuv(in vec3 w, float l)
{
  // vec2 _offset = vec2(w.xz);
  vec2 _offset = vec2(w.xz);
	vec2 uv = _offset*0.003*l;
  uv+=(CAMERA_POS.xz + RENDER_OFFSET.xz)*0.003;
  uv /= 1.0+length(uv)*0.0001;
  uv.y+=FRAME_TIME*0.001;
  // uv.y+=FRAME_TIME*0.04;
  return uv;
}

float Get3DNoise(in vec3 pos)
{
  pos.z += 0.0f;

  pos.xyz += 0.5f;

  vec3 p = floor(pos);
  vec3 f = fract(pos);

  f.x = f.x * f.x * (3.0f - 2.0f * f.x);
  f.y = f.y * f.y * (3.0f - 2.0f * f.y);
  f.z = f.z * f.z * (3.0f - 2.0f * f.z);

  vec2 uv =  (p.xy + p.z * vec2(17.0f)) + f.xy;
  vec2 uv2 = (p.xy + (p.z + 1.0f) * vec2(17.0f)) + f.xy;

  // uv -= 0.5f;
  // uv2 -= 0.5f;

  vec2 coord =  (uv  + 0.5f) / 64.0f;
  vec2 coord2 = (uv2 + 0.5f) / 64.0f;
  float xy1 = texture2D(tex0, coord, -100).x;
  float xy2 = texture2D(tex0, coord2, -100).x;
  return mix(xy1, xy2, f.z);
  // return 1;
}
float getCloudHeight(vec2 uv) {
   	float h=0;
    float f = Get3DNoise(vec3(uv.x, 0.5, uv.y)*0.02);
    vec3 a = vec3(uv.x-f, 1.5*f+FRAME_TIME*0.01, uv.y+f);
    // vec3 b = vec3(uv.x*f, 11.5*f+FRAME_TIME*0.0004, uv.y*f+FRAME_TIME*0.005);
   	// // h += fbm((uv) * UV_FREQ) * (0.48+dens*0.3);
    // float c = abs(Get3DNoise(b*UV_FREQ*22.1));
    // h += abs(Get3DNoise(a*UV_FREQ*22.2*fbm(uv * UV_FREQ * 2)))*(c);
    // // h += abs(Get3DNoise(a*UV_FREQ*12.2))*1.1;
    
    // h *= max(0, h-0.4+cov*2.3)*fbm(uv * UV_FREQ * 4) * 2.14;
     // min(1, CLOUD_FILTER*1);
    // h *= Get3DNoise(a*UV_FREQ*10);
    h+=fbm(uv * UV_FREQ * 1) * 2.14;
    vec2 uv2 = uv;

    uv2*=1.3;
    uv2.x+=6;
    uv2.y-=6;
    uv2.x*=0.2;
    uv2.y*=0.22;
    uv2*=0.3;
    h+=fbm(uv2 * UV_FREQ * 1) * 1.14* abs(Get3DNoise(vec3(uv2.x, 1, uv2.y)*UV_FREQ*12.2))*1.1;
    h-=fbm(uv * UV_FREQ * 1.2+vec2(2,2)*0.1) * 1.44;
    h *= abs(Get3DNoise(vec3(uv.x, 1, uv.y)*UV_FREQ*12.2))*2.1;
    return clamp(h*h, -0.1, 1.2);
}


float rand(vec2 co)
{// implementation found at: lumina.sourceforge.net/Tutorials/Noise.html
	return fract(sin(dot(co*0.123,vec2(12.9898,78.233))) * 43758.5453);
}
void blendColor(inout vec4 dest, in vec3 color, in float alpha) {
  dest.rgb = mix(dest.rgb, color, alpha);
  dest.a=1.0;
}
/*
out vec2 pass_texcoord;
out float dayNoon;
out float nightNoon;
out float dayLightIntens;
out float lightAngleUp;
out float moonSunFlip;
*/
void main() { 
	vec4 pos = unprojectPos(pass_texcoord.st, 1.0);
	vec3 rayDir=-normalize(pos.xyz);
  	vec3 rayOrigin = vec3(0);

    vec3 sunDir = normalize(-SkyLight.lightDir.xyz);
    float sunTheta = max( dot(rayDir, sunDir), 0.0 );
    float sunTheta2 = max( dot(SkyLight.lightDir.xyz, vec3(0, 1, 0)), 0.0 );
    sunTheta2 = max(1.0f-(sunTheta2*sunTheta2*2.0f), 0.0);
    float fNight = smoothstep(0.0f, 1.0f, clamp(nightNoon, 0.0, 1.0f));
    float sunSpotDens = pow(sunTheta, 4.0+fNight*12)*(1.8-fNight*1.7);
    vec3 skySunScat = skyAtmoScat(-rayDir, SkyLight.lightDir.xyz, moonSunFlip, sunTheta2*1);
    float intens = mix(1, 0, moonSunFlip)*dayLightIntens;

    float yt =1-smoothstep(0, 1, abs(dot(rayDir, vec3(0, 1, 0))));
    vec3 fogColor=vec3(0.74f, 1.24f, 1.66f)*0.7;
     fogColor=vec3(0.74f, 1.24f, 1.66f)*0.7;
    vec3 fogColor2=vec3(1.14f, 1.34f, 1.66f)*0.4;
     fogColor=mix(fogColor, fogColor2, yt);
    vec3 fogColorLit=mix(fogColor, vec3(fogColor*0.0001), fNight)*0.1;
    vec3 sky = vec3(fogColorLit);
    sunSpotDens*=(1-fNight*0.82);
    float scatbr = clamp((skySunScat.r+skySunScat.b+skySunScat.g) / 2.0f, 0, 1);
    // sky = mix(sky, sky*skySunScat, clamp(0.14f+sunTheta, 0, 1));
    sky += skySunScat*1.5f;
    // sky = mix(sky, sky*skySunScat, 0.3f-fNight*0.2f);
    // sky += skySunScat*sunSpotDens*1.2;
    // sky += sky*SkyLight.La.rgb*(1.0-sunSpotDens)*1.1f;
    // sky *= .32;



    // float scatbr = clamp((skySunScat.r+skySunScat.b+skySunScat.g) / 2.0f, 0, 1);
    // sky = mix(sky, sky*skySunScat, clamp(0.14f+sunTheta, 0, 1));
    // sky += skySunScat*0.5f;
    // float zfar = in_scene.viewport.w;


    vec4 cloudColor = vec4(sky, 1);
    uint renderData = 0u;


    blendColor(cloudColor, fogColorLit, yt*0.9);

    out_Color = vec4(cloudColor.rgb*0.02, 1.0);
    out_Normal = vec4(vec3(0.5), 1.0);
    renderData = ENCODE_RENDERPASS(8);
    out_Material = uvec4(0u,0u+renderData,0u,0u);
}
