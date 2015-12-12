#version 150 core
#define DO_SHADING

#pragma include "ubo_scene.glsl"
#pragma include "blockinfo.glsl"
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
uniform sampler2D texShadow2;
uniform sampler2D texLight;
uniform sampler2D noisetex;
uniform sampler2D texAO;


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
    v2 = v2 * 0.5 + 0.5;
    return v2;
}
vec3 debugcolor = vec3(0);
const float clampmin = 0;//1.0/8.0;
const float clampmax = 1-clampmin;
bool canLookup(in vec4 v, in float zPos, in float mapZ) {
    return clamp(v.x, clampmin, clampmax) == v.x && clamp(v.z, clampmin, clampmax) == v.z && zPos < mapZ;
}
#define SAMPLE_DISTANCE ((1.0/2048.0) / 4.0)
#define SOFT_SHADOW_TAP_RANGE 1
#define SOFT_SHADOW_WEIGHT ((SOFT_SHADOW_TAP_RANGE*2+1)*(SOFT_SHADOW_TAP_RANGE*2+1))
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
        // debugcolor = vec3(1,0,0);
        // float s = 0;
        // for (int x = -SOFT_SHADOW_TAP_RANGE; x <= SOFT_SHADOW_TAP_RANGE; x++) {
        //     for (int y = -SOFT_SHADOW_TAP_RANGE; y <= SOFT_SHADOW_TAP_RANGE; y++) {
        //         vec2 offs = vec2(x, y) * SAMPLE_DISTANCE;
        //         s += texture(texShadow, vec3(v3.xy*0.5+vec2(0,0.5)+offs,v3.z));   
        //     }
        // }
        // s /= SOFT_SHADOW_WEIGHT;
        // return s;
        shadow.z = texture(texShadow, vec3(v3.xy*0.5+vec2(0,0.5), v3.z));
        shadow.w += 1;
        return shadow.z;
    }
    return 1;
}

float getShadow2() {

    float gdistance = max((length(prop.position.xyz)-26.0f)/17.0f, 0);
    vec4 v = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[0], prop.worldposition);
    vec4 v2 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[1], prop.worldposition);
    vec4 v3 = getShadowTexcoord(in_matrix_shadow.shadow_split_mvp[2], prop.worldposition);
    vec2 cPos = pass_texcoord*2.0-1.0;
    float dst = sqrt(cPos.x*cPos.x+cPos.y*cPos.y);
    float weight = max(0.68, 1.3-dst);
    vec4 mapZSplits = in_matrix_shadow.shadow_split_depth;
    vec4 shadow = vec4(1);
    shadow.xyz = vec3(0.5);
    if (canLookup(v, prop.linearDepth, mapZSplits.x*weight)) {
        v.z*=0.9997;
        // shadow.x = texture(texShadow2, v.xy*0.5).r;
        // shadow.w += 1;
        // return v.z > shadow.x ? 0 : 1;
        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE; x <= SOFT_SHADOW_TAP_RANGE; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE; y <= SOFT_SHADOW_TAP_RANGE; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE;
                s += texture(texShadow2, v.xy*0.5+offs).r;   
            }
        }
        s /= SOFT_SHADOW_WEIGHT;
        // shadow.x = v.z > s ? 0 : 1;
        // shadow.w += 1;
        return v.z > s ? 0 : 1;
    }
    if (canLookup(v2, prop.linearDepth, mapZSplits.y*weight)) {
        v2.z*=0.9999;
        // shadow.y = texture(texShadow2, v2.xy*0.5+vec2(0.5,0)).r;
        // shadow.w += 1;
        // return v2.z > shadow.y ? 0 : 1;
        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE; x <= SOFT_SHADOW_TAP_RANGE; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE; y <= SOFT_SHADOW_TAP_RANGE; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE;
                s += texture(texShadow2, v2.xy*0.5+vec2(0.5,0)+offs).r;   
            }
        }
        s /= SOFT_SHADOW_WEIGHT;
        // shadow.y += v2.z > s ? 0 : 1;
        // shadow.w += 1;
        return v2.z > s ? 0 : 1;
    }
    if (canLookup(v3, prop.linearDepth, mapZSplits.z)) {
        v3.z*=0.9997;
        float s = 0;
        for (int x = -SOFT_SHADOW_TAP_RANGE; x <= SOFT_SHADOW_TAP_RANGE; x++) {
            for (int y = -SOFT_SHADOW_TAP_RANGE; y <= SOFT_SHADOW_TAP_RANGE; y++) {
                vec2 offs = vec2(x, y) * SAMPLE_DISTANCE;
                s += texture(texShadow2, v3.xy*0.5+vec2(0,0.5)+offs).r;   
            }
        }
        s /= SOFT_SHADOW_WEIGHT;
        // shadow.z = v3.z > s ? 0 : 1;
        // shadow.w += 1;
        // shadow.z = texture(texShadow2, v3.xy*0.5+vec2(0,0.5)).r;
        // shadow.w += 1;
        return v3.z > s ? 0 : 1;
    }
    // return (shadow.x+shadow.y+shadow.z) / shadow.w;
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

    vec4 sceneColor = texture(texColor, pass_texcoord);
	prop.albedo = sceneColor.rgb;
    vec4 ssao = texture(texAO, pass_texcoord);
#ifndef DO_SHADING
#else


	// prop.albedo = pow(prop.albedo, vec3(1/1.1));
 //    prop.albedo = pow(prop.albedo, vec3(2.2));
    float lum = dot(prop.albedo, vec3(0.3333f));
    // prop.albedo = mix(prop.albedo, vec3(lum), vec3(0.75f));

    // prop.albedo = pow(prop.albedo, vec3(1.1+lum*0.3));


    vec4 nl = texture(texNormals, pass_texcoord);
	prop.normal = nl.rgb * 2.0f - 1.0f;
    prop.light = texture(texLight, pass_texcoord, 0);
	prop.depth = texture(texDepth, pass_texcoord).r;
    prop.blockinfo = texture(texMaterial, pass_texcoord, 0);
    prop.linearDepth = expToLinearDepth(prop.depth);
    prop.position = unprojectPos(pass_texcoord, prop.depth);
    prop.worldposition = in_matrix_3D.mv_inv * prop.position;
    prop.viewVector = normalize(CAMERA_POS - prop.worldposition.xyz);
    prop.NdotL = dot( prop.normal, SkyLight.lightDir.xyz );
    
    float sunTheta = max( dot(-prop.viewVector, normalize(SkyLight.lightDir.xyz)), 0.0 );
    prop.sunSpotDens = pow(sunTheta, 32.0)*1;
    uint blockid = BLOCK_ID(prop.blockinfo);
    float renderpass = BLOCK_RENDERPASS(prop.blockinfo);

    float isSky = IS_SKY(blockid);
    float isWater = IS_WATER(blockid);
    float isLight = IS_LIGHT(blockid);
    float isIllum = float(renderpass==4);
    float isBackface = float(renderpass==3);
    float isEntity = float(renderpass==5);
    // float isFlower = float(blockid>=48u);
    float alpha = mix(1, sceneColor.a, float(pass));
    if (pass > 0) {
        if(renderpass != 1)
            discard;
        if(sceneColor.a < 0.1)
            discard;
    }
    float roughness = 1.3+isWater*40;

    const vec3 ambLight1 = normalize(vec3(50, 100, 50));
    const vec3 ambLight2 = normalize(vec3(-50, -70, -50));
    float NdotLAmb1 = max(dot( prop.normal, ambLight1 ), 0);
    float NdotLAmb2 = max(dot( prop.normal, ambLight2 ), 0);
    vec3 reflectDirAmb1 = (reflect(-ambLight1, prop.normal));  
    vec3 reflectDirAmb2 = (reflect(-ambLight2, prop.normal));  
    float specAmb1 = pow(max(dot(prop.viewVector, reflectDirAmb1), 0.0), 2);
    float specAmb2 = pow(max(dot(prop.viewVector, reflectDirAmb2), 0.0), 2);

    vec3 reflectDir = (reflect(-SkyLight.lightDir.xyz, prop.normal));  
    float spec = pow(max(dot(prop.viewVector, reflectDir), 0.0), roughness);
    float theta = max(dot(prop.viewVector, prop.normal), 0.0);
    float minRefl = 0.02;
    float amtRefl = minRefl + (1.0 - minRefl) * pow(1.0 - theta, 5.0);

    vec3 skySunScat = skyAtmoScat(-prop.viewVector, SkyLight.lightDir.xyz, moonSunFlip);


    // if (prop.sunSpotDens < 0.7) {
    //      out_Color = vec4(vec3(0), 1);
    //     return;
    // }
    float fNight = smoothstep(0, 1, clamp(nightNoon-isLight, 0, 1));
    float skyLightLvl = prop.light.x;
    float blockLightLvl = prop.light.y;
    float occlusion = min(prop.light.z, ssao.r);
    occlusion+=isWater;
    occlusion = min(1, occlusion);
    // skyLightLvl-=isWater;
    // skyLightLvl = max(0, skyLightLvl);
    // blockLightLvl-=isWater;
    // blockLightLvl = max(0, blockLightLvl);
    float shadow = getShadow2()*(1-isBackface);
    // float shadow = mix(getSoftShadow(), 1, 0.04);
  	float nDotL = clamp(max(0.0f, prop.NdotL * 0.99f + 0.01f), 0, 1);
    float sunLight = skyLightLvl * nDotL * shadow * dayLightIntens;
    sunLight = max(shadow*0.05, sunLight);
    // sunLight = max(occlusion*0.124, sunLight);
    sunLight = mix(sunLight, sunLight*occlusion, 1);
    sunLight = max(0, sunLight);
    
    // float sunLight = skyLightLvl * nDotL * max(shadow, 0.04) * dayLightIntens;
    // sunLight*=10;


    // sunLight *= (1-blockLightLvl); //TODO: test + remove
    // sunLight *= (1-blockLightLvl); //TODO: test + remove


    // sunLight += 0.6;
    // prop.albedo*=mix(prop.light.z, 1, clamp(isSky+nDotL+(1-prop.light.x)*0.3,0,1));

    float blockLight = (1-pow(1-blockLightLvl,0.05))*1.1;
    vec3 lightColor = mix(vec3(1), vec3(1.0)*0.02, fNight);
    vec3 lightColor2 = mix(vec3(1), vec3(0.56, 0.56, 1.0)*0.005, fNight);
	vec3 Ispec = SkyLight.Ls.rgb * lightColor * nDotL * spec;
    vec3 Idiff = SkyLight.Ld.rgb * lightColor2 * nDotL;
    vec3 Iamb = SkyLight.La.rgb * lightColor * mix(((NdotLAmb1+NdotLAmb2)*0.5f), 1.2, isEntity*0.8);
    // if (isFlower == 1) {
    //     Idiff *= 0.0;
    //     // Ispec *= 0.2;
    //     // Iamb *= 0.2;
    // }
    // vec3 Iamb = SkyLight.La.rgb * lightColor * ((NdotLAmb1+NdotLAmb2)*0.5f);
    vec3 finalLight = vec3(0);
    finalLight += Iamb * (0.04+occlusion*(1-0.04)) * (0.04+skyLightLvl*(1-0.04));
    // finalLight += NdotLAmb1 * SkyLight.Ld.rgb * lightColor2 * 0.02;
    // finalLight += NdotLAmb2 * SkyLight.Ld.rgb * lightColor2 * 0.02;
    // finalLight += vec3(0.663)* (1.0-isLight*0.7)* (mix(1, occlusion, 0.79)) * blockLight;
    // finalLight += vec3(1, 0.9, 0.7)*0.363* (1.0-isLight*0.7)* (mix(1, occlusion, 0.79)) * blockLight;
    // lum = (clamp(pow(0.6+lum, 3)-1, 0, 1)+0.33)*isLight*0.82;
    finalLight += lum* (mix(1, occlusion, 0.19)) * blockLight*isLight*0.6;
    float fl=blockLightLvl/15.0f;
    finalLight += Ispec * sunLight;
    finalLight += Idiff * sunLight;
    const float blockLightConst = 60;
    finalLight+=isIllum*4;
    finalLight += vec3(1, 0.9, 0.7) * pow(blockLightLvl/8.0,2)*((1.0-isLight*0.8)*blockLightConst);
    // finalLight *= mix(1, 0.75,isWater);
    finalLight *= max(0.3+ssao.r*0.7, isWater);
    // finalLight *= ssao.r;
    // fina
    alpha = clamp(alpha+float(pass)*0.2*(1-clamp(sunLight, 0, 1)), 0, 1);
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


	vec3 sky=mix(prop.albedo, vec3(0.04), fNight)*0.23;

	float scatbr = clamp((skySunScat.r+skySunScat.b+skySunScat.g) / 2.0f, 0, 1);
	sky = mix(sky, sky*skySunScat, 0.3f);
	sky += skySunScat*prop.sunSpotDens*1.2;
	sky += sky*SkyLight.La.rgb*(1.0-prop.sunSpotDens)*1.1f;
    sky *= 0.4;
    vec3 terr=prop.albedo*finalLight;
    spec*=shadow;//0.6+(shadow*0.1+sunLight*0.3);
    vec3 waterAlb = mix(prop.albedo*finalLight, spec*vec3(0.1), isWater*theta);
    terr = mix (terr, waterAlb, isWater);

    
    vec3 fogColor = mix(vec3(0.5,0.6,0.7), vec3(0.5,0.6,0.7)*0.2, clamp(nightNoon, 0, 1));
    float dist = length(prop.position);
    dist = min(dist, in_scene.viewport.w/6);
    dist = max(dist-46, 0);
    // float fogFactor = clamp( (dist - 135.0f) /  344.0f, 0.0f, 0.94f );
    // terr = applyFog(terr, 20, CAMERA_POS, prop.worldposition.xyz-CAMERA_POS);
    // float fogAmount = getFogDensity(dist, CAMERA_POS, prop.viewVector);
    float fogAmount = clamp(1.0 - exp( -dist*0.00004 ), 0, 1);
    // vec3  fogColor  = vec3(0.5,0.6,0.7);

    // terr =  mix( terr, fogColor, fogAmount );
    prop.albedo = mix(terr, sky, isSky);
    prop.albedo =  mix( prop.albedo, fogColor, fogAmount );
    // if (isEntity == 1) {
    //     if (length(prop.normal) < 0.8) {
    //         prop.albedo = vec3(1,0,0);
    //     }
    // }
    // prop.albedo = mix(prop.albedo, fogged, clamp(1-prop.sunSpotDens*1.2, 0, 0.55));

    // if (length(debugcolor) > 0) {
    //  prop.albedo = mix(prop.albedo, debugcolor, 0.3);
    // }
#endif
    out_Color = vec4(prop.albedo, alpha);
}
