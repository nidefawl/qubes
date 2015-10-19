#version 150 core


#pragma define "FAR_BLOCKFACE"
#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"
#pragma include "util.glsl"

uniform sampler2DArray blockTextures;
uniform sampler2D noisetex;


in vec4 color;
in vec3 normal;
in vec4 texcoord;
in vec4 position;
in vec2 light;
flat in vec4 faceAO;
flat in vec4 faceLight;
flat in vec4 faceLightSky;
in float blockside;

in vec2 texPos;
flat in uvec4 blockinfo;


out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;

float lightAdj(float sky, float block) {
	// x = 1 - x;
	// return 1-x*x*x*(x*(x*6 - 15) + 10);
	const float minLevel = 0.1;
	return minLevel+clamp((sky+block)*(1-minLevel), 0, (1-minLevel));
}

void main2(void) {

	// vec4 tex=textureLod(blockTextures, vec3(texcoord.st, float(blockinfo.x)), 0 );
	// vec4 tex=texture(blockTextures, vec3(texcoord.st, float(blockinfo.x)));
	// tex = vec4(vec3(1),1);
	// if (tex.a<1)
	// 	discard;
	float xPos2 = texPos.x;
	float xPos = 1-texPos.x;
	float yPos2 = texPos.y;
	float yPos = 1-texPos.y;

	float ao =  0.0;
	ao += faceAO.x * xPos  * yPos;
	ao += faceAO.y * xPos2 * yPos;
	ao += faceAO.z * xPos2 * yPos2;
	ao += faceAO.w * xPos  * yPos2;

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

	// int timeW = mod(floor(in_scene.frameTime.x), 20) > 10 ? 1 : 0;
	float ambientOccl = 1 - clamp(ao, 0,1);
	vec3 color_adj = vec3(0.8);
	color_adj *= color.rgb;

	//MINECRAFTISH BLOCK LIGHTING
	// color_adj *= 0.5+abs(dir.z)*0.3+(dir.y)*0.2;
	color_adj *= blockside;



	// float lightLvl = lightAdj(lightSky, lightBlock);
	// color_adj *= ambientOccl;
	// color_adj *= lightLvl;
	float alpha = 1*1;
	// if (alpha<1)
	// 	discard;
    out_Color = vec4(color_adj, alpha);
    out_Normal = vec4((normal) * 0.5f + 0.5f, 1);
    out_Material = blockinfo;
    out_Light = vec4(lightLevelSky, lightLevelBlock, ambientOccl, 1);
    // gl_FragData[0] = vec4(0,1,1,1);
}
void main(void) {

	// vec4 tex=textureLod(blockTextures, vec3(texcoord.st, float(blockinfo.x)), 0 );
	vec4 tex=texture(blockTextures, vec3(texcoord.st, float(blockinfo.x)));
	// tex = vec4(vec3(1),1);
#ifndef FAR_BLOCKFACE
#endif
	if (tex.a<1)
		discard;
	 // tex = vec4(vec3(1),1);
	vec3 color_adj = tex.rgb;
	color_adj *= color.rgb;
	srgbToLin(color_adj.rgb);
	// float lum = dot(color_adj, vec3(0.3));

	//MINECRAFTISH BLOCK LIGHTING
	color_adj *= blockside;
	// color_adj *= 0.5+abs(dir.z)*0.3+(dir.y)*0.2;


	float xPos2 = texPos.x;
	float xPos = 1-texPos.x;
	float yPos2 = texPos.y;
	float yPos = 1-texPos.y;

	float ao =  0.0;
	ao += faceAO.x * xPos  * yPos;
	ao += faceAO.y * xPos2 * yPos;
	ao += faceAO.z * xPos2 * yPos2;
	ao += faceAO.w * xPos  * yPos2;
	// ao*=1;
	float ambientOccl = 1 - clamp(ao, 0,1);
	ambientOccl*=1;
	// ambientOccl += 0.3*lum*4;
#ifndef FAR_BLOCKFACE
#else //FAR_BLOCKFACE
	// color_adj=mix(color_adj, vec3(1, 0, 0), 0.4);
#endif


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

	// int timeW = mod(floor(in_scene.frameTime.x), 20) > 10 ? 1 : 0;


		float f = float(blockinfo.y&0xFFFu);

		//TODO: figure out something better 
	if (f >= 12&&f <=16) { //EXPENSIVE LEAVE

		f-=12;

	    float sampleDist = 4.4;
	    vec2 p0 = position.xz *0.02;
	    // float fSin = sin(in_scene.frameTime.x*0.0003)*0.5+0.5;
	    // p0 += vec2(fSin*110.3);
	    vec2 p1 = p0 + vec2(1, 0)*sampleDist;
	    vec2 p2 = p0 + vec2(0, 1)*sampleDist;
	    float s0 = snoise(p0);
	    float s1 = snoise(p1);
	    float s2 = snoise(p2);
	    color_adj*=pal((s0+s1+s2)/3.0, 
              vec3(0.4+(f/5.0)*0.5,0.78,0.1)*(0.27+(f/10.0)),
              vec3(0.15-clamp(1-f/4.0,0,1)*0.07),
              vec3(0.15),
              vec3(0.15)  )*1.2;
	}


	// float lightLvl = lightAdj(lightSky, lightBlock);
	// color_adj *= ambientOccl;
	// color_adj *= lightLvl;
	float alpha = tex.a*1;
	// if (alpha<1)
	// 	discard;
    out_Color = vec4(color_adj, alpha);
    out_Normal = vec4((normal) * 0.5f + 0.5f, 1);
    out_Material = blockinfo;
    out_Light = vec4(lightLevelSky, lightLevelBlock, ambientOccl, 1);
    // gl_FragData[0] = vec4(0,1,1,1);
}
