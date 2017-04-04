#version 430

#pragma include "ubo_scene.glsl"
#pragma include "unproject.glsl"


#define WORK_GROUP_SIZE 32
#define MAX_LIGHTS_PER_TILE 1024 
#define MAX_LIGHTS 1024
#define EXTEND_RADIUS 1.05f
#define LIGHT_CUTOFF 0.01f


struct SurfaceProperties {
    vec3    albedo;                                 //Diffuse texture aka "color texture"
    vec3    normal;                                 //Screen-space surface normals
    float   depth;                                  //non-linear depth
    vec4    position;  // camera/eye space position
    vec4    worldposition;  // world space position
    vec3    viewVector;                     //Vector representing the viewing direction
} prop;

struct PointLight
{
    vec4 position;
    vec4 color;
    float intensity;
    float radius;
    float quadratic;
    float padding1;
    float padding2;
    float padding3;
    float padding4;
    float padding5;
};


layout (set = 2, binding = 0, std140) buffer PointLightStorageBuffer
{
    PointLight pointLights[];
};
layout (local_size_x = WORK_GROUP_SIZE, local_size_y = WORK_GROUP_SIZE, local_size_z = 1) in;



#ifdef VULKAN_GLSL

// layout (set = 1, binding = 0, rgba16f) readonly uniform highp image2D geometryNormal;
layout (set = 1, binding = 0) uniform sampler2D geometryNormal;
layout (set = 1, binding = 1) uniform sampler2D depthBuffer;
layout (set = 1, binding = 2, rgba16f) writeonly uniform highp image2D finalImage;

layout(push_constant) uniform PushConstantsLightCompute {
  int numActiveLights;
} pushCLightCompute;
#define ACTIVE_LIGHTS pushCLightCompute.numActiveLights
#else

layout (binding = 0, rgba16f) readonly uniform highp image2D geometryNormal;
layout (binding = 1) uniform sampler2D depthBuffer;
layout (binding = 5, rgba16f) writeonly uniform highp image2D finalImage;

uniform int numActiveLights;
#define ACTIVE_LIGHTS numActiveLights
#endif

shared uint minDepth;
shared uint maxDepth;
shared uint pointLightCount;
shared uint pointLightIndex[MAX_LIGHTS];


void buildFrustum(inout vec4 frustumPlanes[6], in vec2 wrkGrp, in float minZ, in float maxZ) {
    vec2 resolution = in_scene.viewport.xy;
    mat4 projection = in_matrix_3D.p;

    float sc1=2;
    float sc2=1;
    vec2 tileScale = vec2(resolution.xy) / (sc2 * vec2(WORK_GROUP_SIZE, WORK_GROUP_SIZE));
    vec2 wrkGrpOffset = wrkGrp*2+1;
    vec2 tileBias = (tileScale) - wrkGrpOffset;

    // vec2 tileScale = vec2(resolution.xy) / (2 * vec2(WORK_GROUP_SIZE, WORK_GROUP_SIZE));
    // vec2 tileBias = (tileScale) - (wrkGrp);

    // Left/Right/Bottom/Top
    vec4 col1 = vec4(projection[0][0] * tileScale.x, projection[0][1], tileBias.x, projection[0][3]);
    vec4 col2 = vec4(projection[1][0], projection[1][1] * tileScale.y, tileBias.y, projection[1][3]);
    vec4 col4 = vec4(projection[3][0], projection[3][1], -1.0, projection[3][3]);
    frustumPlanes[0] = col4 + col1;
    frustumPlanes[1] = col4 - col1;
    frustumPlanes[2] = col4 - col2;
    frustumPlanes[3] = col4 + col2;

    // Near/Far
    frustumPlanes[4] = vec4(0, 0, -1, minZ);
    frustumPlanes[5] = vec4(0, 0, 1, -maxZ);

    for (uint i = 0; i < 4; ++i)
        frustumPlanes[i] /= length(frustumPlanes[i].xyz);

}

void main()
{
    minDepth = 0xfF7FFFFF;
    maxDepth = 0x7f7fffff;
    pointLightCount = 0;
    barrier();
    groupMemoryBarrier();
    vec3 camPos = vec3(0, 0, 0);
    vec2 resolution = in_scene.viewport.xy;
    ivec2 iResolution = ivec2(resolution) - ivec2(1);
    uvec2 workGroupPixelOffset = gl_WorkGroupID.xy * gl_WorkGroupSize.xy;

    ivec2 pixelPos = ivec2(resolution.x-1-gl_GlobalInvocationID.x, resolution.y-1-gl_GlobalInvocationID.y);
    vec2 pass_texcoord = pixelPos / resolution;
    // vec4 nl = imageLoad(geometryNormal, pixelPos);
    vec4 nl = texelFetch(geometryNormal, pixelPos, 0);
    prop.normal = nl.rgb * 2.0f - 1.0f;
    prop.depth = texelFetch(depthBuffer, pixelPos, 0).r;
    vec4 curScreenPos = screencoord(pass_texcoord.st, prop.depth);
    prop.position = unprojectScreenCoord(curScreenPos);
    prop.worldposition = in_matrix_3D.mv_inv * prop.position;
    prop.viewVector = normalize(CAMERA_POS - prop.worldposition.xyz);

    uint depth = floatBitsToUint(prop.position.z);

#if Z_INVERSE
    if (isinf(prop.depth)) {
        depth = -1024;//TODO: increase _AND TEST_
    }
#endif
    bool properDepth = prop.depth>0;
    if (properDepth) {
        atomicMin(minDepth, depth);
        atomicMax(maxDepth, depth);
    }
    barrier();

        float minDepthZ = uintBitsToFloat(minDepth);
        float maxDepthZ = uintBitsToFloat(maxDepth);
    #ifdef DEBUG_LIGHT
        if (IS_DEBUG_FRAG(vec2(pass_texcoord.x, 1.0-pass_texcoord.y))) {
            debugBuf.debugVals[0] = minDepthZ;
            debugBuf.debugVals[1] = maxDepthZ;

        }
    #endif

    vec4 frustumPlanes[6];
    buildFrustum(frustumPlanes, vec2(gl_WorkGroupID.xy), minDepthZ, maxDepthZ);


    uint lightIndex = gl_LocalInvocationIndex;
    if (lightIndex < ACTIVE_LIGHTS) {
        PointLight p = pointLights[lightIndex];
        vec4 pos = in_matrix_3D.mv * vec4(p.position.xyz, 1);
        float rad = p.radius*(EXTEND_RADIUS);

        if (pointLightCount < MAX_LIGHTS_PER_TILE)
        {
            bool inFrustum = true;
            for (int i = 0; i < 6; ++i)
            {
                float d = dot(frustumPlanes[i], vec4(pos.xyz, 1.0));
                inFrustum = inFrustum && (d >= -rad);
            }

            if (inFrustum)
            {
                uint id = atomicAdd(pointLightCount, 1);
                pointLightIndex[id] = lightIndex;
            }
        }
    }

    barrier();
    vec3 finalLight = vec3(0);
    if (properDepth) 
    {
        float fDist = 0;
        for(int i = 0; i < pointLightCount; ++i)
        {
            uint idx = pointLightIndex[i];
            if (idx >= ACTIVE_LIGHTS) {
                continue;
            }
            PointLight p = pointLights[idx];

            vec3 lightRay = p.position.xyz - prop.worldposition.xyz;
            fDist = length(lightRay);
            float occlusion = 1;
            if (fDist < p.radius*EXTEND_RADIUS)
            {
                vec3 normal = prop.normal;
                // normal = vec3(0,1,0);
                // Diffuse
                float lightIntensity = p.intensity;
                float intensityDiffuse = 1 * lightIntensity;
                float intensitySpecular = 1 * lightIntensity;
                vec3 colorLight = clamp(p.color.rgb, vec3(0), vec3(12));
                vec3 lightDir = normalize(lightRay);
                vec3 diffuse = intensityDiffuse * max(dot(normal, lightDir), 0.0) * colorLight;
                // Specular
                vec3 halfwayDir = normalize(lightDir + prop.viewVector);  
                float spec = max(pow(max(dot(normal, halfwayDir), 0.0), 1.4), 0.0);
                vec3 specular = intensitySpecular * spec * colorLight;
                // Attenuation
                // float attenuation = 1.0 / (p.constant + p.linear * fDist + p.exponent * fDist * fDist);
                float at=max(1, (-0.05f+p.quadratic * fDist * fDist));
                float attenuation = 1.0 / at;
                attenuation = (attenuation - LIGHT_CUTOFF) / (1 - LIGHT_CUTOFF);
                attenuation = max(attenuation, 0);

                diffuse *= attenuation * occlusion;
                specular *= attenuation * occlusion;
                finalLight += diffuse;
                // finalLight += specular;
            }
        }
    }
    #ifdef DEBUG_LIGHT
#define LT_IDX gl_WorkGroupID.y*gl_NumWorkGroups.x+(gl_NumWorkGroups.x-1-gl_WorkGroupID.x)
    if (gl_LocalInvocationID.x==0&&gl_LocalInvocationID.y==0) {
        atomicExchange(debugBuf.tileLights[LT_IDX], int(pointLightCount));
    }
    #endif
    barrier();

    imageStore(finalImage, pixelPos, vec4(finalLight,1));
}

