#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "blockinfo.glsl"

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
const float _Iterations = 16;							// maximum ray iterations 
const float _PixelStride = 18;							// number of pixels per ray step close to camera
const float _PixelStrideZCuttoff = 152;					// ray origin Z at this distance will have a pixel stride of 1.0
const float _BinarySearchIterations = 2;				// maximum binary search refinement iterations
const float _PixelZSize = 32.0f;							// Z size in camera space of a pixel in the depth buffer
const float _MaxRayDistance = 41024.0f;						// maximum distance of a ray
const float _ScreenEdgeFadeStart = 0.85f;					// distance to screen edge that ray hits will start to fade (0.0 -> 1.0)

#endif
#ifdef SSR_2
const float _Iterations = 32;							// maximum ray iterations 
const float _PixelStride = 25;							// number of pixels per ray step close to camera
const float _PixelStrideZCuttoff = 1840;					// ray origin Z at this distance will have a pixel stride of 1.0
const float _BinarySearchIterations = 2;				// maximum binary search refinement iterations
const float _PixelZSize = 0.8f;							// Z size in camera space of a pixel in the depth buffer
const float _MaxRayDistance = 1222.0f;						// maximum distance of a ray
const float _ScreenEdgeFadeStart = 0.85f;					// distance to screen edge that ray hits will start to fade (0.0 -> 1.0)

#endif


// aka "CANT PLAY"
#ifdef SSR_3 
const float _Iterations = 512;							// maximum ray iterations 
const float _PixelStride = 4;							// number of pixels per ray step close to camera
const float _PixelStrideZCuttoff = 1;					// ray origin Z at this distance will have a pixel stride of 1.0
const float _BinarySearchIterations = 4;				// maximum binary search refinement iterations
const float _PixelZSize = 32.f;							// Z size in camera space of a pixel in the depth buffer
const float _MaxRayDistance = 41024.0f;						// maximum distance of a ray
const float _ScreenEdgeFadeStart = 0.85f;					// distance to screen edge that ray hits will start to fade (0.0 -> 1.0)

#endif

//don't touch these lines if you don't know what you do!
//default
// const int maxf = 4;				//number of refinements
// const float stp = 1.5;			//size of one step for raytracing algorithm
// const float ref = 0.7;			//refinement multiplier
// const float inc = 1.58;			//increasement factor at each step
// default
// const int maxf = 4;				//number of refinements
// const float stp = 1.5;			//size of one step for raytracing algorithm
// const float ref = 0.7;			//refinement multiplier
// const float inc = 1.58;			//increasement factor at each step


// const int maxf = 6;				//number of refinements
// const float stp = 0.25;			//size of one step for raytracing algorithm
// const float ref = 2.3;			//refinement multiplier
// const float inc = 1.16;			//increasement factor at each step
const int maxf = 4;				//number of refinements
const float stp = 1.2;			//size of one step for raytracing algorithm
const float ref = 0.1;			//refinement multiplier
const float inc = 2.2;			//increasement factor at each step

vec3 nvec3(vec4 pos) {
    return pos.xyz/pos.w;
}

vec4 nvec4(vec3 pos) {
    return vec4(pos.xyz, 1.0);
}

float cdist(vec2 coord) {
	return max(abs(coord.s-0.5),abs(coord.t-0.5))*2.0;
}

vec4 ssr(vec3 fragpos, vec3 normal,vec3 sky) {
    vec4 color = vec4(sky, 0);
    float alphaVal = 0.0;
    vec3 start = fragpos;
    float depthAt = fragpos.z;
    vec3 rvector = normalize(reflect(normalize(fragpos), normalize(normal)));
    vec3 vector = stp * rvector;
    vec3 oldpos = fragpos;
    fragpos += vector;
	vec3 tvector = vector;
    int sr = 0;
    int i=0;
    for(;i<40;i++){
        vec3 pos = nvec3(in_matrix_3D.p * nvec4(fragpos)) * 0.5 + 0.5;
        if(pos.x < 0 || pos.x > 1 || pos.y < 0 || pos.y > 1 || pos.z < 0 || pos.z > 1.0) {
			color.rgb = sky;
			color.a=1;
            break;
    	}
    	float de = texture(texDepthPreWater, pos.st).r;
        vec3 spos = vec3(pos.st, de);
        spos = nvec3(in_matrix_3D.proj_inv * nvec4(spos * 2.0 - 1.0));
        // if (spos.z > depthAt) {
        //     color.rgb = sky;
        //     color.a=1;
        //     break;
        // }
        float err = abs(length(fragpos.xyz-spos.xyz));
		if(err < pow(length(vector)*1.85,1.15)&&spos.z < depthAt*1.01){

                sr++;
                if(sr >= maxf){
                    float border = clamp(pow(cdist(pos.st), 10.0), 0.0, 1.0);
				    uvec4 blockinfo = texture(texMaterial, pos.st, 0);
					uint blockidPixel = BLOCK_ID(blockinfo);
					float isWater = IS_WATER(blockidPixel);
					float isSky = IS_SKY(blockidPixel);
                    color.a = 1.0;
                    color.rgb=mix(texture(texColor, pos.st).rgb, sky, max(0, min(1, isWater+isSky+border)));
                    break;
                }
				tvector -= vector;
                vector *= ref;
		}
        vector *= inc;
        oldpos = fragpos;
        tvector += vector;
		fragpos = start + tvector;
    }
    // color.a = alphaVal;
    return color;
    // if (i==0)
    // 	return vec4(0,0,1,1);
    // return vec4(0,1,0,1);
}


void main(void) {
	vec4 albedo = texture(texColor, pass_texcoord);
	vec3 normal = texture(texNormals, pass_texcoord).rgb * 2.0f - 1.0f;
    uvec4 blockinfo = texture(texMaterial, pass_texcoord, 0);
    vec4 rayDirVS = in_matrix_3D.proj_inv * vec4(pass_texcoord.s * 2.0f - 1.0f, pass_texcoord.t * 2.0f - 1.0f, 1, 1.0f);
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

		vec3 fragpos = vec3(pass_texcoord.st, texture(texDepth, pass_texcoord.st).r);
		fragpos = nvec3(in_matrix_3D.proj_inv * nvec4(fragpos * 2.0 - 1.0));
		float normalDotEye = dot(normalVS, normalize(fragpos));
		vec4 reflection = ssr(fragpos, normalVS, skyboxTex.rgb);

        // if (pass_texcoord.x>0.5)
        // out_Color=vec4(0,1,1,1);
        // else
		// out_Color = vec4(reflection.rgb, 1);

		reflection.a = min(reflection.a, 1.0);
		float angle = max ( min( pow(dot(nrayDirVS, normalVS.xyz) + 1.0, 2.4)*1.1, 1), 0 );
		reflection.rgb = mix(skyboxTex.rgb, reflection.rgb, reflection.a);
		

		out_Color = vec4(reflection.rgb*angle*max(reflection.a,0.75), angle*reflection.a);



		// if (texture(texDepth, pass_texcoord.st).r > 0) {
		// 	out_Color=vec4(1,0,1,1);
		// }
	} else {

		out_Color = vec4(0);
	}

}