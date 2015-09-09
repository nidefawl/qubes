#version 150 core


// #pragma include "ubo_scene.glsl"

uniform sampler2DArray blockTextures;


in vec4 color;
in vec3 normal;
in vec4 texcoord;
in vec2 light;
flat in vec4 faceAO;
flat in vec4 faceLight;
flat in vec4 faceLightSky;

in vec2 texPos;
flat in uvec4 blockinfo;


out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;


float getBrightness(vec2 b) {
	return (1-pow(1-b.x, 2))*(1-pow(1-b.y, 2));
}


float lightAdj(float sky, float block) {
	// x = 1 - x;
	// return 1-x*x*x*(x*(x*6 - 15) + 10);
	const float minLevel = 0.1;
	return minLevel+clamp((sky+block)*(1-minLevel), 0, (1-minLevel));
}
void main() {

	vec4 tex = texture(blockTextures, vec3(texcoord.st, float(blockinfo.x)));

	float xPos2 = texPos.x;
	float xPos = 1-texPos.x;
	float yPos2 = texPos.y;
	float yPos = 1-texPos.y;
	float ao =  0.0;
	ao += faceAO.x * xPos  * yPos;
	ao += faceAO.y * xPos2 * yPos;
	ao += faceAO.z * xPos2 * yPos2;
	ao += faceAO.w * xPos  * yPos2;
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
	// int timeW = mod(floor(in_matrix.frameTime), 20) > 10 ? 1 : 0;
	float ambientOccl = 1 - clamp(ao, 0,1);
	vec3 color_adj = tex.rgb;
	color_adj *= color.rgb;
	color_adj *= ambientOccl;
	color_adj *= lightAdj(lightSky, lightBlock);
    out_Color = vec4(color_adj, tex.a*color.a);
    out_Normal = vec4((normal) * 0.5f + 0.5f, 1.0f);
    out_Material = blockinfo;
    // gl_FragData[0] = vec4(0,1,1,1);
}
