#version 150 core


// #pragma include "ubo_scene.glsl"

uniform sampler2DArray blockTextures;


in vec4 color;
in vec3 normal;
in vec4 texcoord;
flat in vec4 faceAO;

in vec2 texPos;
flat in uvec4 blockinfo;


out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;


float getBrightness(vec2 b) {
	return (1-pow(1-b.x, 2))*(1-pow(1-b.y, 2));
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
	// int timeW = mod(floor(in_matrix.frameTime), 20) > 10 ? 1 : 0;
	float brightness = 1 - clamp(ao, 0,1);
    out_Color = vec4(tex.rgb*color.rgb*brightness, vec3(tex.a*color.a));
    out_Normal = vec4((normal) * 0.5f + 0.5f, 1.0f);
    out_Material = blockinfo;
    // gl_FragData[0] = vec4(0,1,1,1);
}
