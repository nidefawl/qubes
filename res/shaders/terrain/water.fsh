#version 150 core

#define NOISE_TEX_SIZE 64

#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"
#pragma include "ubo_constants.glsl"

uniform sampler2DArray blockTextures;
uniform sampler2D waterNoiseTexture;


in vec4 color;
in vec4 texcoord;
in vec3 normal;
flat in uvec4 blockinfo;

flat in uint faceDir; // duplicate data, see comment int terrain.fsh
flat in vec4 faceLight;
flat in vec4 faceLightSky;
in vec2 texPos;
in vec4 vpos;
in vec4 vwpos;
in float isWater;



out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;


vec4 texture2D_smooth(sampler2D tex, vec2 uv, vec2 res)
{
	uv = uv*res + 0.5;
	vec2 iuv = floor( uv );
	vec2 fuv = fract( uv );
	uv = iuv + fuv*fuv*(3.0-2.0*fuv); // fuv*fuv*fuv*(fuv*(fuv*6.0-15.0)+10.0);;
	uv = (uv - 0.5)/res;
	return texture( tex, uv );
}
vec4 lookupNoiseTex(in vec2 noisePos)
{
	const vec2 noiseTexRes = vec2(NOISE_TEX_SIZE);
	return texture2D_smooth(waterNoiseTexture, noisePos, noiseTexRes);
}
float waterDist(vec2 worldpos, vec2 offset) {
	float timeF = FRAME_TIME;
	float waterScale;
	float waterSpeed;
	float wave = 0;
	float w1 = 0;
	float w = 1;
	const float fs=0.05;
	{
		waterSpeed = 0.0002f;
		waterScale = 1/8.0f;
		vec2 waterPos = (worldpos)+offset*fs;
		waterPos.x+=timeF*waterSpeed;
		waterPos.y+=timeF*waterSpeed;
		wave += lookupNoiseTex(waterPos*0.1).x; 
		// w+=2;
	}

	// {
	// 	waterSpeed = 0.0003f;
	// 	waterScale = 1/4.0f;
	// 	vec2 waterPos = (worldpos+offset*fs/2)*waterScale;
	// 	waterPos.x-=timeF*waterSpeed;
	// 	waterPos.y-=timeF*waterSpeed;
	// 	wave += lookupNoiseTex(waterPos).x; 
	// 	// w+=2;
	// }

	// {
	// 	waterSpeed = 0.00002f;
	// 	waterScale = 1/11.0f;
	// 	vec2 waterPos = (worldpos+offset*fs/3)*waterScale;
	// 	waterPos.x+=timeF*waterSpeed;
	// 	waterPos.y-=timeF*waterSpeed;
	// 	wave += lookupNoiseTex(waterPos).x; 
	// 	// w+=2;
	// }
	return wave/w;
}


vec4 getNoise(vec2 uv){
	float timeF = FRAME_TIME*0.006;
    vec2 uv0 = (uv/103.0)+vec2(timeF/17.0, timeF/29.0);
    vec2 uv1 = uv/107.0-vec2(timeF/-19.0, timeF/31.0)+vec2(0.23);
    vec2 uv2 = uv/vec2(897.0, 983.0)+vec2(timeF/101.0, timeF/97.0)+vec2(0.51);
    vec2 uv3 = uv/vec2(991.0, 877.0)-vec2(timeF/109.0, timeF/-113.0)+vec2(0.71);
    vec4 noise = (texture(waterNoiseTexture, uv0)) +
                 (texture(waterNoiseTexture, uv1)) +
                 (texture(waterNoiseTexture, uv2)) +
                 (texture(waterNoiseTexture, uv3));
    return noise*0.5-1.0;
}

void main() {
	vec3 normal_out = normal;
	vec2 texCoord2 = texcoord.st;
    if (isWater > 0) {
		mat3 tbnMat = mat3(matrix_tbn.mat[faceDir]);
		float dist = (14+length(vpos)/122);
		vec2 vw = vec2(vwpos.x, vwpos.z);
	    vec4 noise = getNoise(vw*2.5);
	    vec3 nd = normalize(noise.xzy*vec3(2.0, clamp(dist, 2.0, 100.0), 2.0));
		normal_out = normalize(tbnMat*nd);
		texCoord2.st+=nd.xz*2.2;
    }
	vec4 tex = texture(blockTextures, vec3(texCoord2, float(blockinfo.x)));
    uint blockid = (blockinfo.y&0xFFFu);
    float isWater = float(blockid==4u);
    // tex = mix(tex, vec4(0.2, 0.32, 0.43, 1)*1.1, isWater);
    tex.a=1;
	// tex.a=1;
	float xPos2 = texPos.x;
	float xPos = 1-texPos.x;
	float yPos2 = texPos.y;
	float yPos = 1-texPos.y;
	float lightLevelBlock =  0.0;
	lightLevelBlock += faceLight.x * xPos  * yPos;
	lightLevelBlock += faceLight.y * xPos2 * yPos;
	lightLevelBlock += faceLight.z * xPos2 * yPos2;
	lightLevelBlock += faceLight.w * xPos  * yPos2;
	float lightLevelSky =  0.0;
	lightLevelSky += faceLightSky.x * xPos  * yPos;
	lightLevelSky += faceLightSky.y * xPos2 * yPos;
	lightLevelSky += faceLightSky.z * xPos2 * yPos2;
	lightLevelSky += faceLightSky.w * xPos  * yPos2;
	vec3 color_adj = tex.rgb * color.rgb;
	srgbToLin(color_adj.rgb);
    out_Color = vec4(color_adj.rgb, color.a*tex.a);
    out_Normal = vec4((normal_out) * 0.5f + 0.5f, 1);
    out_Material = blockinfo;
    out_Light = vec4(lightLevelSky*0.5, lightLevelBlock, 1, 1);
    // gl_FragData[0] = vec4(0,1,1,1);
}
