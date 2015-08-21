#version 120

//#define WAVING_WATER
const float waveHeight = 1.0f;
const float waveWidth = 0.1f;
uniform int worldTime;

uniform vec3 cameraPosition;

uniform float frameTimeCounter;

uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;

varying vec4 color;
varying vec4 texcoord;
varying vec4 lmcoord;
varying vec3 worldPosition;
varying vec4 vertexPos;

varying vec3 normal;
varying vec3 globalNormal;
varying vec3 tangent;
varying vec3 binormal;
varying vec3 viewVector;
varying float vdistance;

attribute vec4 blockinfo; // x == blockid, y == rendertype, z = metadata
//attribute vec4 mc_Entity;

varying float iswater;
varying float isice;
flat varying int blockTexture;

void main() {

	blockTexture = int(blockinfo.x);
	iswater = 0.0f;
	isice = 0.0f;


	
	/*if (mc_Entity.x == 79) {
		isice = 1.0f;
	}*/
	
		 vertexPos = gl_Vertex;

/*	if (mc_Entity.x == 1971.0f)
	{
		iswater = 1.0f;
	}
	
	if (mc_Entity.x == 8 || mc_Entity.x == 9) {
		iswater = 1.0f;
	}*/
		iswater = 1.0f;
	

		
	vec4 viewPos = gbufferModelViewInverse * gl_ModelViewMatrix * vertexPos;
	vec4 position = viewPos;

	worldPosition.xyz = viewPos.xyz + cameraPosition.xyz;

	vec4 localPosition = gbufferModelView * vertexPos;

	vdistance = length(localPosition.xyz);

	gl_Position = gl_ProjectionMatrix * (gbufferModelView * position);

	
	color = gl_Color;
	
	texcoord = gl_TextureMatrix[0] * gl_MultiTexCoord0;
	lmcoord = gl_TextureMatrix[1] * gl_MultiTexCoord1;
	
// lmcoord.x=0.78f;

	gl_FogFragCoord = gl_Position.z;


	
	
	normal = normalize(gl_NormalMatrix * gl_Normal);
	globalNormal = normalize(gl_Normal);

	if (gl_Normal.x > 0.5) {
		//  1.0,  0.0,  0.0
		tangent  = normalize(gl_NormalMatrix * vec3( 0.0,  0.0, -1.0));
		binormal = normalize(gl_NormalMatrix * vec3( 0.0, -1.0,  0.0));
	} else if (gl_Normal.x < -0.5) {
		// -1.0,  0.0,  0.0
		tangent  = normalize(gl_NormalMatrix * vec3( 0.0,  0.0,  1.0));
		binormal = normalize(gl_NormalMatrix * vec3( 0.0, -1.0,  0.0));
	} else if (gl_Normal.y > 0.5) {
		//  0.0,  1.0,  0.0
		tangent  = normalize(gl_NormalMatrix * vec3( 1.0,  0.0,  0.0));
		binormal = normalize(gl_NormalMatrix * vec3( 0.0,  0.0,  1.0));
	} else if (gl_Normal.y < -0.5) {
		//  0.0, -1.0,  0.0
		tangent  = normalize(gl_NormalMatrix * vec3( 1.0,  0.0,  0.0));
		binormal = normalize(gl_NormalMatrix * vec3( 0.0,  0.0,  1.0));
	} else if (gl_Normal.z > 0.5) {
		//  0.0,  0.0,  1.0
		tangent  = normalize(gl_NormalMatrix * vec3( 1.0,  0.0,  0.0));
		binormal = normalize(gl_NormalMatrix * vec3( 0.0, -1.0,  0.0));
	} else if (gl_Normal.z < -0.5) {
		//  0.0,  0.0, -1.0
		tangent  = normalize(gl_NormalMatrix * vec3(-1.0,  0.0,  0.0));
		binormal = normalize(gl_NormalMatrix * vec3( 0.0, -1.0,  0.0));
	}
	
	mat3 tbnMatrix = mat3(tangent.x, binormal.x, normal.x,
                          tangent.y, binormal.y, normal.y,
                          tangent.z, binormal.z, normal.z);

	viewVector = (gl_ModelViewMatrix * vertexPos).xyz;
	viewVector = normalize(tbnMatrix * viewVector);


	
}