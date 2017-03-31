#version 150 core

#pragma define "ALPHA_TEST"
#pragma define "SAMPLER_CONVERT_GAMMA"
#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"
#pragma include "unproject.glsl"

layout (set = 2, binding = 0) uniform sampler2D texShadow;

in vec4 pass_Color;
in vec2 pass_texcoord;
 
out vec4 out_Color;
 
float Linear01Depth(float depth) {
	float clipSpaceZ= (depth-Z_NEAR) / (10000-Z_NEAR);
	return min(1, clipSpaceZ);
}
void main(void) {
  // float z = texture(texShadow, pass_texcoord).r;
  // if (z == 0) {
  // 	out_Color = vec4(0);
  // } else {
  // 	out_Color = vec4(1);
  // }
  // out_Color = vec4(texture(texShadow, pass_texcoord).r);

    vec4 prevp = vec4(0, 480, 0, 320);
    if (gl_FragCoord.x > prevp.x && gl_FragCoord.x < prevp.y && gl_FragCoord.y > prevp.z && gl_FragCoord.y < prevp.w) {
        vec2 txc = (gl_FragCoord.xy-prevp.xz) / vec2(prevp.y-prevp.x, prevp.w-prevp.z);
        vec3 shadowmaps=vec3(texture(texShadow, txc).r);
        // shadowmaps.xy += txc.xy*0.3;
        vec2 v = step(txc, vec2(0.005))+step(vec2(1-0.005), txc);
        out_Color = vec4(shadowmaps+v.x+v.y, 1.0);
    } else discard;
  // float eyeZ = linearizeDepth(z);
  // float lind = Linear01Depth(eyeZ);
  // float d = pow(lind, 1.0f);
  // // float r = clamp(lind, 0, 1);
  // out_Color = vec4(1.0-pow(1.0-z, 600.0f));
  // out_Color = vec4(1.0-pow(lind, 1024));
}