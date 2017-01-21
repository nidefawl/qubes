#version 150 core

#pragma include "ubo_scene.glsl"

#pragma include "blockinfo.glsl"

uniform samplerCube tex0;

in vec4 pass_texcoord;

out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;

vec4 unprojectPos(in vec2 coord, in float depth) { 
    // vec4 fragposition = in_matrix_3D.proj_inv * vec4(coord.s * 2.0f - 1.0f, coord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
    vec4 fragposition = inverse(in_matrix_3D.vp) * vec4(coord.s * 2.0f - 1.0f, coord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
    fragposition /= fragposition.w;
    return fragposition;
}
vec4 unprojectPosWS(in vec2 coord, in float depth) { 
    // vec4 fragposition = in_matrix_3D.proj_inv * vec4(coord.s * 2.0f - 1.0f, coord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
    vec4 fragposition = inverse(in_matrix_3D.mvp) * vec4(coord.s * 2.0f - 1.0f, coord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
    fragposition /= fragposition.w;
    return fragposition;
}

void main() { 
	vec4 pos = unprojectPos(vec2(pass_texcoord.s, pass_texcoord.t), 1.0);
	vec3 rayDir=-normalize(pos.xyz);

    uint renderData = 0u;


    // blendColor(cloudColor, fogColorLit, yt*0.9);

    // out_Color = cloudColor*0.4;
    // out_Color = vec4(1.0, 1.0, 0.0, 1.0);
    vec4 sampledColor = texture(tex0, rayDir*vec3(1,1,-1));
    out_Color = vec4(sampledColor.rgb*0.02, 1.0);
    out_Normal = vec4(0.5);
    renderData = ENCODE_RENDERPASS(8);
    out_Material = uvec4(0u,0u+renderData,0u,0u);
}
