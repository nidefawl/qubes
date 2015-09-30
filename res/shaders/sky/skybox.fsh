#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "atmosphere.glsl"

#pragma include "basic/sky_scatter.glsl"

uniform sampler2D tex0;
uniform float lightIntens;

in vec4 position;
 
out vec4 out_Color;
 
void main2(void) {
	vec3 eye = in_scene.cameraPosition.xyz;
	vec3 rayDirection = normalize(position.xyz);
    vec3 atmosphere = applyFog(atmosphereColor(rayDirection), 100000.0, eye, rayDirection); 
    out_Color = vec4(atmosphere, 1.0);
}
/*
 float rayleigh_brightness = 1.0;
 float mie_brightness = 0.049;
const float spot_brightness = 12;
float scatter_strength = 0.019;
 float rayleigh_strength = 0.139;
const float mie_strength = 0.77;
const float rayleigh_collection_power = 0.65;
 float mie_collection_power = 0.03;
 float mie_distribution = 0.63;

float surface_height = 0.98;
float range = 0.05;
float intensity = 0.75;
const int step_count = 4;
*/
void main(void) {
	vec3 eye = in_scene.cameraPosition.xyz;
	vec3 viewVector = normalize(position.xyz-eye);
	// intensity += 0.4;
 //    intensity -= lightIntens*0.1;
 rayleigh_strength = 0.139;
 scatter_strength = 0.044;
 mie_collection_power-=0.02;
    mie_collection_power += lightIntens*0.03;
    mie_brightness += 0.1+clamp(lightIntens/4.0,0,1)*0.11;
	vec3 rayDirection = normalize(position.xyz);



    float sunTheta = max( dot(rayDirection, sunDirection), 0.0 );
    sunTheta = pow(sunTheta, 32.0)*1;



    vec3 skySunScat = skyAtmoScat(viewVector, sunDirection.xyz, 0.0);
    vec3 sky = atmosphereColor(rayDirection);

	float scatbr = clamp((skySunScat.r+skySunScat.b+skySunScat.g) / 2.0f, 0, 1);
	sky = mix(sky, sky*skySunScat, 0.2f+sunTheta);
	sky += skySunScat*0.5f;
	// sky += sky*(1.0-scatbr)*1.1f;
	float zfar = in_scene.viewport.w;
	sky = applyFog(sky, zfar*2.5, eye, rayDirection); 



    // sky+=skySunScat;


    out_Color = vec4(sky, 1.0);
}