#version 150 core
 
#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

layout(std140) uniform LightInfo {
  vec4 dayLightTime; 
  vec4 posSun; // Light position in world space
  vec4 lightDir; // Light dir in world space
  vec4 La; // Ambient light intensity
  vec4 Ld; // Diffuse light intensity
  vec4 Ls; // Specular light intensity
} SkyLight;


out vec4 color;

out vec2 texcoord;

out vec3 lightVector;
out vec3 sunVec;
out vec3 moonVec;
out vec3 upVec;

out vec3 sunlight;
out vec3 moonlight;
out vec3 ambient_color;
out vec4 lightS;

out float SdotU;
out float MdotU;
out float sunVisibility;
out float moonVisibility;

uniform ivec2 eyeBrightnessSmooth;
uniform int worldTime;
uniform float rainStrength;


////////////////////sunlight color////////////////////
////////////////////sunlight color////////////////////
////////////////////sunlight color////////////////////
const ivec4 ToD[25] = ivec4[25](ivec4( 0,200,134,48), //hour,r,g,b
								ivec4( 1,200,134,48),
								ivec4( 2,200,134,48),
								ivec4( 3,200,134,48),
								ivec4( 4,200,134,48),
								ivec4( 5,200,134,48),
								ivec4( 6,200,134,48),
								ivec4( 7,200,148,79),
								ivec4( 8,200,164,109),
								ivec4( 9,200,177,140),
								ivec4(10,200,188,169),
								ivec4(11,200,200,200),	
								ivec4(12,200,200,200),	
								ivec4(13,200,200,200),
								ivec4(14,200,188,169),
								ivec4(15,200,177,140),
								ivec4(16,200,164,109),
								ivec4(17,200,148,79),
								ivec4(18,200,134,48),
								ivec4(19,200,134,48),
								ivec4(20,200,134,48),
								ivec4(21,200,134,48),
								ivec4(22,200,134,48),
								ivec4(23,200,134,48),
								ivec4(24,200,134,48));
vec3 sky_color = ivec3(60,170,255)/255.0;
float fx(float x) {
return (2 *(-sin(x)*sin(x)*sin(x) + 3*sin(x) + 3*x)) / 3;

}
float fx2(float x) {
return (-cos(x) * sin(x) + 6*x) / 2;

}

void main() {
	vec4 viewspace = in_matrix_3D.view*SkyLight.lightDir;
	// viewspace.xyz/=viewspace.w;
	vec4 viewspace2 = in_matrix_3D.view*vec4(0.0, 1.0, 0.0, 0.0);
	// viewspace2.xyz/=viewspace2.w;
	vec3 upPosition = viewspace2.xyz;
	vec3 sunPosition = viewspace.xyz;
	// worldTime = 7000;
	moonlight = ivec3(1,3,5)/255.0/2.2;
	// gl_Position = ftransform();
	// texcoord = gl_MultiTexCoord0;
	texcoord = in_texcoord.st;

	if (worldTime < 12700 || worldTime > 23250) {
		lightVector = normalize(sunPosition);
	}
	else {
		lightVector = normalize(-sunPosition);
	}
	
	sunVec = normalize(sunPosition);
	moonVec = normalize(-sunPosition);
	upVec = normalize(upPosition);

	
	SdotU = dot(sunVec,upVec);
	MdotU = dot(moonVec,upVec);
	sunVisibility = pow(clamp(SdotU+0.1,0.0,0.1)/0.1,2.0);
	moonVisibility = pow(clamp(MdotU+0.1,0.0,0.1)/0.1,2.0);
	
	float hour = mod(worldTime/1000.0+6.0,24);
	//if (hour > 24.0) hour = hour - 24.0;
	
	ivec4 temp = ToD[int(floor(hour))];
	ivec4 temp2 = ToD[int(floor(hour)) + 1];
	
	sunlight = mix(vec3(temp.yzw),vec3(temp2.yzw),(hour-float(temp.x))/float(temp2.x-temp.x))/255.0f;
	

sky_color = pow(sky_color,vec3(2.2));
vec3 nsunlight = normalize(mix(pow(sunlight,vec3(2.2)),vec3(0.25,0.3,0.4),rainStrength));
sky_color = normalize(mix(sky_color,vec3(0.25,0.3,0.4),rainStrength)); //normalize colors in order to don't change luminance
vec3 sVector = normalize(upVec);
const float PI = 3.14159265359;
float cosT = 1.; 
float T = acos(cosT);
float absCosT = abs(cosT);
float cosS = SdotU;
float S = acos(cosS);				
float cosY = cosS;
float Y = acos(cosY);	
		
lightS.x = (fx(Y+PI/2.0)-fx(Y-PI/2.0))*2.0;
lightS.y = (fx2(T+PI/2.0)-fx2(T-PI/2.0))*1.2;
float tL = (lightS.x+ lightS.y)/6.28;

//moon sky color
float McosS = MdotU;
float MS = acos(McosS);
float McosY = MdotU;
float MY = acos(McosY);

lightS.z = (fx(MY+PI/2.0)-fx(MY-PI/2.0))*3.0;
lightS.w = (fx2(T+PI/2.0)-fx2(T-PI/2.0));
float tLMoon = (lightS.z + lightS.w)/6.28;

ambient_color = mix(sky_color, nsunlight,1-exp(-0.16*tL*(1-rainStrength*0.8)))*tL*sunVisibility*(1-rainStrength*0.8) + tLMoon*moonVisibility*moonlight;


	// handItemLight = 0.0;
	// if (heldItemId == 50) {
	// 	// torch
	// 	handItemLight = 0.5;
	// }
	
	// else if (heldItemId == 76 || heldItemId == 94) {
	// 	// active redstone torch / redstone repeater
	// 	handItemLight = 0.1;
	// }
	
	// else if (heldItemId == 89) {
	// 	// lightstone
	// 	handItemLight = 0.6;
	// }
	
	// else if (heldItemId == 10 || heldItemId == 11 || heldItemId == 51) {
	// 	// lava / lava / fire
	// 	handItemLight = 0.5;
	// }
	
	// else if (heldItemId == 91) {
	// 	// jack-o-lantern
	// 	handItemLight = 0.6;
	// }
	
	
	// else if (heldItemId == 327) {
	// 	handItemLight = 0.2;
	// }
	
	// float hour = mod(worldTime/1000.0+6.0,24);
	// //if (hour > 24.0) hour = hour - 24.0;
	
	// ivec4 temp = ToD[int(floor(hour))];
	// ivec4 temp2 = ToD[int(floor(hour)) + 1];
	color = in_color;
	gl_Position = in_matrix_2D.mvp * in_position;
}

