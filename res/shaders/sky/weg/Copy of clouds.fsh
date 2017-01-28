#version 150 core
 
#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

layout(std140) uniform LightInfo {
  vec4 dayLightTime; 
  vec4 posSun; // Light position in world space
  vec4 lightDir; // Light dir in world space
  vec4 La; // Ambient light intensity
  vec4 Ld; // Diffuse light intensity
  vec4 Ls; // Specular light intensity
} SkyLight;

uniform sampler2D tex0;
in vec4 pass_texcoord;

out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;

const vec3 fogColor=vec3(0.54f, 0.74f, 0.96f)*1.1f;
bool IntersectRayPlane(vec3 rayOrigin, vec3 rayDirection, vec3 posOnPlane, vec3 planeNormal, out vec3 intersectionPoint)
{
  float rDotn = dot(rayDirection, planeNormal);
 
  //parallel to plane or pointing away from plane?
  if (rDotn < 0.0000001 )
    return false;
 
  float s = dot(planeNormal, (posOnPlane - rayOrigin)) / rDotn;
 
  intersectionPoint = rayOrigin + s * rayDirection;
 
  return true;
}
vec4 unprojectPos(in vec2 coord, in float depth) { 
    // vec4 fragposition = in_matrix_3D.proj_inv * vec4(coord.s * 2.0f - 1.0f, coord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
    vec4 fragposition = inverse(in_matrix_3D.vp) * vec4(coord.s * 2.0f - 1.0f, coord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
    fragposition /= fragposition.w;
    return fragposition;
}

float filter(float f, float a)
{
   f = clamp(f - a, 0.0, 1.0);
   f /= (1.0 - a);    
   return f;
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

// cloud rendering

vec2 getuv(in vec2 uv, float l)
{
    return uv*l;
}

// cloud rendering


// maybe something between 0.5 and 3.0
const float CLOUD_HEIGHT = 0.8;

// scale of clouds
const float UV_FREQ = 0.009;

// cloudiness, bigger number = less clouds
const float CLOUD_FILTER = 0.28;

// parallax layers
const int PARALLAX_LAYERS = 6;

void main() { 
	vec4 pos = unprojectPos(pass_texcoord.st, 1.0);
	vec3 dir = pos.xyz;
	vec3 rayDir=-normalize(dir);
  	vec3 color = vec3(1);
  	float alpha = 1.0;
  	vec3 rayOrigin = vec3(0);
    float sunTheta = dot(-rayDir, normalize(SkyLight.lightDir.xyz));
  	if (pos.y>0) {
		// out_Color = vec4(vec3(0), 1);
  		vec3 intersectPoint = vec3(0);	
  		if (IntersectRayPlane(rayOrigin, rayDir, vec3(0, 342, 0), vec3(0, -1, 0), intersectPoint)) {
  		// 	out_Color = vec4(vec3(1, 0, 0), 0.5);
	  		// float a = fbm(intersectPoint.xz*0.00001);
	  		// a += fbm(intersectPoint.xz*0.00001);
		    float t = FRAME_TIME*0.001;
		    float freq = UV_FREQ;
  			vec2 uv = intersectPoint.xz*0.002;
		    vec2 _uv = getuv(uv, 1.0);
		    _uv.y += t;
		    float l = 1.0;
		    vec4 col = vec4(0);
		    float strength = 0.11;
		    vec4 color = vec4(0.9, 0.9, 1.0, 0.8);
		    color = mix(color, vec4(1.0, 1.0, 0.9, 1.0), sunTheta);
		    float alpha = 0;
		    float sc = (1.0);
		    for (int i = 0; i < PARALLAX_LAYERS; ++i)
		    {
		        // 3 parallax layers of clouds
		        // float h = fbm((_uv) * freq) * strength*6.1;
		        float aa = texture2D(tex0, _uv * 2.0).r*0.01;
		        vec2 uv2 = vec2(_uv.x+aa*0.1,_uv.y-aa*0.1);
		       float h = fbm((uv2) * freq) * 0.5;
		        h += max(0, h-0.2)*fbm(vec2(-t * 0.001, t * 0.0015) + _uv * freq * 1.1) * 0.64;
		        h += fbm(vec2(t * 0.003, -t * 0.0025) + _uv * freq * 1.2) * 0.05;
		        
		        float f = filter(h, CLOUD_FILTER);
		        f += -(l - 1.0) * CLOUD_HEIGHT; // height
		        f = clamp(f, 0.0, 1.0);
		        alpha+=(f*i*0.3);
		        // if (col.a >= 1)
		        // 	break;
		        float frange = 0.2;
		        l *= 1.0+frange - (frange*3.3*h);
		       
		        _uv = getuv(uv, l);
		        _uv.y += 0.1;
		    }
		    col = clamp(col, vec4(0), vec4(1));
		    if (length(uv)<10.2)
	  			out_Color = vec4(vec3(4), clamp(alpha, 0, 1));//vec4(vec3(0), clamp(a, 0, 1)*0.9);
  		}
  	}
  	// vec3 intersectPoint2 = vec3(0);
  	// if (IntersectRayPlane(rayOrigin, rayDir, vec3(0, 10, 0), vec3(0, -1, 0), intersectPoint)) {
  	// 	// out_Color = vec4(vec3(pos, 1), 0.3);	
	  // 	if (IntersectRayPlane(rayOrigin, rayDir, vec3(0, 30, 0), vec3(0, -1, 0), intersectPoint2)) {

			// vec3 rayLen=intersectPoint2-intersectPoint;
	  // 		const int steps = 20;
	  // 		vec3 stepSize = rayLen/steps;
	  // 		vec3 wPos = intersectPoint;
	  // 		float acc = 0;
	  // 		float freq = 0.001;
	  // 		for (int i = 0; i < steps; i++) {
	  // 			vec2 pos = wPos.xz;
		 //        float h = Clouds(wPos * freq);
		 //        acc += h;
		 //        wPos += stepSize;
	  // 		}
	  // 		acc*=0.1;
	  // 		acc = clamp(acc, 0.001, 0.99);
			// out_Color = vec4(vec3(0), acc*0.9);	
	  // 	}
  	// } else {
  	// }
	
	out_Normal = vec4(0.5);
	out_Material = uvec4(0);
}
