#version 430

#define WORK_GROUP_SIZE 32
#define MAX_LIGHTS_PER_TILE 40 
#define MAX_LIGHTS 1024

struct Attenuation
{
    float constant;
    float linear;
    float exponent;
};

struct Light
{
    vec3 color;
    float intensity;
};

struct DirectionalLight
{
    Light light;
    vec3 direction;
};

struct PointLight
{
    Light light;
    Attenuation atten;
    vec3 position;
    float radius;
};

struct SpotLight
{
    Light light;
    Attenuation atten;
    vec3 position;
    float range;
    vec3 direction;
    float cutoff;
};

layout (std140, binding = 0) buffer point
{
    PointLight pointLights[];
};

layout (std140, binding = 1) buffer spot
{
    SpotLight spotLights[];
};

//uniform DirectionalLight directionalLight;
//uniform Light ambientLight;

//uniform vec2 resolution;
//uniform vec3 camPos;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;
uniform int numActiveLights;

layout (binding = 6, rgba32f) uniform writeonly image2D finalImage;
layout (binding = 2, rgba32f) uniform readonly image2D geometryPosition;
layout (binding = 3, rgba32f) uniform readonly image2D geometryDiffuse;
layout (binding = 4, rgba32f) uniform readonly image2D geometryNormal;
layout (binding = 5, rgba32f) uniform readonly image2D geometrySpecular;

layout (local_size_x = WORK_GROUP_SIZE, local_size_y = WORK_GROUP_SIZE, local_size_z = 1) in;

shared uint minDepth;
shared uint maxDepth;
shared uint pointLightCount;
shared uint pointLightIndex[MAX_LIGHTS];

vec4 calcLight(Light light, vec3 direction, vec3 normal, vec3 fragPos, vec3 specular, vec3 camPos)
{
    float diffuseFactor = dot(normal, -direction);

    vec4 diffuseColor = vec4(0, 0, 0, 0);
    vec4 specularColor = vec4(0, 0, 0, 0);

    if (diffuseFactor > 0)
    {
        // might need to be 0.0----------| |
        diffuseColor = vec4(light.color, 1.0) * light.intensity * diffuseFactor;

        vec3 directionToEye = normalize(camPos - fragPos);
        vec3 halfDirection = normalize(directionToEye - direction);

        float specularFactor = dot(halfDirection, normal);
        // maybe not 32?
        specularFactor = pow(specularFactor, 32);

        if (specularFactor > 0)
        {
            // maybe 0.0 again
            specularColor = vec4(light.color, 1.0) * vec4(specular, 1.0) * specularFactor;
        }
    }

    return diffuseColor + specularColor;
}

vec4 calcDirectionalLight(DirectionalLight dirLight, vec3 pos, vec3 normal, vec3 specular, vec3 camPos)
{
    return calcLight(dirLight.light, dirLight.direction, normal, pos, specular, camPos);
}

vec4 calcPointLight(PointLight pointLight, vec3 pos, vec3 normal, vec3 specular, vec3 camPos)
{
    vec3 lightDirection = pos - pointLight.position;
    float distanceToPoint = length(lightDirection);

    lightDirection = normalize(lightDirection);

    vec4 color = calcLight(pointLight.light, lightDirection, normal, pos, specular, camPos);

    float atten = pointLight.atten.constant + pointLight.atten.linear * distanceToPoint + pointLight.atten.exponent * distanceToPoint * distanceToPoint + 0.0001;

    return color / atten;
}

void main()
{
    vec3 camPos = vec3(0, 0, 0);
    vec2 resolution = vec2(600, 400);

    ivec2 pixelPos = ivec2(gl_GlobalInvocationID.xy);
    vec2 tilePos = vec2(gl_WorkGroupID.xy * gl_WorkGroupSize.xy) / resolution;

    vec4 normalColor = imageLoad(geometryNormal, pixelPos);

    float d = normalColor.w;
    uint depth = uint(d * 0xFFFFFFFF);

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

    vec3 position = imageLoad(geometryPosition, pixelPos).xyz;
    vec4 diffuse = vec4(imageLoad(geometryDiffuse, pixelPos).xyz, 1.0);
    vec3 normal = normalColor.xyz;
    vec3 specular = imageLoad(geometrySpecular, pixelPos).xyz;

    vec4 color = vec4(0.0, 0.0, 0.0, 1.0);

    for (int i = 0; i < pointLightCount; i++)
    {
        color += calcPointLight(pointLights[pointLightIndex[i]], position, normal, specular, camPos);
    }

    //color += calcDirectionalLight(directionalLight, position, normal, specular, camPos);
    //diffuse *= vec4((ambientLight.intensity * ambientLight.color).xyz, 1.0);
    color += diffuse;

    barrier();

    imageStore(finalImage, pixelPos, color);

}