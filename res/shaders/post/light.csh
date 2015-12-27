#version 430

#pragma include "ubo_scene.glsl"


#define WORK_GROUP_SIZE 32
#define MAX_LIGHTS_PER_TILE 40 
#define MAX_LIGHTS 1024

struct Attenuation
{
    float constant;
    float linear;
    float exponent;
}; 
// aten size = 3
struct Light
{
    vec3 color;
    float intensity;
};
//light size = 4
struct PointLight
{
    Light light;
    Attenuation atten;
    vec3 position;
    float radius;
};
//pnt light size = 3+4+4=11

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

layout(std140) uniform LightInfo {
  vec4 dayLightTime; 
  vec4 posSun; // Light position in world space
  vec4 lightDir; // Light dir in world space
  vec4 La; // Ambient light intensity
  vec4 Ld; // Diffuse light intensity
  vec4 Ls; // Specular light intensity
} SkyLight;


//uniform DirectionalLight directionalLight;
//uniform Light ambientLight;

//uniform vec2 resolution;
//uniform vec3 camPos;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;
uniform int numActiveLights;

layout (binding = 0, rgba16f) readonly uniform highp image2D geometryNormal;
layout (binding = 1) uniform sampler2D depthBuffer;

layout (binding = 5, rgba16f) writeonly uniform highp image2D finalImage;
layout (std430) buffer DebugOutputBuffer
{
    uint maxLights;
    uint maxLights2;
    uint maxLights3;
    uint maxLights4;
} debugBuf;

layout (std430) buffer PointLightStorageBuffer
{
    PointLight pointLights[];
};

layout (local_size_x = WORK_GROUP_SIZE, local_size_y = WORK_GROUP_SIZE, local_size_z = 1) in;

shared uint minDepth = 0;
shared uint maxDepth = 0;
shared uint pointLightCount = 0;
shared uint pointLightIndex[MAX_LIGHTS];
shared uint maxLightIndex = 0;

float expToLinearDepth(in float depth)
{
    return 2.0f * in_scene.viewport.z * in_scene.viewport.w / (in_scene.viewport.w + in_scene.viewport.z - (2.0f * depth - 1.0f) * (in_scene.viewport.w - in_scene.viewport.z));
}

vec4 unprojectPos(in vec2 coord, in float depth) { 
    vec4 fragposition = in_matrix_3D.proj_inv * vec4(coord.s * 2.0f - 1.0f, coord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
    fragposition /= fragposition.w;
    return fragposition;
}
void main()
{
    vec3 camPos = vec3(0, 0, 0);
    vec2 resolution = in_scene.viewport.xy;
    ivec2 pixelPos = ivec2(gl_GlobalInvocationID.xy);
    vec2 tilePos = vec2(gl_WorkGroupID.xy * gl_WorkGroupSize.xy) / resolution;

    vec4 nl = imageLoad(geometryNormal, pixelPos);
    prop.normal = nl.rgb * 2.0f - 1.0f;
    // prop.light = texture(texLight, pixelPos, 0);
    prop.depth = texture(depthBuffer, pixelPos).r;
    prop.linearDepth = expToLinearDepth(prop.depth);
    prop.position = unprojectPos(pixelPos, prop.depth);
    prop.worldposition = in_matrix_3D.mv_inv * prop.position;
    prop.viewVector = normalize(CAMERA_POS - prop.worldposition.xyz);
    prop.NdotL = dot( prop.normal, SkyLight.lightDir.xyz );
    // vec3 color = vec3(0);
    // if (prop.depth < 0) {
    //     color = vec3(1,0,0);
    // } else if (prop.depth > 1) {
    //     color = vec3(0,1,0);
    // }
    // color.r = WORK_GROUP_SIZE/40.0;

    // float d = normalColor.w;
    uint depth = uint(prop.depth * 0xFFFFFFFF);

    atomicMin(minDepth, depth);
    atomicMax(maxDepth, depth);

    barrier();

    float minDepthZ = float(minDepth / float(0xFFFFFFFF));
    float maxDepthZ = float(maxDepth / float(0xFFFFFFFF));

    vec2 tileScale = resolution * (1.0f / float( 2 * WORK_GROUP_SIZE));
    vec2 tileBias = tileScale - vec2(gl_WorkGroupID.xy);

    vec4 col1 = vec4(-projectionMatrix[0][0] * tileScale.x, projectionMatrix[0][1], tileBias.x, projectionMatrix[0][3]);
    vec4 col2 = vec4(projectionMatrix[1][0], -projectionMatrix[1][1] * tileScale.y, tileBias.y, projectionMatrix[1][3]);
    vec4 col4 = vec4(projectionMatrix[3][0], projectionMatrix[3][1], -1.0, projectionMatrix[3][3]);

    vec4 frustumPlanes[6];
    frustumPlanes[0] = col4 + col1;
    frustumPlanes[1] = col4 - col1;
    frustumPlanes[2] = col4 - col2;
    frustumPlanes[3] = col4 + col2;
    frustumPlanes[4] = vec4(0.0, 0.0, -1.0, -minDepthZ);
    frustumPlanes[5] = vec4(0.0, 0.0, -1.0, maxDepthZ);

    for (int i = 0; i < 4; i++)
    {
        frustumPlanes[i] *= 1.0 / length(frustumPlanes[i].xyz);
    }

    uint threadCount = WORK_GROUP_SIZE * WORK_GROUP_SIZE;
    uint passCount = (numActiveLights + threadCount - 1) / threadCount;

    for (uint passIt = 0; passIt < passCount; passIt++)
    {
        uint lightIndex = passIt * threadCount + gl_LocalInvocationIndex;
        lightIndex = min(lightIndex, numActiveLights);
        atomicMax(maxLightIndex, lightIndex);

        PointLight p = pointLights[lightIndex];
        vec4 pos = viewMatrix * vec4(p.position, 1.0);
        float rad = p.radius;

        if (pointLightCount < MAX_LIGHTS_PER_TILE)
        {
            bool inFrustum = true;
            for (uint i = 3; i >= 0 && inFrustum; i--)
            {
                float dist = dot(frustumPlanes[i], pos);
                inFrustum = (-rad <= dist);
            }

            if (inFrustum)
            {
                uint id = atomicAdd(pointLightCount, 1);
                pointLightIndex[id] = lightIndex;
            }
        }
    }

    barrier();

    // vec3 position = imageLoad(geometryPosition, pixelPos).xyz;
    // vec4 diffuse = vec4(imageLoad(geometryDiffuse, pixelPos).xyz, 1.0);
    // vec3 normal = normalColor.xyz;
    // vec3 specular = imageLoad(geometrySpecular, pixelPos).xyz;

    vec4 color = vec4(0.0, 0.0, 0.0, 1.0);

    for (int i = 0; i < pointLightCount; i++)
    {
        // color += calcPointLight(pointLights[pointLightIndex[i]], position, normal, specular, camPos);
        color.r += 0.01f;
    }
    //color += calcDirectionalLight(directionalLight, position, normal, specular, camPos);
    //diffuse *= vec4((ambientLight.intensity * ambientLight.color).xyz, 1.0);
    // color += diffuse;
    // color.r = pointLightCount/1000.0f;
    // debugBuf.maxLights = 1;
    debugBuf.maxLights = pointLightCount/4;
    debugBuf.maxLights2 = maxLightIndex;
    debugBuf.maxLights3 = numActiveLights;
    barrier();

    imageStore(finalImage, pixelPos, color);

}