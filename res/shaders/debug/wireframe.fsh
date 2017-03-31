#version 150 core


#pragma define "RENDER_WIREFRAME"


#ifdef VULKAN_GLSL
layout(push_constant) uniform PushConstantsWireFrame2 {
    layout(offset = 84) float thickness;
    float maxDistance;
} pushCWireFrame2;
#define LINE_THICKNESS pushCWireFrame2.thickness
#define WIREFRAME_RENDER_DISTANCE pushCWireFrame2.maxDistance
#else
uniform float thickness;
uniform float maxDistance;
#define LINE_THICKNESS thickness
#define WIREFRAME_RENDER_DISTANCE maxDistance
#endif


in vec4 color;
noperspective in vec3 vposition;
noperspective in vec3 normal;
in vec3 triangle;

out vec4 out_Color;

float edgeFactor(){
    vec3 d = fwidth(triangle);
    vec3 a3 = smoothstep(vec3(0.0), d*1.5, triangle);
    return min(min(a3.x, a3.y), a3.z);
}

void main() {
    float dist = length(vposition);
    float fdistscale = 1.0f-clamp((dist - WIREFRAME_RENDER_DISTANCE) / 15.0f, 0.0f, 1.0f);
    vec3 d = fwidth(triangle)*fdistscale;
    vec3 tdist = smoothstep(vec3(0.0), d*2.0f, triangle);
    float mixF = min(min(tdist.x, tdist.y), tdist.z);
    if (mixF > LINE_THICKNESS)
        discard;
    out_Color = vec4(color.rgb, color.a*fdistscale);
}
