#version 150 core

#pragma include "SMAA_common.glsl"
#define SMAA_INCLUDE_VS 0
#define SMAA_INCLUDE_PS 1
#pragma include "SMAA.hlsl"

layout (set = 0, binding = 0) uniform sampler2D texColorCurrent;
layout (set = 1, binding = 0) uniform sampler2D texColorPrev;


#if SMAA_REPROJECTION
layout (set = 2, binding = 0) uniform sampler2D velocityTex;
#endif

in vec2 pass_texcoord;
 
out vec4 out_Color;


void main(void) {
#if SMAA_REPROJECTION
    out_Color = SMAAResolvePS(pass_texcoord, texColorCurrent, texColorPrev, velocityTex);
#else
    out_Color = SMAAResolvePS(pass_texcoord, texColorCurrent, texColorPrev);
#endif
}