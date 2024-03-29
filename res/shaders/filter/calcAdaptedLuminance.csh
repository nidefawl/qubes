#version 430
#extension GL_ARB_shader_image_load_store : enable

layout (local_size_x =1, local_size_y = 1) in;

layout(binding=0, rgba16f) uniform highp image2D currentImage;
layout(binding=1, rgba16f) uniform highp image2D image0;
layout(binding=2, rgba16f) uniform highp image2D image1;

uniform float elapsedTime;


void main()
{
	float currentLum = imageLoad(currentImage, ivec2(0,0)).r;
	float lastLum = imageLoad(image0, ivec2(0,0)).r;
	float newLum = lastLum + (currentLum - lastLum) * ( 1.0 - pow( 0.98f, 30.0 * elapsedTime ) );
	imageStore(image1, ivec2(0,0), vec4(newLum,newLum,newLum,newLum));
}