#version 150 core


#pragma define "NORMAL_MAPPING"
#pragma define "FAR_BLOCKFACE"
#pragma define "MODEL_RENDER"
#pragma include "ubo_scene.glsl"
#pragma include "ubo_constants.glsl"
#pragma include "tonemap.glsl"
#pragma include "util.glsl"
#pragma include "blockinfo.glsl"

uniform sampler2DArray blockTextures;
uniform sampler2DArray normalTextures;


flat in uvec4 blockinfo;
flat in vec4 faceAO;
flat in vec4 faceLight;
flat in vec4 faceLightSky;

in vec4 color;
in vec3 normal;
in vec2 texcoord;
in vec4 position;
in vec2 light;
in float camDistance;
in vec2 texPos;
in float roughness;

out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;

void main(void) {
#ifdef MODEL_RENDER
	vec4 tex = vec4(vec3(1), 1);
#else
	vec4 tex=texture(blockTextures, vec3(texcoord.st, BLOCK_TEX_SLOT(blockinfo)));
#ifndef FAR_BLOCKFACE
#endif
 //!MODEL_RENDER
#endif
	if (tex.a<1)
		discard;
	vec3 color_adj = tex.rgb;
	vec3 color_adj2 = color.rgb;
	linearizeInput(color_adj.rgb);
	linearizeInput2(color_adj2.rgb);
	color_adj *= color_adj2.rgb;



	float xPos2 = texPos.x;
	float xPos = 1-texPos.x;
	float yPos2 = texPos.y;
	float yPos = 1-texPos.y;

	float ao =  0.0;
	ao += faceAO.x * xPos  * yPos;
	ao += faceAO.y * xPos2 * yPos;
	ao += faceAO.z * xPos2 * yPos2;
	ao += faceAO.w * xPos  * yPos2;
	float ambientOccl = 1 - clamp(ao, 0,1);
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


	#ifdef NORMAL_MAPPING
	#define NORMAL_DISTANCE 32.0
	#define NORMAL_FADEOUT 4.0
	float indexNormalMap = BLOCK_NORMAL_SLOT(blockinfo);
	if (indexNormalMap > 0) { //figure out something better, this skips normal mapping for all non (greedy)meshed faces

 		uint faceDir = BLOCK_FACEDIR(blockinfo);
		if (camDistance < NORMAL_DISTANCE)
		{
	        float scale = clamp((NORMAL_DISTANCE-camDistance) / NORMAL_FADEOUT, 0.0, 1.0);
	        // color_adj.r = scale;
	        // color_adj.g = 1.0-scale;
	 		// vec2 newCoords = texcoord.st;//
			mat3 tbnMat = mat3(matrix_tbn.mat[faceDir-1u]);
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
			vec3 normalMapTex=texture(normalTextures, vec3(texcoord.st, indexNormalMap)).xzy * 2.0 - 1.0; // swizzling is important here
			// normalMapTex *= 1/1.1;

			// vec3 normalMapTex = texture(normalTest, newCoords).xzy * 2.0 - 1.0; // swizzling is important here
			outNormal = normalize(outNormal+(tbnMat * normalMapTex)*scale);
			// if (length(outNormal) > 1.01) {
			// 	color_adj=vec3(1,0,0);
			// }

		}
	}	
	#endif

#endif

    out_Color = vec4(color_adj, tex.a);
    out_Normal = vec4((outNormal) * 0.5f + 0.5f, roughness);
    out_Material = blockinfo;
    out_Light = vec4(lightLevelSky, lightLevelBlock, ambientOccl, 1);
}
