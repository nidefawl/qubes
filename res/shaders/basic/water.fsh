#version 150 core

#define NOISE_TEX_SIZE 64

#pragma include "ubo_scene.glsl"

uniform sampler2DArray blockTextures;
uniform sampler2D waterNormals;


in vec4 color;
in vec3 normal;
in vec4 texcoord;
in vec2 light;

flat in vec4 faceLight;
flat in vec4 faceLightSky;

in vec2 texPos;
flat in uvec4 blockinfo;
in vec3 tangent;
in vec3 binormal;
in mat3 normalMat;
in vec4 vpos;
in vec4 vwpos;
in mat3 tbnMatrix;


out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;



float getBrightness(vec2 b) {
	return (1-pow(1-b.x, 2))*(1-pow(1-b.y, 2));
}


float lightAdj(float sky, float block) {
	// x = 1 - x;
	// return 1-x*x*x*(x*(x*6 - 15) + 10);
	const float minLevel = 0.1;
	return minLevel+clamp((sky+block)*(1-minLevel), 0, (1-minLevel));
}

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
	return texture2D_smooth(waterNormals, noisePos, noiseTexRes);
}
float waterDist(vec2 worldpos, vec2 offset) {
	float timeF = in_matrix.frameTime;
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
	float timeF = in_matrix.frameTime*0.01;
    vec2 uv0 = (uv/103.0)+vec2(timeF/17.0, timeF/29.0);
    vec2 uv1 = uv/107.0-vec2(timeF/-19.0, timeF/31.0)+vec2(0.23);
    vec2 uv2 = uv/vec2(897.0, 983.0)+vec2(timeF/101.0, timeF/97.0)+vec2(0.51);
    vec2 uv3 = uv/vec2(991.0, 877.0)-vec2(timeF/109.0, timeF/-113.0)+vec2(0.71);
    vec4 noise = (texture(waterNormals, uv0)) +
                 (texture(waterNormals, uv1)) +
                 (texture(waterNormals, uv2)) +
                 (texture(waterNormals, uv3));
    return noise*0.5-1.0;
}

/*

	vec2 texcoord_out = texcoord.st;
	vec2 texcoord_out2 = texcoord.st;
	const float fscale2=1.0f;
	float l = 1.0;//1/(0.3+clamp(length(vpos)/15, 0, 42));
	vec2 vw = normalize(tbnMatrix * vwpos.xyz).xy;
	vw = vec2(vwpos.x, vwpos.z);
	float sample0 = waterDist(vw, vec2(0,0));
	float sample1 = waterDist(vw, vec2(1,0));
	float sample2   = waterDist(vw, vec2(0,1));
	vec3 nd = vec3(sample0 - sample1, sample0 - sample2, 0);
	vw.x*=1.3;
	vw.y*=1.2;
	 sample0 = waterDist(vw, vec2(0,0));
	 sample1 = waterDist(vw, vec2(1,0));
	 sample2   = waterDist(vw, vec2(0,1));
	vec3 nd2 = vec3(sample0 - sample1, sample0 - sample2, 1);
	nd *= fscale2*l;
	nd2 *= fscale2*4;
	float f1= 1.0f - nd.r * nd.r - nd.g * nd.g;
	f1 = max(0.00001, f1);
	nd.b = sqrt(f1);
	nd = normalize(nd);
	nd2 = normalize(nd2);
	texcoord_out2.t+=nd2.s;
	texcoord_out2.s+=nd2.t;
	texcoord_out.s+=nd.s;
	texcoord_out.t+=nd.t;
	normal_out = tbnMatrix*nd;


*/
void main() {
	vec3 normal_out = normal;
	float dist = (14+length(vpos)/122);
	vec2 vw = vec2(vwpos.x, vwpos.z);
    vec4 noise = getNoise(vw*2.5);
    vec3 nd = normalize(noise.xzy*vec3(2.0, clamp(dist, 2.0, 100.0), 2.0));
	normal_out = nd;


	vec4 tex = texture(blockTextures, vec3(texcoord.st, float(blockinfo.x)));
	float xPos2 = texPos.x;
	float xPos = 1-texPos.x;
	float yPos2 = texPos.y;
	float yPos = 1-texPos.y;
	float lightSky =  0.0;
	lightSky += faceLight.x * xPos  * yPos;
	lightSky += faceLight.y * xPos2 * yPos;
	lightSky += faceLight.z * xPos2 * yPos2;
	lightSky += faceLight.w * xPos  * yPos2;
	float lightBlock =  0.0;
	lightBlock += faceLightSky.x * xPos  * yPos;
	lightBlock += faceLightSky.y * xPos2 * yPos;
	lightBlock += faceLightSky.z * xPos2 * yPos2;
	lightBlock += faceLightSky.w * xPos  * yPos2;
	vec3 color_adj = tex.rgb;
	color_adj *= color.rgb;
	// color_adj *= lightAdj(lightSky, lightBlock);
    out_Color = vec4(color_adj, tex.a*color.a);
    out_Normal = vec4((normal_out) * 0.5f + 0.5f, 1);
    out_Material = blockinfo;
    out_Light = vec4(lightSky, lightBlock, 1, 0);
    // gl_FragData[0] = vec4(0,1,1,1);
}