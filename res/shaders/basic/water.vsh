#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

// uniform vec4 terroffset;
out vec4 color;
out vec4 texcoord;
out vec3 normal;
flat out uvec4 blockinfo;

flat out vec4 faceLight;
flat out vec4 faceLightSky;
out vec2 texPos;
out vec3 tangent;
out vec3 binormal;
out mat3 normalMat;
out mat3 tbnMatrix;
out vec4 vpos;
out vec4 vwpos;


#define LIGHT_MASK 0xFu

void main() {
	texcoord = in_texcoord;
	vec4 camNormal = in_matrix_3D.normal * vec4(in_normal.xyz, 1);
	camNormal.xyz/=camNormal.w;

	normal = normalize(camNormal.xyz);

	normalMat = mat3(in_matrix_3D.normal);
	if (in_normal.x > 0.5) {
		//  1.0,  0.0,  0.0
		tangent  = normalize(normalMat * vec3( 0.0,  0.0, -1.0));
		binormal = normalize(normalMat * vec3( 0.0, -1.0,  0.0));
	} else if (in_normal.x < -0.5) {
		// -1.0,  0.0,  0.0
		tangent  = normalize(normalMat * vec3( 0.0,  0.0,  1.0));
		binormal = normalize(normalMat * vec3( 0.0, -1.0,  0.0));
	} else if (in_normal.y > 0.5) {
		//  0.0,  1.0,  0.0
		tangent  = normalize(normalMat * vec3( 1.0,  0.0,  0.0));
		binormal = normalize(normalMat * vec3( 0.0,  0.0,  1.0));
	} else if (in_normal.y < -0.5) {
		//  0.0, -1.0,  0.0
		tangent  = normalize(normalMat * vec3( 1.0,  0.0,  0.0));
		binormal = normalize(normalMat * vec3( 0.0,  0.0,  1.0));
	} else if (in_normal.z > 0.5) {
		//  0.0,  0.0,  1.0
		tangent  = normalize(normalMat * vec3( 1.0,  0.0,  0.0));
		binormal = normalize(normalMat * vec3( 0.0, -1.0,  0.0));
	} else if (in_normal.z < -0.5) {
		//  0.0,  0.0, -1.0
		tangent  = normalize(normalMat * vec3(-1.0,  0.0,  0.0));
		binormal = normalize(normalMat * vec3( 0.0, -1.0,  0.0));
	}
	
	tbnMatrix = mat3(tangent.x, binormal.x, normal.x,
                          tangent.y, binormal.y, normal.y,
                          tangent.z, binormal.z, normal.z);



	color = in_color;

	faceLightSky = vec4(
		float(in_light.y&LIGHT_MASK)/15.0,
		float((in_light.y>>4)&LIGHT_MASK)/15.0,
		float((in_light.y>>8)&LIGHT_MASK)/15.0,
		float((in_light.y>>12)&LIGHT_MASK)/15.0
		);
	faceLight = vec4(
		float(in_light.x&LIGHT_MASK)/15.0,
		float((in_light.x>>4)&LIGHT_MASK)/15.0,
		float((in_light.x>>8)&LIGHT_MASK)/15.0,
		float((in_light.x>>12)&LIGHT_MASK)/15.0
		);

	texPos = clamp(in_texcoord.xy, vec2(0), vec2(1));
	

	blockinfo = in_blockinfo;
	vpos = in_matrix_3D.mv * in_position;
	vwpos = in_position;
	// gl_Position = in_matrix_3D.mvp * (in_position+terroffset);
	gl_Position = in_matrix_3D.mvp * in_position;
}
