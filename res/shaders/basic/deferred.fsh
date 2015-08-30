#version 150 core
#define DO_SHADING
#define FRAME_TIME in_matrix.frameTime
struct Ray {
	vec3 dir;
	vec3 origin;
};

struct Plane {
	vec3 normal;
	vec3 origin;
};
struct Intersection {
	vec3 pos;
	float d;
	float angle;
};

#pragma include "ubo_scene.glsl"

layout(std140) uniform LightInfo {
  vec4 vSun; // Light position in eye coords.
  vec4 vMoon; // Light position in eye coords.
  vec4 La; // Ambient light intensity
  vec4 Ld; // Diffuse light intensity
  vec4 Ls; // Specular light intensity
} Light;

layout(std140) uniform MaterialInfo {
  vec4 Ka; // Ambient reflectivity
  vec4 Kd; // Diffuse reflectivity
  vec4 Ks; // Specular reflectivity
  float Shininess; // Specular shininess factor
} Material;


struct SurfaceProperties {
    vec3    albedo;                                 //Diffuse texture aka "color texture"
    vec3    normal;                                 //Screen-space surface normals
    float   depth;                                  //Scene depth
    float   linearDepth;                    //Linear depth
    float   NdotL;
    vec4    position;  // camera/eye space position
    vec4    worldposition;  // world space position
    vec3    viewVector;                     //Vector representing the viewing direction
    vec3    lightVector;                    //Vector representing sunlight direction
    vec4    blockinfo;
    vec4    sunSpotColor;
    float   sunSpotDens;
    float   sunProximity;
    float   timeMidnight;
} prop;

#pragma include "sunsky3.glsl"

// #define ENABLE_SOFT_SHADOWS 1

uniform sampler2D texColor;
uniform sampler2D texNormals;
uniform sampler2D texMaterial;
uniform sampler2D texDepth;
uniform sampler2DShadow texShadow;
uniform sampler2D noisetex;

uniform float near;
uniform float far;

in vec2 pass_texcoord;
in vec3 sunDirection;
in vec3 moonDirection;
in float cosSunUpAngle;
in float nightlight;
in float dayLight;
in float dayLightIntens;

out vec4 out_Color;


float expToLinearDepth(in float depth)
{
    return 2.0f * near * far / (far + near - (2.0f * depth - 1.0f) * (far - near));
}
vec3 debugcolor = vec3(0);

vec4 unprojectPos(in vec2 coord, in float depth) { 
    // depth += float(GetMaterialMask(coord, 5, GetMaterialIDs(coord))) * 0.38f;
    vec4 fragposition = in_matrix.proj_inv * vec4(coord.s * 2.0f - 1.0f, coord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
     fragposition /= fragposition.w;
    return fragposition;
}

vec4 getShadowTexcoord(in mat4 shadowMVP, in vec4 worldpos) {
    vec4 v2 = shadowMVP * worldpos;
    v2 /= v2.w;
    v2 = v2 * 0.5f + 0.5f;
    return v2;
}
float getShadow() {

    float gdistance = max((length(prop.position.xyz)-26.0f)/17.0f, 0);

    vec4 v = getShadowTexcoord(in_matrix.shadow_split0_mvp, prop.worldposition);
    vec4 v2 = getShadowTexcoord(in_matrix.shadow_split1_mvp, prop.worldposition);
    vec4 v3 = getShadowTexcoord(in_matrix.shadow_split2_mvp, prop.worldposition);

    float depth = prop.worldposition.z/prop.worldposition.w;

    // float wDistSq = v.x * v.x + v.y * v.y;
    // float dist = sqrt(wDistSq);


    float shadowFactor1 = 1;
    float shadowFactor2 = 1;
    float shadowFactor3 = 1;
    // float bias = 0.0006+clamp(tan(acos(clamp(prop.NdotL, 0, 1))), -1, 1)*0.000001f;
    // bias = clamp(bias, 0, 0.0001);
    if (clamp(v.x, 0, 1) == v.x && clamp(v.z, 0, 1) == v.z && prop.linearDepth<in_matrix.shadow_split_depth.x*0.98) {
    // v.z *= (1.0f-0.001f * gdistance);
    	v.z-=0.00004f;
		shadowFactor1 = texture(texShadow, vec3(v.xy*0.5, v.z)).r;
	    // return shadowFactor;
	    debugcolor = vec3(1-shadowFactor1,0,0);
   		return shadowFactor1;
    }
    if (clamp(v2.x, 0, 1) == v2.x && clamp(v2.z, 0, 1) == v2.z && prop.linearDepth<in_matrix.shadow_split_depth.y*0.98) {
    // v.z *= (1.0f-0.001f * gdistance);
    	v2.z-=0.00007f;
	    // debugcolor = vec3(0,1,0);
	    shadowFactor2 = texture(texShadow, vec3(v2.xy*0.5+vec2(0.5,0),v2.z)).r;
	    debugcolor = vec3(0,1-shadowFactor2,0);
		return shadowFactor2;
    }
    if (clamp(v3.x, 0, 1) == v3.x && clamp(v3.z, 0, 1) == v3.z && prop.linearDepth<in_matrix.shadow_split_depth.z*0.98) {
    // v.z *= (1.0f-0.001f * gdistance);
    	// v3.z-=bias;
    	v3.z-=0.00005f;
	    // debugcolor = vec3(0,1,0);
	    shadowFactor3 = texture(texShadow, vec3(v3.xy*0.5+vec2(0,0.5),v3.z)).r;
	    debugcolor = vec3(0,0,1-shadowFactor3);
		return shadowFactor3;
    }
    return 1;
}
void setSunSpotDens() {

    vec3 npos = normalize(prop.position.xyz);
	vec3 halfVector2 = normalize(-prop.lightVector + npos);
    prop.sunProximity = 1.0f - dot(halfVector2, npos);

    prop.sunSpotDens = clamp((prop.sunProximity-.9f)/1.3f, 0, 1);
    float extraDens = clamp(pow((prop.sunProximity-0.942f)/0.01f, 4), 0, 1);
    prop.sunSpotColor = vec4(0.9f+extraDens*0.1f, 0.9f+extraDens*0.1f, 0.8f+extraDens*0.2f, 1.0f);
}



float GetCoverage(in float coverage, in float density, in float clouds)
{
	clouds = clamp(clouds - (1.0f - coverage), 0.0f, 1.0f - density) / (1.0f - density);
	clouds = max(0.0f, clouds * 1.1f - 0.1f);
	// clouds = clouds = clouds * clouds * (3.0f - 2.0f * clouds);
	// clouds = pow(clouds, 1.0f);
	return clouds;
}
Intersection 	RayPlaneIntersectionWorld(in Ray ray, in Plane plane)
{
	float rayPlaneAngle = dot(ray.dir, plane.normal);

	float planeRayDist = 100000000.0f;
	vec3 intersectionPos = ray.dir * planeRayDist;

	if (rayPlaneAngle > 0.0001f || rayPlaneAngle < -0.0001f)
	{
		planeRayDist = dot((plane.origin), plane.normal) / rayPlaneAngle;
		intersectionPos = ray.dir * planeRayDist;
		intersectionPos = -intersectionPos;

		intersectionPos += in_matrix.cameraPosition.xyz;
	}

	Intersection i;

	i.pos = intersectionPos;
	i.d = planeRayDist;
	i.angle = rayPlaneAngle;

	return i;
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
	float xy1 = texture2D(noisetex, coord).x;
	float xy2 = texture2D(noisetex, coord2).x;
	return mix(xy1, xy2, f.z);
}

void main() {


	prop.albedo = texture(texColor, pass_texcoord).rgb;
#ifdef DO_SHADING
	prop.albedo = pow(prop.albedo, vec3(1/2.2));
    prop.albedo = pow(prop.albedo, vec3(2.2));
    prop.albedo = mix(prop.albedo, vec3(dot(prop.albedo, vec3(0.3333f))), vec3(0.15f));


	prop.timeMidnight = nightlight;
	prop.normal = texture(texNormals, pass_texcoord).rgb * 2.0f - 1.0f;
	prop.depth = texture(texDepth, pass_texcoord).r;
    prop.blockinfo = texture(texMaterial, pass_texcoord, 0);
	prop.linearDepth = expToLinearDepth(prop.depth);
    prop.position = unprojectPos(pass_texcoord, prop.depth);
    prop.worldposition = in_matrix.mv_inv * prop.position;
    prop.viewVector = normalize(prop.position.rgb);   //Gets the view vector
    prop.lightVector = normalize((in_matrix.view*mix(Light.vSun, Light.vMoon, nightlight)).xyz);
    prop.NdotL = dot( prop.normal, prop.lightVector );

    // vec4 lightsunScreen = in_matrix.mvp * vec4(Light.vSun);
    // lightsunScreen.xyz/=lightsunScreen.w;
    // lightsunScreen = normalize(lightsunScreen);

	vec3 mView = normalize((vec4(prop.viewVector, 1)*in_matrix.view).xyz);
	vec3 vSky = vec3(0);
	vec3 vMoon = vec3(0);
	if (Light.vSun.y > 0) {
		vSky = sunsky(mView, sunDirection);
	}
	if (Light.vMoon.y > 0) {
		vMoon= moonsky(mView, moonDirection);
	}

    setSunSpotDens();
    float block = prop.blockinfo.x;
  	float directShading = clamp(max(0.0f, prop.NdotL * 0.99f + 0.01f), 0, 1);
    float shadow = getShadow();
	float isSky = clamp(1.0f-prop.blockinfo.x, 0.0f, 1.0f);
	float isWater = float(block==4||block==6);
	float isLight = float(block==6);
	vec3 E = normalize(-prop.position.xyz);
	vec3 R = normalize(-reflect(prop.lightVector, prop.normal));  
	vec3 Ispec = vec3(0.6) * directShading * pow(max(dot(R,E),0.0),2.0f);
	vec3 Idiff = Light.Ld.rgb * clamp( directShading, 0.0, 1.0);     
	vec3 finalLight = vec3(0);
		finalLight = Light.La.rgb;
		finalLight += shadow * Idiff*dayLightIntens;
		finalLight += shadow * Ispec*dayLightIntens;
		finalLight *= clamp(dayLight, 0.5, 1.0f)*0.97f;
	finalLight.rg *= 1.0f-nightlight*0.4f;


	// finalLight = mix(finalLight, vSky, isSky);
	// finalLight = mix(finalLight, vec4(1,0,0,1), isWater);
	// finalLight.a = 1.0f;
	vec3 sky=mix(prop.albedo, vec3(0.04), nightlight)*0.23;
	// sky += pow(mix(vSky, vMoon, nightlight), vec3(1));
	vec3 scat = mix(vSky, vMoon, nightlight);
	float scatbr = clamp((scat.r+scat.b+scat.g) / 2.0f, 0, 1);
	sky = mix(sky, sky*scat, 0.3f);
	sky += scat*0.5f;
	sky += sky*Light.La.rgb*(1.0-scatbr)*1.1f;
	// sky += prop.sunSpotColor.rgb*prop.sunSpotDens;
	// sky += vSky*0.9f*(1-nightlight);
	vec3 terr=prop.albedo*finalLight;

	
	vec3 fogColor = vec3(0.82f, 0.82f, 0.92f);
	//distance
	float dist = length(prop.position);

    float fogFactor = clamp( (dist - 75.0f) /  429.0f, 0.0f, 0.94f );
    // terr = mix(terr, vec3(1), fogFactor);
	prop.albedo = mix(terr, sky, isSky);
	prop.albedo = mix(prop.albedo, fogColor, clamp(fogFactor-prop.sunSpotDens*1.2, 0, 0.05));

	// prop.albedo = vec4(vec3(bla), 1);
	// prop.albedo = vec4(prop.sunSpotDens, 0, 0, 1);
	// debugcolor = vec3(tan(acos(clamp(prop.NdotL, 0, 1))), 0, 0);
	// if (length(debugcolor) > 0)
	// 	prop.albedo.rgb *= debugcolor;
#endif
	out_Color = vec4(prop.albedo, 1);
}
