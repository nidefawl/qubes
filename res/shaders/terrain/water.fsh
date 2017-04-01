#version 150 core

#define NOISE_TEX_SIZE 64

#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"
#pragma include "ubo_constants.glsl"
#pragma include "blockinfo.glsl"



uniform sampler2DArray blockTextures;
uniform sampler2D waterNoiseTexture;
#define noisetex waterNoiseTexture
#pragma include "water.glsl"
const float rainStrength = 0;


in vec4 color;
in vec2 texcoord;
in vec3 normal;
flat in uvec4 blockinfo;
flat in uint faceDir; // duplicate data, see comment int terrain.fsh
flat in vec4 faceLight;
flat in vec4 faceLightSky;
in vec2 texPos;
in vec4 vpos;
in vec3 vwpos;
in float isWater;
in float roughness;


out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
out vec4 out_Light;

void main() {
	
	#ifdef Watercolor_Vanila
	vec3 watercolor = vec3(1.0);
	float wateropacity = 0.8;
	#endif
	
	#ifdef Watercolor_Clear
	vec3 watercolor = vec3(0.02,0.08,0.14);
	float wateropacity = 0.3;
	#endif
	
	#ifdef Watercolor_Tropical
	vec3 watercolor = vec3(0.1,0.6,0.6);
	float wateropacity = 0.41;
	#endif
	
	#ifdef Watercolor_Legacy
	vec3 watercolor = vec3(0.0,0.3,0.7);
	float wateropacity = 0.7;
	#endif
	
	#ifdef Watercolor_Classic
	vec3 watercolor = vec3(0.1,0.4,0.7);
	float wateropacity = 0.4;
	#endif
	
	#ifdef Watercolor_Original
	vec3 watercolor = vec3(0.02,0.08,0.14);
	float wateropacity = 0.8;
	#endif
	
	vec3 posxz = vwpos.xyz;

	posxz.x += sin(posxz.z+frametime)*0.25;
	posxz.z += cos(posxz.x+frametime)*0.25;
	
	float deltaPos = 0.5;
	float bumpmult = 0.02;
	float h0 = waterH(posxz);
	float h1 = waterH(posxz + vec3(deltaPos,0.0,0.0));
	float h2 = waterH(posxz + vec3(-deltaPos,0.0,0.0));
	float h3 = waterH(posxz + vec3(0.0,0.0,deltaPos));
	float h4 = waterH(posxz + vec3(0.0,0.0,-deltaPos));
	
	float xDelta = ((h1-h0)+(h0-h2))/deltaPos;
	float yDelta = ((h3-h0)+(h0-h4))/deltaPos;
	
	vec3 newnormal = normalize(vec3(xDelta,yDelta,1.0-xDelta*xDelta-yDelta*yDelta));
	newnormal = newnormal + (xDelta*yDelta) / (sin(xDelta) + cos(yDelta)+frametime);
	vec2 texCoordWater = texcoord.st;
	vec3 waterNormal = normal;
	// texCoordWater.s+=h0*0.08f;
	// texCoordWater.t+=h1*0.08f;
	// texCoordWater.s+=h3*0.08f;
	// texCoordWater.t+=h4*0.08f;
    if (isWater > 0 && faceDir != 0u) {
		mat3 tbnMat = mat3(matrix_tbn.mat[faceDir-1u]);
		vec3 bump = newnormal;
			
		
		
		bump = 	bump * vec3(bumpmult, bumpmult, bumpmult) + vec3(0.0f, 0.0f, 1.0f - bumpmult);
		
		// waterNormal = normalize(tbnMat * bump.xzy);
	}
	
	

	vec4 raw = texture(blockTextures, vec3(texCoordWater, BLOCK_TEX_SLOT(blockinfo)));
	// raw.a=1;
    uint blockid = BLOCK_ID(blockinfo);
    float iswater = IS_WATER(blockid);
	float isEyeInWater = 0;
	// vec4 raw = texture(texture, texcoord.xy);
	vec4 tex = vec4(vec3(raw.b + (raw.r+raw.g)),max(wateropacity*(1-isEyeInWater),0.2));
	tex *= vec4(watercolor,1);
	
	#ifdef Watercolor_Vanila
	tex.rgb = raw.rgb*color.rgb;
	#endif
	tex.a = 1;
	
	if (iswater < 0.9) tex = raw*color;

	

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

    out_Color = tex;
    out_Normal = vec4(waterNormal.xyz* 0.5f + 0.5f, roughness);
    out_Material = blockinfo;
    out_Light = vec4(lightLevelSky, lightLevelBlock, 1, 1);
}