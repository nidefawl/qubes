#version 150 core

#pragma include "ubo_scene.glsl"

#pragma include "blockinfo.glsl"
#pragma include "unproject.glsl"

uniform samplerCube tex0;

in vec2 pass_texcoord;

out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;


void main() { 
	vec4 pos = unprojectPos(pass_texcoord.st, DEPTH_FAR);
	vec3 rayDir=-normalize(pos.xyz);

    uint renderData = 0u;
    
    vec4 sampledColor = texture(tex0, rayDir*vec3(1,1,-1));
    out_Color = vec4(sampledColor.rgb*0.02, 1.0);
    out_Normal = vec4(0.5);
    renderData = ENCODE_RENDERPASS(8);
    out_Material = uvec4(0u,0u+renderData,0u,0u);
    out_Light = vec4(0);
}
