#version 150 core
#define DO_SHADING
#define FRAME_TIME in_matrix.frameTime

#pragma include "ubo_scene.glsl"
#pragma include "sky_scatter.glsl"

layout(std140) uniform LightInfo {
  vec4 dayLightTime; 
  vec4 posSun; // Light position in world space
  vec4 lightDir; // Light dir in world space
  vec4 La; // Ambient light intensity
  vec4 Ld; // Diffuse light intensity
  vec4 Ls; // Specular light intensity
} SkyLight;

struct Light {
    vec3 Position;
    vec3 Color;
    
    float Linear;
    float Quadratic;
    float Radius;
};

#define NR_LIGHTS 256
uniform int numLights;
uniform Light lights[NR_LIGHTS];

struct SurfaceProperties {
    vec3    albedo;                                 //Diffuse texture aka "color texture"
    vec3    normal;                                 //Screen-space surface normals
    float   depth;                                  //Scene depth
    float   linearDepth;                    //Linear depth
    float   NdotL;
    vec4    position;  // camera/eye space position
    vec4    worldposition;  // world space position
    vec3    viewVector;                     //Vector representing the viewing direction
    vec4    blockinfo;
    vec4    sunSpotColor;
    float   sunSpotDens;
    float   sunProximity;
} prop;



uniform sampler2D texColor;
uniform sampler2D texNormals;
uniform sampler2D texMaterial;
uniform sampler2D texDepth;
uniform sampler2DShadow texShadow;
uniform sampler2D texShadow2;
uniform sampler2D noisetex;


in vec2 pass_texcoord;

in float dayNoon;
in float nightNoon;
in float dayLightIntens;
in float lightAngleUp;
in float moonSunFlip;

out vec4 out_Color;


float expToLinearDepth(in float depth)
{
    return 2.0f * in_matrix.viewport.z * in_matrix.viewport.w / (in_matrix.viewport.w + in_matrix.viewport.z - (2.0f * depth - 1.0f) * (in_matrix.viewport.w - in_matrix.viewport.z));
}

vec4 unprojectPos(in vec2 coord, in float depth) { 
    vec4 fragposition = in_matrix.proj_inv * vec4(coord.s * 2.0f - 1.0f, coord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
    fragposition /= fragposition.w;
    return fragposition;
}

vec4 getShadowTexcoord(in mat4 shadowMVP, in vec4 worldpos) {
    vec4 v2 = shadowMVP * worldpos;
    v2 = v2 * 0.5f + 0.5f;
    return v2;
}
vec3 debugcolor = vec3(0);
const float clampmin = 0;//1.0/8.0;
const float clampmax = 1-clampmin;
float getShadow() {

    float gdistance = max((length(prop.position.xyz)-26.0f)/17.0f, 0);
    vec4 v = getShadowTexcoord(in_matrix.shadow_split_mvp[0], prop.worldposition);
    vec4 v2 = getShadowTexcoord(in_matrix.shadow_split_mvp[1], prop.worldposition);
    vec4 v3 = getShadowTexcoord(in_matrix.shadow_split_mvp[2], prop.worldposition);

    // float depth = prop.worldposition.z/prop.worldposition.w;
    // if (prop.linearDepth > 2) {
    // 	debugcolor = vec3(1,0,0);
    // }
    vec2 cPos = pass_texcoord*2.0-1.0;
    float dst = sqrt(cPos.x*cPos.x+cPos.y*cPos.y);
    float weight = max(0.68, 1.3-dst);
    // weight = 0.98;
	    // debugcolor = vec3(weight, 0,0);
    if (clamp(v.x, clampmin, clampmax) == v.x && clamp(v.z, clampmin, clampmax) == v.z && prop.linearDepth<in_matrix.shadow_split_depth.x*weight) {
    	v.z-=0.00004f;
	    // debugcolor = vec3(v.xy,1);
		return texture(texShadow, vec3(v.xy*0.5, v.z)).r;
    }
    if (clamp(v2.x, clampmin, clampmax) == v2.x && clamp(v2.z, clampmin, clampmax) == v2.z && prop.linearDepth<in_matrix.shadow_split_depth.y*weight) {
    // v.z *= (1.0f-0.001f * gdistance);
    	v2.z-=0.00007f;
	    // debugcolor = vec3(0,1,0);
	    return texture(texShadow, vec3(v2.xy*0.5+vec2(0.5,0),v2.z)).r;
    }
    if (clamp(v3.x, clampmin, clampmax) == v3.x && clamp(v3.z, clampmin, clampmax) == v3.z && prop.linearDepth<in_matrix.shadow_split_depth.z) {
    // v.z *= (1.0f-0.001f * gdistance);
    	// v3.z-=bias;
    	v2.z-=0.00007f;
	    // debugcolor = vec3(1,0,0);
	    return texture(texShadow, vec3(v3.xy*0.5+vec2(0,0.5),v3.z)).r;
    }
    return 1;
}

void setSunSpotDens() {
    vec3 npos = normalize(prop.position.xyz);
	vec3 halfVector2 = normalize(-SkyLight.lightDir.xyz + npos);
    prop.sunProximity = 1.0f - dot(halfVector2, npos);
    prop.sunSpotDens = clamp((prop.sunProximity-.9f)/1.3f, 0, 1);
    float extraDens = clamp(pow((prop.sunProximity-0.942f)/0.01f, 4), 0, 1);
    prop.sunSpotColor = vec4(0.9f+extraDens*0.1f, 0.9f+extraDens*0.1f, 0.8f+extraDens*0.2f, 1.0f);
}




void main() {


	prop.albedo = texture(texColor, pass_texcoord).rgb;
#ifdef DO_SHADING
	prop.albedo = pow(prop.albedo, vec3(1/2.2));
    prop.albedo = pow(prop.albedo, vec3(2.2));
    prop.albedo = mix(prop.albedo, vec3(dot(prop.albedo, vec3(0.3333f))), vec3(0.15f));


	prop.normal = texture(texNormals, pass_texcoord).rgb * 2.0f - 1.0f;
	prop.depth = texture(texDepth, pass_texcoord).r;
    prop.blockinfo = texture(texMaterial, pass_texcoord, 0);
	prop.linearDepth = expToLinearDepth(prop.depth);
    prop.position = unprojectPos(pass_texcoord, prop.depth);
    prop.worldposition = in_matrix.mv_inv * prop.position;
    prop.viewVector = normalize(in_matrix.cameraPosition.xyz - prop.worldposition.xyz);
    prop.NdotL = dot( prop.normal, SkyLight.lightDir.xyz );

    vec3 reflectDir = reflect(-SkyLight.lightDir.xyz, prop.normal);  
    float spec = pow(max(dot(prop.viewVector, reflectDir), 0.0), 2);

	vec3 skySunScat = skyAtmoScat(-prop.viewVector, SkyLight.lightDir.xyz, moonSunFlip);

    setSunSpotDens();
    float block = prop.blockinfo.x;
  	float directShading = clamp(max(0.0f, prop.NdotL * 0.99f + 0.01f), 0, 1);
    float shadow = getShadow();
	float isSky = clamp(1.0f-prop.blockinfo.x, 0.0f, 1.0f);
	float isWater = float(block==4||block==6);
	float isLight = float(block==6);


	vec3 Ispec = SkyLight.Ls.rgb * directShading * spec;
	vec3 Idiff = SkyLight.Ld.rgb * directShading;     
	vec3 finalLight = vec3(0);
	float fNight = smoothstep(0, 1, clamp(nightNoon-isLight, 0, 1));
	finalLight = SkyLight.La.rgb*(1-fNight*0.48);
	finalLight += shadow * Idiff*dayLightIntens;
	finalLight += shadow * Ispec*dayLightIntens;
	finalLight *= clamp(dayLightIntens, 0.5, 1.0)*1.0;
	finalLight.rg *= 1.0-fNight*0.43;
	finalLight *= isLight*12+1;
	finalLight *= 1.0-fNight*0.73;

    for(int i = 1; i < numLights; ++i)
    {
        // Calculate distance between light source and current fragment
        float fDist = length(lights[i].Position - prop.worldposition.xyz);
        if(fDist < lights[i].Radius)
        {
            // Diffuse
            vec3 lightDir = normalize(lights[i].Position - prop.worldposition.xyz);
            vec3 diffuse = max(dot(prop.normal, lightDir), 0.0) * SkyLight.Ld.rgb * lights[i].Color;
            // Specular
            vec3 halfwayDir = normalize(lightDir + prop.viewVector);  
            float spec = pow(max(dot(prop.normal, halfwayDir), 0.0), 32.0);
            vec3 specular = lights[i].Color * spec * SkyLight.Ls.rgb;
            // Attenuation
            float attenuation = 1.0 / (1.0 + lights[i].Linear * fDist + lights[i].Quadratic * fDist * fDist);
            diffuse *= attenuation;
            specular *= attenuation;
            finalLight += diffuse;
            finalLight += specular;
        }
    }
	// finalLight *= ;

	// finalLight = mix(finalLight, vSky, isSky);
	// finalLight = mix(finalLight, vec4(1,0,0,1), isWater);
	// finalLight.a = 1.0f;
	vec3 sky=mix(prop.albedo, vec3(0.04), fNight)*0.23;
	// sky += pow(mix(vSky, vMoon, fNight), vec3(1));

	float scatbr = clamp((skySunScat.r+skySunScat.b+skySunScat.g) / 2.0f, 0, 1);
	sky = mix(sky, sky*skySunScat, 0.3f);
	sky += skySunScat*0.5f;
	sky += sky*SkyLight.La.rgb*(1.0-scatbr)*1.1f;
	// sky += prop.sunSpotColor.rgb*prop.sunSpotDens;
	// sky += vSky*0.9f*(1-fNight);
	vec3 terr=prop.albedo*finalLight;

	
	float finalLightbr = clamp((finalLight.r+finalLight.b+finalLight.g) / 2.0f, 0, 1);
	vec3 fogColor = vec3(0.82f, 0.82f, 0.92f)*(1-fNight*0.8)*finalLightbr;
	//distance
	float dist = length(prop.position);

    float fogFactor = clamp( (dist - 35.0f) /  1429.0f, 0.0f, 0.94f );
	prop.albedo = mix(terr, sky, isSky);
	prop.albedo = mix(prop.albedo, fogColor, clamp(fogFactor-prop.sunSpotDens*1.2, 0, 0.05));

	if (length(debugcolor) > 0) {
		prop.albedo = mix(prop.albedo, debugcolor, 0.3);
	}

#endif
	out_Color = vec4(prop.albedo, 1);
}
