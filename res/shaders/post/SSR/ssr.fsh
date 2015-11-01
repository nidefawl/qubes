#version 150 core

#pragma include "ubo_scene.glsl"

uniform sampler2D texColor;
uniform sampler2D texNormals;
uniform usampler2D texMaterial;
uniform sampler2D texDepth;

uniform mat4 inverseProj;
uniform mat4 pixelProj;

uniform mat4 worldMVP;

in vec2 pass_texcoord;
flat in vec3 clipInfo;
flat in vec2 renderBufferSize;
flat in float nearPlaneZ;
flat in float farPlaneZ;

float reconstructCSZ(float depthBufferValue) {
    return clipInfo[0] / (depthBufferValue * clipInfo[1] + clipInfo[2]);
}

float reconstructCSZ2(float depth) //same as above
{
    return 2.0f * nearPlaneZ * farPlaneZ / (farPlaneZ + nearPlaneZ - (2.0f * depth - 1.0f) * (farPlaneZ - nearPlaneZ));
}
float Linear01Depth(float depth) {
	float camSpaceZ = reconstructCSZ(depth);
	float clipSpaceZ= (camSpaceZ-nearPlaneZ) / (farPlaneZ-nearPlaneZ);
	return clipSpaceZ;
}
#pragma include "math.glsl"


out vec4 out_Color;


float getBrightness(vec2 b) {
	return (1-pow(1-b.x, 2))*(1-pow(1-b.y, 2));
}
#pragma define "SSR"

#ifdef SSR_1
const float _Iterations = 16;							// maximum ray iterations 
const float _PixelStride = 18;							// number of pixels per ray step close to camera
const float _PixelStrideZCuttoff = 152;					// ray origin Z at this distance will have a pixel stride of 1.0
const float _BinarySearchIterations = 4;				// maximum binary search refinement iterations
const float _PixelZSize = 32.0f;							// Z size in camera space of a pixel in the depth buffer
const float _MaxRayDistance = 41024.0f;						// maximum distance of a ray
const float _ScreenEdgeFadeStart = 0.85f;					// distance to screen edge that ray hits will start to fade (0.0 -> 1.0)

#endif
#ifdef SSR_2
const float _Iterations = 24;							// maximum ray iterations 
const float _PixelStride = 25;							// number of pixels per ray step close to camera
const float _PixelStrideZCuttoff = 80;					// ray origin Z at this distance will have a pixel stride of 1.0
const float _BinarySearchIterations = 4;				// maximum binary search refinement iterations
const float _PixelZSize = 3.1f;							// Z size in camera space of a pixel in the depth buffer
const float _MaxRayDistance = 2222.0f;						// maximum distance of a ray
const float _ScreenEdgeFadeStart = 0.85f;					// distance to screen edge that ray hits will start to fade (0.0 -> 1.0)

#endif


// aka "CANT PLAY"
#ifdef SSR_3 
const float _Iterations = 512;							// maximum ray iterations 
const float _PixelStride = 4;							// number of pixels per ray step close to camera
const float _PixelStrideZCuttoff = 1;					// ray origin Z at this distance will have a pixel stride of 1.0
const float _BinarySearchIterations = 4;				// maximum binary search refinement iterations
const float _PixelZSize = 32.f;							// Z size in camera space of a pixel in the depth buffer
const float _MaxRayDistance = 41024.0f;						// maximum distance of a ray
const float _ScreenEdgeFadeStart = 0.85f;					// distance to screen edge that ray hits will start to fade (0.0 -> 1.0)

#endif
/*

const float _Iterations = 8;							// maximum ray iterations
const float _BinarySearchIterations = 2;				// maximum binary search refinement iterations
const float _PixelZSize = 16.0f;							// Z size in camera space of a pixel in the depth buffer
const float _PixelStride = 24;							// number of pixels per ray step close to camera
const float _PixelStrideZCuttoff = 222;					// ray origin Z at this distance will have a pixel stride of 1.0
const float _MaxRayDistance = 1024.0f;						// maximum distance of a ray
const float _ScreenEdgeFadeStart = 0.85f;					// distance to screen edge that ray hits will start to fade (0.0 -> 1.0)

*/

// const float _Iterations = 16;							// maximum ray iterations
// const float _BinarySearchIterations = 2;				// maximum binary search refinement iterations
// const float _PixelZSize = 16.0f;							// Z size in camera space of a pixel in the depth buffer
// const float _PixelStride = 16;							// number of pixels per ray step close to camera
// const float _PixelStrideZCuttoff = 222;					// ray origin Z at this distance will have a pixel stride of 1.0
// const float _MaxRayDistance = 1024.0f;						// maximum distance of a ray
// const float _ScreenEdgeFadeStart = 0.85f;					// distance to screen edge that ray hits will start to fade (0.0 -> 1.0)



// NEEDS BLUR, LOTS OF



float lastcameraz=0;
bool isCamSpaceZ = false;
bool rayIntersectsDepthBF( float zMin, float zMax, float2 uv)
{
	float depthFrag = texelFetch( texDepth, int2(uv), 0).r;
	float cameraZ = Linear01Depth(depthFrag) * - farPlaneZ;
    return zMax <= cameraZ && zMin >= cameraZ - _PixelZSize;
}

// Trace a ray in screenspace from rayOrigin (in camera space) pointing in rayDirection (in camera space)
// using jitter to offset the ray based on (jitter * _PixelStride).
//
// Returns true if the ray hits a pixel in the depth buffer
// and outputs the hitPixel (in UV space), the hitPoint (in camera space) and the number
// of iterations it took to get there.
//
// Based on Morgan McGuire & Mike Mara's GLSL implementation:
// http://casual-effects.blogspot.com/2014/08/screen-space-ray-tracing.html
bool traceScreenSpaceRay( float3 rayOrigin, 
						  		 float3 rayDirection, 
						  		 float jitter, float angle, 
						  		 out float2 hitPixel, 
						  		 out float3 hitPoint, 
						  		 out float iterationCount) 
{
	vec2 _OneDividedByRenderBufferSize = vec2(1.0f) / renderBufferSize;
	// Clip to the near plane    
	float rayLength = ((rayOrigin.z + rayDirection.z * _MaxRayDistance) > -nearPlaneZ) ?
	    			  (-nearPlaneZ - rayOrigin.z) / rayDirection.z : _MaxRayDistance;
	float3 rayEnd = rayOrigin + rayDirection * rayLength;

	// Project into homogeneous clip space
	float4 H0 = pixelProj * float4( rayOrigin, 1.0);
	float4 H1 = pixelProj * float4( rayEnd, 1.0);

	float k0 = 1.0 / H0.w, k1 = 1.0 / H1.w;

	// The interpolated homogeneous version of the camera-space points  
	float3 Q0 = rayOrigin * k0, Q1 = rayEnd * k1;
		
	// Screen-space endpoints
	float2 P0 = H0.xy * k0, P1 = H1.xy * k1;

	// If the line is degenerate, make it cover at least one pixel
	// to avoid handling zero-pixel extent as a special case later
	P1 += (distanceSquared(P0, P1) < 0.0001) ? 0.01 : 0.0;

	float2 delta = P1 - P0;

    // Permute so that the primary iteration is in x to reduce
    // large branches later
    bool permute = (abs(delta.x) < abs(delta.y));
	if (permute) {
		// More-vertical line. Create a permutation that swaps x and y in the output
        // by directly swizzling the inputs.
		delta = delta.yx;
		P1 = P1.yx;
		P0 = P0.yx;        
	}

	float stepDir = sign(delta.x);
	float invdx = stepDir / delta.x;

	// Track the derivatives of Q and k
	float3  dQ = (Q1 - Q0) * invdx;
	float dk = (k1 - k0) * invdx;
	float2  dP = float2(stepDir, delta.y * invdx);

		// Calculate pixel stride based on distance of ray origin from camera.
		// Since perspective means distant objects will be smaller in screen space
		// we can use this to have higher quality reflections for far away objects
		// while still using a large pixel stride for near objects (and increase performance)
		// this also helps mitigate artifacts on distant reflections when we use a large
		// pixel stride.
		float strideScaler = 1 - min( 1.0, -rayOrigin.z / _PixelStrideZCuttoff);
		float pixelStride = 1 + strideScaler * _PixelStride;

	// Scale derivatives by the desired pixel stride and then
	// offset the starting values by the jitter fraction
	dP *= pixelStride; dQ *= pixelStride; dk *= pixelStride;
	P0 += dP * jitter; Q0 += dQ * jitter; k0 += dk * jitter;

	float i, rayZMin = 0.0, rayZMax = 0.0;

	// Track ray step and derivatives in a float4 to parallelize
	float4 pqk = float4( P0, Q0.z, k0);
	float4 dPQK = float4( dP, dQ.z, dk);
	bool intersect = false;

	for( i=0; i<_Iterations && intersect == false; i++)
	{
    	pqk += dPQK;
    	
    	rayZMin = rayZMax;
    	rayZMax = (dPQK.z * 0.5 + pqk.z) / (dPQK.w * 0.5 + pqk.w);
    	swapIfBigger( rayZMax, rayZMin);
    	
    	hitPixel = permute ? pqk.yx : pqk.xy;
    	// hitPixel *= _OneDividedByRenderBufferSize;
        
        intersect = rayIntersectsDepthBF( rayZMin, rayZMax, hitPixel);
	}

	// Binary search refinement
	if( pixelStride > 1.0 && intersect)
	{
		pqk -= dPQK;
		dPQK /= pixelStride;
		
		float originalStride = pixelStride * 0.5;
		float stride = originalStride;
		
		rayZMin = pqk.z / pqk.w;
		rayZMax = rayZMin;
		
		for( float j=0; j<_BinarySearchIterations; j++)
	    {
	    	pqk += dPQK * stride;
	    	
	    	rayZMin = rayZMax;
			rayZMax = (dPQK.z * -0.5 + pqk.z) / (dPQK.w * -0.5 + pqk.w);
			swapIfBigger( rayZMax, rayZMin);
    		float2 newHit = permute ? pqk.yx : pqk.xy;;
			// if (newHit.x < 0 || newHit.y < 0 || newHit.x > renderBufferSize.x || newHit.y > renderBufferSize.y) {
			// 	break;
			// }
	    	hitPixel = newHit;
	        
	        originalStride *= 0.5;
	        stride = rayIntersectsDepthBF( rayZMin, rayZMax, hitPixel) ? -originalStride : originalStride;
	    }
	}


	Q0.xy += dQ.xy * i;
	Q0.z = pqk.z;
	hitPoint = Q0 / pqk.w;
	iterationCount = i;
	    	
	return intersect;
}
float calculateAlphaForIntersection( bool intersect, 
								   float iterationCount, 
								   float specularStrength,
								   float2 hitPixel,
								   float3 hitPoint,
								   float3 vsRayOrigin,
								   float3 vsRayDirection)
{
	float alpha = 1;

	// Fade ray hits that approach the maximum iterations
	float distBlend = pow((iterationCount / _Iterations), 1);
	alpha *= 1.0 - clamp(distBlend*0.55, 0, 1);

	// Fade ray hits based on distance from ray origin
	alpha *= 1.0 - clamp( distance( vsRayOrigin, hitPoint) / _MaxRayDistance, 0.0, 1.0);

	alpha = max(0.01, alpha);
	alpha = min( 1.0, alpha*specularStrength * 1.0);
	// Fade ray hits that approach the screen edge
	float screenFade = _ScreenEdgeFadeStart;
	hitPixel /= renderBufferSize;
	float2 hitPixelNDC = (hitPixel * 2.0 - 1.0);
	float maxDimension = min( 1.0, max( abs( hitPixelNDC.x), abs( hitPixelNDC.y)));
	float screenFadeA = ((max( 0.0, maxDimension - screenFade) / (1.0 - screenFade)));

	alpha *= 1.0 - pow(screenFadeA, 2);

	return alpha*float(intersect);
}
bool inScreen(vec2 hitPixel) {
	return hitPixel.x >= 0 && hitPixel.y >= 0 && hitPixel.x < renderBufferSize.x && hitPixel.y < renderBufferSize.y;
}

void main(void) {
	vec4 albedo = texture(texColor, pass_texcoord);
	vec3 normal = texture(texNormals, pass_texcoord).rgb * 2.0f - 1.0f;
    uvec4 blockinfo = texture(texMaterial, pass_texcoord, 0);
	float depthFrag = texture(texDepth, pass_texcoord).r;
	float cameraZ = Linear01Depth(depthFrag);
    vec4 cameraRay = inverseProj * vec4(pass_texcoord.s * 2.0f - 1.0f, pass_texcoord.t * 2.0f - 1.0f, 1, 1.0f);


    cameraRay /= cameraRay.w;
    // cameraRay.y+=0.4;
    vec3 camRay3 = vec3(cameraRay.xyz);
    camRay3 *= cameraZ;

	float3 vsRayOrigin = camRay3.xyz;
	// vsRayOrigin.y-=2.51;
	vec4 normal4 = in_matrix_3D.view *  vec4(normal.xyz, 1);
	normal4.xyz/=normal4.w;
	float3 nvsRayOrigin = normalize( vsRayOrigin);
	float3 vsRayDirection = normalize( reflect( nvsRayOrigin, normalize(normal4.xyz)));
	vec4 textureSpace = pixelProj * cameraRay;
	textureSpace = textureSpace / textureSpace.w;


	float2 hitPixel; 
	float3 hitPoint;
	float iterationCount;
	float2 uv2 = pass_texcoord * renderBufferSize;
	float c = (uv2.x + uv2.y);
	float jitter = mod(c*1,1);
 			float angle = saturate( dot(nvsRayOrigin, normal4.xyz) + 1.0 );
	// jitter = 0;
	bool hit = traceScreenSpaceRay( vsRayOrigin, vsRayDirection, jitter, angle, hitPixel, hitPoint, iterationCount);

	uint blockidPixel = (blockinfo.y&0xFFFu);
	float isWater = float(blockidPixel==4u);
	float specularStrength = isWater;
	float alpha = calculateAlphaForIntersection( hit, iterationCount, specularStrength, hitPixel, hitPoint, vsRayOrigin, vsRayDirection);
	// float alpha = specularStrength;

	vec4 cOut = vec4(albedo.rgb, 1);
	if (isWater>0) {
	if (hit) {
		if (!inScreen(hitPixel)) {
			// cOut = vec4(1,1,0,1);
		} else {
 			// angle *= angle;
			uvec4 type = texelFetch(texMaterial, int2(hitPixel), 0);
  			uint blockidHit = (type.y&0xFFFu);
			if (blockidHit!=0u) {
				vec4 refl = texelFetch(texColor, int2(hitPixel), 0);
				//alpha = mix(alpha, 0, float(blockid==0u));
				cOut = vec4(refl.rgb,  min(1, alpha*angle*1.8));// isWater*float(type.y!=0u)*0.7);
			// cOut = vec4(1,0,0,1);
				// cOut = vec4(vec3(jitter),1);
				// cOut = vec4(1,1,0,1);
			} else {
			// cOut = vec4(1,0,1,1);
			}
		}
	} else {
			// cOut = vec4(1,0,0,1);
	}
	}
	out_Color = cOut;
}
