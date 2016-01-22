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

const vec3 fogColor=vec3(0.54f, 0.74f, 0.96f)*1.1f;

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
#define noiseTextureResolution (256.0f)
#define noisetex tex0
#define texcoord pass_texcoord
#define viewWidth in_scene.viewport.x
#define viewHeight in_scene.viewport.y
#define cameraPosition CAMERA_POS
#define rainStrength 0.0
#define colorSunlight vec3(1.2,1.1,1.0)
#define colorSkylight vec3(1)
#define colorBouncedSunlight (mix(colorSunlight, colorSkylight, 0.15f))
#define timeMidnight nightNoon
#define wetness 0.0

#define isSky true

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
  float xy1 = texture2D(noisetex, coord, -100).x;
  float xy2 = texture2D(noisetex, coord2, -100).x;
  return mix(xy1, xy2, f.z);
}


float GetCoverage(in float coverage, in float density, in float clouds)
{
  clouds = clamp(clouds - (1.0f - coverage), 0.0f, 1.0f - density) / (1.0f - density);
  clouds = max(0.0f, clouds * 1.1f - 0.1f);
  // clouds = clouds = clouds * clouds * (3.0f - 2.0f * clouds);
  // clouds = pow(clouds, 1.0f);
  return clouds;
}

vec4 CloudColor2(in vec4 worldPosition, in float sunglow, in vec3 worldLightVector, const bool isShadowPass)
{


  float dist = length(worldPosition.xz - cameraPosition.xz);
  worldPosition.xz /= 1.0f + max(0.0f, exp(dist*0.00000001f));
  vec3 p = worldPosition.xyz / 4000.0f;

  float t = FRAME_TIME * 1.0f;
  t *= 0.01;

  p += (Get3DNoise(p * 2.5f + vec3(0.0f, t * 0.01f, 0.0f)) * 2.0f - 1.0f) * 0.10f;
  p.x -= (Get3DNoise(p * 0.125f + vec3(0.1f, t * 0.01f, 0.0f)) * 2.0f - 1.0f) * 1.2f;


  p.x *= 0.45f;
  p.x -= t * 0.01f;

    vec3 p1 = p * vec3(1.0f, 0.5f, 1.0f)  + vec3(0.0f, t * 0.01f, 0.0f);
    float noise  =  Get3DNoise(p * vec3(1.0f, 0.5f, 1.0f) + vec3(0.0f, t * 0.01f, 0.0f)); 

    p *= 1.5f;  
    // p.x -= t * 0.017f;  
    // p.z += noise * 1.35f; 
    // p.x += noise * 0.5f;                  
    vec3 p2 = p;
    noise += (2.0f - abs(Get3DNoise(p) * 2.0f - 0.0f)) * (0.25f);

    // p *= 1.5f;  
    // p.xz -= t * 0.05f;
    // p.z += noise * 1.35f; 
    // p.x += noise * 0.5f;  
    // p.x *= 3.0f;  
    // p.z *= 0.55f; 
    // p.z -= (Get3DNoise(p * 0.25f + vec3(0.0f, t * 0.01f, 0.0f)) * 2.0f - 1.0f) * 0.4f;
    vec3 p3 = p;
    noise += (3.0f - abs(Get3DNoise(p) * 3.0f - 0.0f)) * (0.035f);

    p *= 3.0f;
    p.xz -= t * 0.005f;
    vec3 p4 = p;
    noise += (3.0f - abs(Get3DNoise(p) * 3.0f - 0.0f)) * (0.025f);

    p *= 3.0f;
    p.xz -= t * 0.005f;
    if (!isShadowPass)
    {
      noise += ((Get3DNoise(p))) * (0.022f);
      p *= 3.0f;
      noise += ((Get3DNoise(p))) * (0.024f);
    }
    noise /= 1.575f;

  //cloud edge
  float rainy = mix(wetness, 1.0f, rainStrength);
  //rainy = 0.0f;
  float coverage = 0.65f + rainy * 0.35f;
  //coverage = mix(coverage, 0.97f, rainStrength);

  // float dist = length(worldPosition.xz - cameraPosition.xz);
  coverage *= max(0.0, 1.2f - dist * 0.000006);
  float density = 0.0f;

  if (isShadowPass)
  {
    return vec4(GetCoverage(coverage + 0.2f, density, noise));
  }
  else
  {

    noise = GetCoverage(coverage, density, noise);
    noise = noise * noise * (3.0f - 2.0f * noise);

    float lightOffset = 0.02;

float largeSundiff=0;
    float sundiff = Get3DNoise(p1 + worldLightVector.xyz * lightOffset);
    sundiff += (2.0f - abs(Get3DNoise(p2 + worldLightVector.xyz * lightOffset / 2.0f) * 2.0f - 0.0f)) * (0.55f);    
    largeSundiff = sundiff;
    sundiff += (3.0f - abs(Get3DNoise(p3 + worldLightVector.xyz * lightOffset / 5.0f) * 3.0f - 0.0f)) * (0.065f);
    sundiff += (3.0f - abs(Get3DNoise(p4 + worldLightVector.xyz * lightOffset / 8.0f) * 3.0f - 0.0f)) * (0.025f);
    sundiff /= 1.5f;

      sundiff = -GetCoverage(coverage * 1.0f, 0.0f, sundiff);
      largeSundiff = -GetCoverage(coverage, 0.0f, largeSundiff * 1.3f);
    float secondOrder   = pow(clamp(sundiff * 1.00f + 1.35f, 0.0f, 1.0f), 7.0f);
    float firstOrder  = pow(clamp(largeSundiff * 1.1f + 1.56f, 0.0f, 1.0f), 3.0f);



    float directLightFalloff = secondOrder+firstOrder;
    float anisoBackFactor = mix(clamp(pow(noise, 1.2f) * 3.5f, 0.0f, 1.0f), 1.0f, sunglow);

        directLightFalloff *= anisoBackFactor;
        directLightFalloff *= mix(11.5f, 1.0f, pow(sunglow, 0.5f));
    


    vec3 colorDirect = colorSunlight * 1.915f;
       colorDirect = mix(colorDirect, colorDirect * vec3(0.2f, 0.5f, 1.0f), timeMidnight);
       colorDirect *= 0.9f + sunglow * pow(directLightFalloff, 1.1f) * (1.0f - rainStrength);
       // colorDirect *= 1.0f + pow(1.0f - sunglow, 2.0f) * 30.0f * pow(directLightFalloff, 1.1f) * (1.0f - rainStrength);


    vec3 colorAmbient = mix(colorSkylight, colorSunlight * 2.0f, vec3(0.15f)) * 0.04f;
       colorAmbient *= mix(1.0f, 0.3f, timeMidnight);
       colorAmbient *= mix(1.0f, ((1.0f - noise) + 0.5f) * 1.4f, rainStrength);
       colorAmbient = mix(colorAmbient, colorAmbient * 3.0f + colorSunlight * 0.05f, vec3(clamp(pow(1.0f - noise, 12.0f) * 1.0f, 0.0f, 1.0f)));


    directLightFalloff *= 1.0f - rainStrength * 0.45f;


    directLightFalloff += (pow(Get3DNoise(p3), 2.0f) * 0.5f + pow(Get3DNoise(p3 * 1.5f), 2.0f) * 0.25f) * 0.02f;
    directLightFalloff *= Get3DNoise(p2);

    vec3 color = mix(colorAmbient, colorDirect, vec3(min(1.0f, directLightFalloff)));

    color *= 0.5f;

    // noise *= mix(1.0f, 5.0f, sunglow);

    vec4 result = vec4(color, clamp(noise*1.2, 0, 1));

    return result;
  }
  
}
void blendColor(inout vec4 dest, vec3 color, float alpha) {
  dest.rgb = mix(dest.rgb*dest.a, color, alpha);
}

void main() { 
    vec4 pos = unprojectPos(pass_texcoord.st, 1.0);
    vec3 rayDir=-normalize(pos.xyz);
    vec3 rayOrigin = vec3(0);

    vec3 sunDir = normalize(-SkyLight.lightDir.xyz);
    float sunTheta = max( dot(rayDir, sunDir), 0.0 );
    float sunSpotDens = pow(sunTheta, 8.0)*0.4;
    vec3 skySunScat = skyAtmoScat(-rayDir, SkyLight.lightDir.xyz, moonSunFlip);
    vec3 sky = vec3(fogColor*0.05);
    float scatbr = clamp((skySunScat.r+skySunScat.b+skySunScat.g) / 2.0f, 0, 1);
    sky = mix(sky, sky*skySunScat, clamp(0.14f+sunTheta, 0, 1));
    sky += skySunScat*0.5f;
    float zfar = in_scene.viewport.w;


    vec4 cloudColor = vec4(sky, 1);
    vec3 worldLightVector = normalize(SkyLight.lightDir.xyz);

    if (pos.y>0) {
        vec3 i = vec3(0);  
        if (IntersectRayPlane(rayOrigin, rayDir, vec3(0, 7500, 0), vec3(0, -1, 0), i)) {


          float density = 1.0f;
          vec4 cloudSample = CloudColor2(vec4(i.xyz, 1.0f), sunSpotDens, worldLightVector, false);
          blendColor(cloudColor, cloudSample.rgb*0.2, cloudSample.a * density);
        }
    }
    // out_Color = vec4((normalize(SkyLight.lightDir.rgb)*0.5+0.5)*0.04, 1);
    out_Color = cloudColor;

    out_Normal = vec4(0.5);
    uint renderData = 0u;
    // if (cloudColor.a > 0) {
    renderData = ENCODE_RENDERPASS(8);
    // }
    out_Material = uvec4(0u,renderData,0u,1u);
}
