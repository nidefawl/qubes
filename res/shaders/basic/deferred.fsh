#version 150 core
#define DO_SHADING
#define FRAME_TIME in_scene.frameTime

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

#define NR_LIGHTS 64
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
    uvec4    blockinfo;
    float   sunSpotDens;
    vec4   light;
} prop;

uniform int pass;


uniform sampler2D texColor;
uniform sampler2D texNormals;
uniform usampler2D texMaterial;
uniform sampler2D texDepth;
uniform sampler2DShadow texShadow;
uniform sampler2D texLight;
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
    return 2.0f * in_scene.viewport.z * in_scene.viewport.w / (in_scene.viewport.w + in_scene.viewport.z - (2.0f * depth - 1.0f) * (in_scene.viewport.w - in_scene.viewport.z));
}

vec4 unprojectPos(in vec2 coord, in float depth) { 
    vec4 fragposition = in_matrix_3D.proj_inv * vec4(coord.s * 2.0f - 1.0f, coord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
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
bool canLookup(in vec4 v, in float zPos, in float mapZ) {
    return clamp(v.x, clampmin, clampmax) == v.x && clamp(v.z, clampmin, clampmax) == v.z && zPos < mapZ;
}
float getShadow() {

    float gdistance = max((length(prop.position.xyz)-26.0f)/17.0f, 0);
    vec4 v = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[0], prop.worldposition);
    vec4 v2 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[1], prop.worldposition);
    vec4 v3 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[2], prop.worldposition);

    // float depth = prop.worldposition.z/prop.worldposition.w;
    // if (prop.linearDepth > 2) {
    // 	debugcolor = vec3(1,0,0);
    // }
    vec2 cPos = pass_texcoord*2.0-1.0;
    float dst = sqrt(cPos.x*cPos.x+cPos.y*cPos.y);
    float weight = max(0.68, 1.3-dst);
    vec4 mapZSplits = in_matrix_shadow.shadow_split_depth;
    vec4 shadow = vec4(1);
    if (canLookup(v, prop.linearDepth, mapZSplits.x*weight)) {
        v.z-=0.00004f;
        shadow.x = texture(texShadow, vec3(v.xy*0.5, v.z));
        shadow.w += 1;
        return shadow.x;
    }
    if (canLookup(v2, prop.linearDepth, mapZSplits.y*weight)) {
        v2.z-=0.00007f;
        shadow.y = texture(texShadow, vec3(v2.xy*0.5+vec2(0.5,0), v2.z));
        shadow.w += 1;
        return shadow.y;
    }
    if (canLookup(v3, prop.linearDepth, mapZSplits.z)) {
        v2.z-=0.00007f;
        shadow.z = texture(texShadow, vec3(v3.xy*0.5+vec2(0,0.5), v3.z));
        shadow.w += 1;
        return shadow.z;
    }
    return 1;
}

float getSoftShadow() {

    float gdistance = max((length(prop.position.xyz)-26.0f)/17.0f, 0);
    vec4 v = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[0], prop.worldposition);
    vec4 v2 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[1], prop.worldposition);
    vec4 v3 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[2], prop.worldposition);

    // float depth = prop.worldposition.z/prop.worldposition.w;
    // if (prop.linearDepth > 2) {
    //  debugcolor = vec3(1,0,0);
    // }
    vec2 cPos = pass_texcoord*2.0-1.0;
    float dst = sqrt(cPos.x*cPos.x+cPos.y*cPos.y);
    float weight = max(0.68, 1.3-dst);
    #define SAMPLE_DISTANCE ((1.0/2048.0) / 4.0)
    #define SOFT_SHADOW_TAP_RANGE 1
    #define SOFT_SHADOW_WEIGHT ((SOFT_SHADOW_TAP_RANGE*2+1)*(SOFT_SHADOW_TAP_RANGE*2+1))*0.4
    // weight = 0.98;
        // debugcolor = vec3(weight, 0,0);
    if (clamp(v.x, clampmin, clampmax) == v.x && clamp(v.z, clampmin, clampmax) == v.z && prop.linearDepth<in_matrix_shadow.shadow_split_depth.x*weight) {
        v.z-=0.00004f;
        // debugcolor = vec3(v.xy,1);
        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE; x <= SOFT_SHADOW_TAP_RANGE; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE; y <= SOFT_SHADOW_TAP_RANGE; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE;
                s += texture(texShadow, vec3(v.xy*0.5+offs, v.z));        
            }
        }
        s /= SOFT_SHADOW_WEIGHT;
        return s;
    }
    if (clamp(v2.x, clampmin, clampmax) == v2.x && clamp(v2.z, clampmin, clampmax) == v2.z && prop.linearDepth<in_matrix_shadow.shadow_split_depth.y*weight) {
    // v.z *= (1.0f-0.001f * gdistance);
        v2.z-=0.00007f;
        // debugcolor = vec3(0,1,0);
        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE; x <= SOFT_SHADOW_TAP_RANGE; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE; y <= SOFT_SHADOW_TAP_RANGE; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE;
                s += texture(texShadow, vec3(v2.xy*0.5+vec2(0.5,0)+offs, v2.z));        
            }
        }
        s /= SOFT_SHADOW_WEIGHT;
        return s;
    }
    if (clamp(v3.x, clampmin, clampmax) == v3.x && clamp(v3.z, clampmin, clampmax) == v3.z && prop.linearDepth<in_matrix_shadow.shadow_split_depth.z) {
    // v.z *= (1.0f-0.001f * gdistance);
        // v3.z-=bias;
        v2.z-=0.00007f;
        // debugcolor = vec3(1,0,0);
        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE; x <= SOFT_SHADOW_TAP_RANGE; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE; y <= SOFT_SHADOW_TAP_RANGE; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE;
                s += texture(texShadow, vec3(v3.xy*0.5+vec2(0,0.5)+offs,v3.z));   
            }
        }
        s /= SOFT_SHADOW_WEIGHT;
        return s;

        // return texture(texShadow, vec3(v3.xy*0.5+vec2(0,0.5),v3.z));   
    }
    return 1;
}



vec3 applyFoga(vec3 albedo, float dist, vec3 rayOrigin, vec3 rayDirection){
    float fogDensity = 0.00006;
    float vFalloff = 20.0;
    vec3 fogColor = vec3(0.88, 0.92, 0.999);
    float fog = exp((-rayOrigin.y*vFalloff)*fogDensity) * (1.0-exp(-dist*rayDirection.y*vFalloff*fogDensity))/(rayDirection.y*vFalloff);
    return mix(albedo, fogColor, clamp(fog, 0.0, 1.0));
}


void main() {


	prop.albedo = texture(texColor, pass_texcoord).rgb;
    float alpha = 1.0f;
#ifdef DO_SHADING


	// prop.albedo = pow(prop.albedo, vec3(1.4));
 //    prop.albedo = pow(prop.albedo, vec3(2.2));
    float lum = dot(prop.albedo, vec3(0.3333f));
    prop.albedo = mix(prop.albedo, vec3(lum), vec3(0.05f));

    vec4 nl = texture(texNormals, pass_texcoord);
	prop.normal = nl.rgb * 2.0f - 1.0f;
    prop.light = texture(texLight, pass_texcoord, 0);
	prop.depth = texture(texDepth, pass_texcoord).r;
    prop.blockinfo = texture(texMaterial, pass_texcoord, 0);
    prop.linearDepth = expToLinearDepth(prop.depth);
    prop.position = unprojectPos(pass_texcoord, prop.depth);
    prop.worldposition = in_matrix_3D.mv_inv * prop.position;
    prop.viewVector = normalize(in_scene.cameraPosition.xyz - prop.worldposition.xyz);
    prop.NdotL = dot( prop.normal, SkyLight.lightDir.xyz );
    float sunTheta = max( dot(-prop.viewVector, normalize(SkyLight.lightDir.xyz)), 0.0 );
    prop.sunSpotDens = pow(sunTheta, 32.0)*1;

    float isSky = float(prop.blockinfo.y==0u);
    float isWater = float(prop.blockinfo.y==4u);
    float isLight = float(prop.blockinfo.y==6u);
    if (pass == 1 && isWater < 1) {
        discard;
    }
#ifndef DO_SOMETHING_AWEFUL
    alpha -= float(pass)*.3f;

    vec3 reflectDir = (reflect(-SkyLight.lightDir.xyz, prop.normal));  
    float roughness = 1.4+isWater*100;
    float spec = pow(max(dot(prop.viewVector, reflectDir), 0.0), roughness);
    float theta = max(dot(prop.viewVector, prop.normal), 0.0);
    float minRefl = 0.02;
    float amtRefl = minRefl + (1.0 - minRefl) * pow(1.0 - theta, 5.0);

    vec3 skySunScat = skyAtmoScat(-prop.viewVector, SkyLight.lightDir.xyz, moonSunFlip);
    vec3 skySunScat2 = skyAtmoScat(-prop.viewVector, SkyLight.lightDir.xyz, moonSunFlip);


    // if (prop.sunSpotDens < 0.7) {
    //      out_Color = vec4(vec3(0), 1);
    //     return;
    // }
    float fNight = smoothstep(0, 1, clamp(nightNoon-isLight, 0, 1));
    float skyLightLvl = prop.light.x;
    float blockLightLvl = prop.light.y;
    float occlusion = prop.light.z;
    float shadow = getShadow();
    // float shadow = mix(getSoftShadow(), 1, 0.04);
  	float nDotL = clamp(max(0.0f, prop.NdotL * 0.99f + 0.01f), 0, 1);
    float sunLight = skyLightLvl * nDotL * shadow * dayLightIntens;
    float sunLightMin = skyLightLvl * nDotL * max(shadow, 0.04) * dayLightIntens;
    // sunLight*=10;
    sunLight *= (1-blockLightLvl); //TODO: test + remove
    sunLightMin *= (1-blockLightLvl); //TODO: test + remove
    // sunLight += 0.6;
    // prop.albedo*=mix(prop.light.z, 1, clamp(isSky+nDotL+(1-prop.light.x)*0.3,0,1));

    float blockLight = (1-pow(1-blockLightLvl,0.35));
    vec3 lightColor = mix(vec3(1), vec3(1.0)*0.05, fNight);
    vec3 lightColor2 = mix(vec3(1), vec3(0.56, 0.56, 1.0)*0.05, fNight);
	vec3 Ispec = SkyLight.Ls.rgb * lightColor * nDotL * spec;
    vec3 Idiff = SkyLight.Ld.rgb * lightColor2 * nDotL;
    vec3 Iamb = SkyLight.La.rgb * lightColor;

    vec3 finalLight = vec3(0);
    finalLight += Iamb * mix(occlusion, 1, 0.1) * (skyLightLvl+0.25);
    // finalLight += vec3(0.663)* (1.0-isLight*0.7)* (mix(1, occlusion, 0.79)) * blockLight;
    finalLight += vec3(1, 0.9, 0.7)*0.363* (1.0-isLight*0.7)* (mix(1, occlusion, 0.79)) * blockLight;
    lum = (clamp(pow(0.6+lum, 3)-1, 0, 1)+0.33)*isLight*0.82;
    finalLight += lum* (mix(1, occlusion, 0.19)) * blockLight;
    // finalLight += clamp((1-lum), 0, 1)*vec3(0,1,4.3)*isLight;
    finalLight += Ispec * sunLight;
    finalLight += Idiff * sunLightMin;
    // finalLight *= occlusion;

    alpha += float(pass)*0.2*(1-clamp(sunLight, 0, 1));
    // finalLight = mix (finalLight, vec3(1), lum*);


	// finalLight += SkyLight.La.rgb*prop.light.z*(1-clamp(fNight*0.48, 0, 1));
	// finalLight += shadow * Idiff*dayLightIntens*(1-fNight*0.78);
	// finalLight += shadow * Ispec*dayLightIntens;
	// finalLight *= clamp(dayLightIntens, 0.5, 1.0)*1.0;
	// finalLight.rg *= 1.0-fNight*0.23;
	// finalLight *= 1.0-fNight*0.73;
 //    finalLight = mix(finalLight, vec3(0.56), prop.light.y);

    for(int i = 0; i < numLights; ++i)
    {
        // Calculate distance between light source and current fragment
        float fDist = length(lights[i].Position - prop.worldposition.xyz);
        if(fDist < lights[i].Radius && occlusion > 0) //executed anyways
        {
            // Diffuse

            vec3 colorLight = clamp(lights[i].Color, vec3(0), vec3(12));
            vec3 lightDir = normalize(lights[i].Position - prop.worldposition.xyz);
            vec3 diffuse = max(dot(prop.normal, lightDir), 0.0) * SkyLight.Ld.rgb * colorLight;
            // Specular
            vec3 halfwayDir = normalize(lightDir + prop.viewVector);  
            float spec = max(pow(max(dot(prop.normal, halfwayDir), 0.0), 1.4), 0.0);
            vec3 specular = colorLight * spec * SkyLight.Ls.rgb * 2.2;
            // Attenuation
            float attenuation = 1.0 / (1.0 + lights[i].Linear * fDist + lights[i].Quadratic * fDist * fDist);
            diffuse *= attenuation * occlusion;
            specular *= attenuation * occlusion;
            finalLight += diffuse;
            finalLight += specular;
        }
    }
#endif


	vec3 sky=mix(prop.albedo, vec3(0.04), fNight)*0.23;

	float scatbr = clamp((skySunScat.r+skySunScat.b+skySunScat.g) / 2.0f, 0, 1);
	sky = mix(sky, sky*skySunScat, 0.3f);
	sky += skySunScat*0.5f;
	sky += sky*SkyLight.La.rgb*(1.0-scatbr)*1.1f;
	
    vec3 terr=prop.albedo*finalLight;
    spec*=shadow;//0.6+(shadow*0.1+sunLight*0.3);
    vec3 waterAlb = mix(prop.albedo*finalLight, vec3(0.)+spec*vec3(0.9), isWater*theta);
    terr = mix (terr, waterAlb, isWater);
	float finalLightbr = clamp((finalLight.r+finalLight.b+finalLight.g) / 2.0f, 0, 1);

    
    vec3 fogColor = mix(vec3(0.5,0.6,0.7), vec3(0.5,0.6,0.7)*0.2, clamp(nightNoon, 0, 1));
    float dist = length(prop.position);

    // float fogFactor = clamp( (dist - 135.0f) /  344.0f, 0.0f, 0.94f );
    // terr = applyFog(terr, 20, in_scene.cameraPosition.xyz, prop.worldposition.xyz-in_scene.cameraPosition.xyz);
    // float fogAmount = getFogDensity(dist, in_scene.cameraPosition.xyz, prop.viewVector);
    float fogAmount = clamp(1.0 - exp( -dist*0.0005 ), 0, 1);
    // vec3  fogColor  = vec3(0.5,0.6,0.7);

    terr =  mix( terr, fogColor, fogAmount*0.52 );
    prop.albedo = mix(terr, sky, isSky);

    // prop.albedo = mix(prop.albedo, fogged, clamp(1-prop.sunSpotDens*1.2, 0, 0.55));

    // if (length(debugcolor) > 0) {
    //  prop.albedo = mix(prop.albedo, debugcolor, 0.3);
    // }
#else 
    prop.albedo *= 0.25;
#endif
    out_Color = vec4(prop.albedo, alpha);
}
