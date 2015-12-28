#version 430

#pragma include "ubo_scene.glsl"


#define WORK_GROUP_SIZE 32
#define MAX_LIGHTS_PER_TILE 1024 
#define MAX_LIGHTS 1024
#define EXTEND_RADIUS 1.05f
#define LIGHT_CUTOFF 0.01f


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

struct PointLight
{
    vec4 position;
    vec4 color;
    float intensity;
    float radius;
    float constant;
    float linear;
    float exponent;
    float padding1;
};
layout (std430) buffer DebugOutputBuffer
{
    float debugVals[16];
    int tileLights[];
} debugBuf;

layout (std140) buffer PointLightStorageBuffer
{
    PointLight pointLights[];
};
layout (local_size_x = WORK_GROUP_SIZE, local_size_y = WORK_GROUP_SIZE, local_size_z = 1) in;


layout (binding = 0, rgba16f) readonly uniform highp image2D geometryNormal;
layout (binding = 1) uniform sampler2D depthBuffer;
layout (binding = 5, rgba16f) writeonly uniform highp image2D finalImage;

uniform int numActiveLights;

shared uint minDepth = 0xfF7FFFFF;
shared uint maxDepth = 0x7f7fffff;
shared uint pointLightCount = 0;
shared uint pointLightIndex[MAX_LIGHTS];
shared uint maxLightIndex = 0;

float expToLinearDepth(in float depth)
{
    return 2.0f * in_scene.viewport.z * in_scene.viewport.w / (in_scene.viewport.w + in_scene.viewport.z - (2.0f * depth - 1.0f) * (in_scene.viewport.w - in_scene.viewport.z));
}

vec4 unprojectPos(vec2 coord, in float depth) { 
    vec4 fragposition = in_matrix_3D.proj_inv * vec4(coord.s * 2.0f - 1.0f, coord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
    fragposition /= fragposition.w;
    return fragposition;
}

//----------------------------------------------------------------------------
vec3 ReconstructViewPosition(float zBuffer, uvec2 fragCoord)
{
    vec2 clipPos = (vec2(fragCoord) + 0.5) * (1.0/in_scene.viewport.xy); // InvViewDim
    clipPos = clipPos * 2.0 - 1.0;
    
    vec4 viewPositionH = in_matrix_3D.proj_inv * vec4(clipPos, zBuffer, 1.0);
    return viewPositionH.xyz / viewPositionH.w; 
}

// p1 is always camera origin in view space, float3(0, 0, 0)
vec4 CreatePlaneEquation(/*float3 p1,*/ vec3 p2, vec3 p3)
{
    vec4 plane;

    plane.xyz = normalize(cross(p2, p3));
    plane.w = 0;

    return plane;
}
void buildFrustum(inout vec4 frustumPlanes[6], in vec2 wrkGrp, in float minZ, in float maxZ) {
    vec2 resolution = in_scene.viewport.xy;
    double tileScaleX = 64.0/double(resolution.x);
    double tileScaleY = 64.0/double(resolution.y);
    double extendX = tileScaleX*0.01;
    double extendY = tileScaleY*0.01;
    // Top/Bottom
    frustumPlanes[0] = vec4( 0,  1, 0, -1+tileScaleY*wrkGrp.y+tileScaleY+extendY);
    frustumPlanes[1] = vec4( 0, -1, 0, 1-tileScaleY*wrkGrp.y+extendY);
    // Left/Right
    frustumPlanes[2] = vec4( 1,  0, 0, -1+tileScaleX*wrkGrp.x+tileScaleX+extendX);
    frustumPlanes[3] = vec4(-1,  0, 0, 1-tileScaleX*wrkGrp.x+extendX);
    // Near/Far
    frustumPlanes[4] = vec4(0, 0, 1, -minZ);
    frustumPlanes[5] = vec4(0, 0, -1, maxZ);
    for (int i = 0; i < 4; ++i) {
        frustumPlanes[i] /= length(frustumPlanes[i].xyz);
    }
}
void buildFrustum2(inout vec4 frustumPlanes[6], in vec2 wrkGrp, in float minZ, in float maxZ) {
    vec2 resolution = in_scene.viewport.xy;
    mat4 Projection = in_matrix_3D.p;
    vec2 tileScale = vec2(resolution.xy) / (2.0f * vec2(WORK_GROUP_SIZE, WORK_GROUP_SIZE));
    vec2 tileBias = tileScale - vec2(gl_WorkGroupID.xy);

    // Left/Right/Bottom/Top
    frustumPlanes[0] = vec4(Projection[0][0] * tileScale.x, 0, tileBias.x, 0);
    frustumPlanes[1] = vec4(-Projection[0][0] * tileScale.x, 0, 1 - tileBias.x, 0);
    frustumPlanes[2] = vec4(0, Projection[1][1] * tileScale.y, tileBias.y, 0);
    frustumPlanes[3] = vec4(0, -Projection[1][1] * tileScale.y, 1 - tileBias.y, 0);
    // Near/Far
    frustumPlanes[4] = vec4(0, 0, -1, minZ);
    frustumPlanes[5] = vec4(0, 0, 1, -maxZ);

    for (uint i = 0; i < 4; ++i)
        frustumPlanes[i] /= length(frustumPlanes[i].xyz);

}
bool inFrustumDbg(in vec4 pos, vec2 wrkGrp) {
    vec4 frustumPlanes[6];
    buildFrustum(frustumPlanes, wrkGrp, -1, 1);
    bool inFrustum = true;
    for (int i = 0; inFrustum && i < 6; i++) {
        float d = dot(frustumPlanes[i], vec4(pos.xyz, 1.0));
        inFrustum = (d >= 0);
    }
    return inFrustum;
}
void main()
{
    vec3 camPos = vec3(0, 0, 0);
    vec2 resolution = in_scene.viewport.xy;
    ivec2 iResolution = ivec2(resolution) - ivec2(1);
    uvec2 workGroupPixelOffset = gl_WorkGroupID.xy * gl_WorkGroupSize.xy;

    ivec2 pixelPos = ivec2(resolution.x-1-gl_GlobalInvocationID.x, resolution.y-1-gl_GlobalInvocationID.y);
    vec2 texCoord = pixelPos / resolution;
    vec4 nl = imageLoad(geometryNormal, pixelPos);
    prop.normal = nl.rgb * 2.0f - 1.0f;
    prop.depth = texelFetch(depthBuffer, pixelPos, 0).r;
    prop.linearDepth = expToLinearDepth(prop.depth);
    prop.position = unprojectPos(texCoord, prop.depth);
    prop.worldposition = in_matrix_3D.mv_inv * prop.position;
    // prop.worldposition.xyz /= prop.worldposition.w;
    prop.viewVector = normalize(CAMERA_POS - prop.worldposition.xyz);

    float viewSpaceZ = in_matrix_3D.p[3][2] / (prop.depth - in_matrix_3D.p[2][2]);
    uint depth = floatBitsToUint(prop.position.z);

    if (prop.depth > 0.0) 
    {
        atomicMin(minDepth, depth);
        atomicMax(maxDepth, depth);
    }
    barrier();

    float minDepthZ = uintBitsToFloat(minDepth);
    float maxDepthZ = uintBitsToFloat(maxDepth);
    debugBuf.debugVals[0] = minDepthZ;
    debugBuf.debugVals[1] = maxDepthZ;
   
    vec4 frustumPlanes[6];
    buildFrustum2(frustumPlanes, vec2(gl_WorkGroupID.xy), minDepthZ, maxDepthZ);




    uint lightIndex = gl_LocalInvocationIndex;
    if (lightIndex < numActiveLights) {
        PointLight p = pointLights[lightIndex];
        vec4 pos = in_matrix_3D.mv * vec4(p.position.xyz, 1);
        pos /= pos.w;
        // pos.xyz/pos.w;
        // pos.w = 1;
        // pos = in_matrix_3D.p * pos;
        // pos.w = 1;
        float rad = p.radius*(EXTEND_RADIUS);

        // if (pointLightCount < MAX_LIGHTS_PER_TILE)
        {
            bool inFrustum = true;
            for (int i = 0; i < 2; ++i)
            {
                float d = dot(frustumPlanes[i], vec4(pos.xyz, 1.0));
                inFrustum = inFrustum && (d >= -rad);
            }
            for (int i = 2; i < 4; ++i)
            {
                float d = dot(frustumPlanes[i], vec4(pos.xyz, 1.0));
                inFrustum = inFrustum && (d >= -rad);
            }
            for (int i = 4; i < 6; ++i)
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
    debugBuf.tileLights[gl_WorkGroupID.y*gl_NumWorkGroups.x+(gl_NumWorkGroups.x-1-gl_WorkGroupID.x)] = int(pointLightCount);
    // debugBuf.tileLights[0] = 4;
    // debugBuf.tileLights[1] = 5;
    // debugBuf.tileLights[2] = 6;
    // debugBuf.tileLights[3] = 7;
    // barrier();
    // memoryBarrierShared();
    // groupMemoryBarrier();
    // memoryBarrier();
    vec3 finalLight = vec3(0);
    float fDist = 0;
    for(int i = 0; i < pointLightCount; ++i)
    {
        uint idx = pointLightIndex[i];
        if (idx >= numActiveLights) {
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
            float attenuation = 1.0 / (p.constant + p.linear * fDist + p.exponent * fDist * fDist);
            attenuation = (attenuation - LIGHT_CUTOFF) / (1 - LIGHT_CUTOFF);
            attenuation = max(attenuation, 0);

            diffuse *= attenuation * occlusion;
            specular *= attenuation * occlusion;
            finalLight += diffuse;
            finalLight += specular;
        }
    }
    barrier();
    // finalLight = vec3(1);
    vec4 pos = in_matrix_3D.mvp * vec4(prop.worldposition.xyz, 1);
    pos /= pos.w;
    // bool inFrustum = true;
    // for (int i = 0; i < 2; ++i)
    // {
    //     float d = dot(frustumPlanes[i], vec4(pos.xyz, 1.0));
    //     inFrustum = inFrustum && (d >= 0);
    // }
    // if (inFrustum) {
    //     finalLight = prop.normal.rgb;
    // }
    // for (int i = 2; i < 4; ++i)
    // {
    //     float d = dot(frustumPlanes[i], vec4(pos.xyz, 1.0));
    //     inFrustum = inFrustum && (d >= 0);
    // }


    // if (inFrustumDbg(pos, vec2(gl_WorkGroupID.xy))) {
    //     finalLight = prop.normal.rgb;
    // } else {

    //     finalLight = vec3(0);
    // }

    imageStore(finalImage, pixelPos, vec4(finalLight,1));
}

