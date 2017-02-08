#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "blockinfo.glsl"
#pragma include "unproject.glsl"

uniform sampler2D texColor;
uniform sampler2D texNormals;
uniform usampler2D texMaterial;
uniform sampler2D texDepth;
uniform samplerCube texSkybox;
uniform sampler2D texDepthPreWater;


in vec2 pass_texcoord;


out vec4 out_Color;

#pragma define "SSR"

#ifdef SSR_1
#define MAX_REFINEMENTS 4
#define STEP_MULT 1.4
#define REFINE_MULT 0.78
#define MAX_STEPS 15
#define STEP_SIZE 1.3
#endif

#ifdef SSR_2
#define MAX_REFINEMENTS 3
#define STEP_MULT 1.2
#define REFINE_MULT 0.96
#define MAX_STEPS 28
#define STEP_SIZE 1.05
#endif

#ifdef SSR_3 
#define MAX_REFINEMENTS 6
#define STEP_MULT 1.1
#define REFINE_MULT 0.98
#define MAX_STEPS 35
#define STEP_SIZE 0.9
#endif



float cdist(vec2 coord) {
	return max(abs(coord.s-0.5),abs(coord.t-0.5))*2.0;
}

vec3 toScreen(vec3 worldPos) {
    vec4 tmp1 = vec4(worldPos, 1.0);
    vec4 tmp2 = in_matrix_3D.p * tmp1;
    vec3 pos = tmp2.xyz/tmp2.w;
#if Z_INVERSE
    return vec3(pos.xy*0.5+0.5, pos.z);
#else
    return pos * 0.5 + 0.5;
#endif
}
vec3 toWorld(vec3 screenPos) {
#if Z_INVERSE
    vec4 tmp1 = vec4(screenPos.xy*2.0-1.0, screenPos.z, 1.0);
#else
    vec4 tmp1 = vec4(screenPos * 2.0 - 1.0, 1.0);
#endif
    vec4 tmp2 = in_matrix_3D.proj_inv * tmp1;
    vec3 pos = tmp2.xyz/tmp2.w;
    return pos;
}
vec4 ssr(vec3 worldPos, vec3 normal,vec3 sky) {
    int numRefinements = 0;
    vec3 reflectRay = normalize(reflect(normalize(worldPos), normalize(normal)));
    vec4 color = vec4(sky, 0);
    vec3 refineVector = STEP_SIZE * reflectRay;
	vec3 ray = refineVector;
    float depthStart = worldPos.z;
    for(int i=0; i < MAX_STEPS; i++) {
        vec3 rayPos = worldPos + ray;
        vec3 pos = toScreen(rayPos);
        if(pos.x < 0 || pos.x >= 1 || pos.y < 0 || pos.y >= 1/* || pos.z < -1.0*/ || pos.z > 1.0) {
			// color.rgb = sky;
			// color.a=1;
            break;
    	}
        vec3 worldPosUnderwater = toWorld(vec3(pos.st, texture(texDepthPreWater, pos.st).r));
        float err = abs(length(rayPos.xyz-worldPosUnderwater.xyz));
		if(err < pow(length(refineVector)*1.85,1.15) && worldPosUnderwater.z < depthStart*1.01) {
            if (numRefinements++ >= MAX_REFINEMENTS) {
                float border = clamp(pow(cdist(pos.st), 10.0), 0.0, 1.0);
			    uvec4 blockinfo = texture(texMaterial, pos.st, 0);
				uint blockidPixel = BLOCK_ID(blockinfo);
				float isWater = IS_WATER(blockidPixel);
				float isSky = IS_SKY(blockidPixel);
                color.a = max(0, min(1, isWater+isSky+border));
                color.rgb=mix(texture(texColor, pos.st).rgb, sky, color.a);
                color.a = 1.0-color.a;
                if (isSky > 0) {
                    color.r = 1;
                    color.a = 1;
                }

                // color.a = 0;
                break;
            }
			ray -= refineVector;
            refineVector *= REFINE_MULT;
		}
        refineVector *= STEP_MULT;
        ray += refineVector;
    }
    // return vec4(0, 1, 0, color.a);
    return color;
}


void main(void) {
	vec4 albedo = texture(texColor, pass_texcoord);
	vec3 normal = texture(texNormals, pass_texcoord).rgb * 2.0f - 1.0f;
    uvec4 blockinfo = texture(texMaterial, pass_texcoord, 0);
    vec4 rayDirVS = in_matrix_3D.proj_inv * vec4(pass_texcoord.s * 2.0f - 1.0f, pass_texcoord.t * 2.0f - 1.0f, DEPTH_FAR, 1.0f);
	rayDirVS /= rayDirVS.w;
	vec4 normalVS4 = transpose(inverse(in_matrix_3D.mv)) * vec4(normal, 1);
    vec3 normalVS = normalize( normalVS4.xyz);
	vec3 nrayDirVS = normalize( rayDirVS.xyz);

	vec3 vsRayWorld = normalize( reflect( rayDirVS.xyz, normalVS));
    vsRayWorld = mat3(in_matrix_3D.mv_inv) * vsRayWorld;
	vec4 skyboxTex = texture(texSkybox, vsRayWorld * vec3(-1, -1, 1))*0.05;

	uint blockidPixel = BLOCK_ID(blockinfo);
	float isWater = IS_WATER(blockidPixel);
	if (isWater>0) {
		vec4 cAlbedo = vec4(0.0);

		vec3 fragpos = toWorld(vec3(pass_texcoord.st, texture(texDepth, pass_texcoord.st).r));
		float normalDotEye = dot(normalVS, normalize(fragpos));
		vec4 reflection = ssr(fragpos, normalVS, skyboxTex.rgb);

        // if (pass_texcoord.x>0.5)
        // out_Color=vec4(0,1,1,1);
        // else
		// out_Color = vec4(reflection.rgb, 1);

        // reflection.a=1;
		reflection.a = min(reflection.a, 1.0);
		float angle = max ( min( pow(dot(nrayDirVS, normalVS.xyz) + 1.0, 2.4)*1.1, 1), 0 );
		reflection.rgb = mix(skyboxTex.rgb, reflection.rgb, reflection.a);

		out_Color = vec4(reflection.rgb*angle*max(reflection.a,0.75), angle);



		// if (texture(texDepth, pass_texcoord.st).r > 0) {
		// 	out_Color=vec4(1,0,1,1);
		// }
	} else {

		out_Color = vec4(0);
	}

}