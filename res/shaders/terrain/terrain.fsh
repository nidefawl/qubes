#version 150 core


#pragma define "FAR_BLOCKFACE"
#pragma define "MODEL_RENDER"
#pragma include "ubo_scene.glsl"
#pragma include "ubo_constants.glsl"
#pragma include "tonemap.glsl"
#pragma include "util.glsl"
#pragma include "blockinfo.glsl"

uniform sampler2DArray blockTextures;
uniform sampler2D noisetex;
uniform sampler2DArray normalTextures; // needs to be another array later one (I guess)


in vec4 color;
in vec3 normal;
in vec4 texcoord;
in vec4 position;
in vec2 light;
flat in vec4 faceAO;
flat in vec4 faceLight;
flat in vec4 faceLightSky;
flat in uvec4 blockinfo;
flat in float blockid;
flat in uint faceDir;
in float blockside;
in vec2 texPos;


out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;

void main(void) {
#ifdef MODEL_RENDER
	vec4 tex = vec4(vec3(1), 1);
	// const float texScale = 1.0/16.0;
	// vec2 xzPos = mod(position.xz, vec2(1.0));
	// vec4 tex=texture(blockTextures, vec3((texcoord.st)*texScale + xzPos, BLOCK_TEX_SLOT(blockinfo)));
	// tex.rgb *= vec3(0.267, 0.451, 0.208)*1.5f;
#else
	// vec4 tex=textureLod(blockTextures, vec3(texcoord.st, BLOCK_TEX_SLOT(blockinfo)), 0 );
	vec4 tex=texture(blockTextures, vec3(texcoord.st, BLOCK_TEX_SLOT(blockinfo)));
	// tex = vec4(vec3(1),1);
#ifndef FAR_BLOCKFACE
#endif
 //!MODEL_RENDER
#endif
	if (tex.a<1)
		discard;
	 // tex = vec4(vec3(1),1);
	vec3 color_adj = tex.rgb;
	color_adj *= color.rgb;
	srgbToLin(color_adj.rgb);
	// float lum = dot(color_adj, vec3(0.3));

	//MINECRAFTISH BLOCK LIGHTING
	color_adj *= 1;
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


#ifndef MODEL_RENDER
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
#else
	float lightLevelBlock =  0.0;
	float lightLevelSky =  0.5;
#endif

	// int timeW = mod(floor(FRAME_TIME), 20) > 10 ? 1 : 0;

	vec3 outNormal = normal;
#ifndef MODEL_RENDER

		//TODO: figure out something better 
	// if (IS_LEAVES(blockid)) { //EXPENSIVE LEAVE
	// 	colorizeLeaves(color_adj, position.xyz);
	// }
	// if (blockid == 1) {
 		// vec2 newCoords = texcoord.st;//
		mat3 tbnMat = mat3(matrix_tbn.mat[faceDir]);
		// float height = texture(normalTextures, texcoord.st).a;
		 //Our heightmap only has one color channel.
		 // const vec2 scaleBias = vec2(0.04, 0.02);
		 // float v = height * scaleBias.r - scaleBias.g; 
		// vec3 eye = normalize(tbnMat * (CAMERA_POS-position.xyz));
 		// newCoords = texcoord.st + (eye.xy * v);
		//  tex=texture(blockTextures, vec3(newCoords, BLOCK_TEX_SLOT(blockinfo)));
		//  color_adj = tex.rgb;
		// color_adj *= color.rgb;
		// srgbToLin(color_adj.rgb);
		vec3 normalMapTex=texture(normalTextures, vec3(texcoord.st, BLOCK_TEX_SLOT(blockinfo))).xzy * 2.0 - 1.0; // swizzling is important here
		// normalMapTex *= 1/1.1;

		// vec3 normalMapTex = texture(normalTest, newCoords).xzy * 2.0 - 1.0; // swizzling is important here
		outNormal = normalize((tbnMat * normalMapTex));
		// if (length(outNormal) > 1) {
		// 	color_adj=vec3(1,0,0);
		// }
	// }	

#endif
	// color_adj*=ambientOccl;
	float alpha = tex.a*1;
    out_Color = vec4(color_adj, alpha);
    out_Normal = vec4((outNormal) * 0.5f + 0.5f, 1);
    out_Material = blockinfo;
    out_Light = vec4(lightLevelSky, lightLevelBlock, ambientOccl, 1);
    // gl_FragData[0] = vec4(0,1,1,1);
}
