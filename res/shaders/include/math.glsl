#ifndef g3dmath_glsl
#define g3dmath_glsl

/**
 Support for some GL 4.0 shader calls on older versions of OpenGL, and 
 support for some HLSL types and functions.
 */      
#if __VERSION__ < 400
    #define textureQueryLod textureQueryLOD
#endif

#if __VERSION__ < 410
#extension GL_ARB_separate_shader_objects : enable
#endif


#if __VERSION__ == 120
#   define texture      texture2D
#   define textureLod   texture2DLod
#   if G3D_SHADER_STAGE == G3D_FRAGMENT_SHADER
vec4 texture2DLod(sampler2D s , vec2 c, int L) { return texture2D(s, c); }
#   endif
#   define texelFetch   texelFetch2D
#   define textureSize  textureSize2D
#endif



// Some constants
const float pi = 3.1415927;
const float invPi = 1.0 / pi;
const float inv8pi = 1.0 / (8.0 * pi);

const float meters      = 1.0;
const float centimeters = 0.01;
const float millimeters = 0.001;
const float inf         = 1.0 / 0.0;


#define Vector2 vec2
#define Point2  vec2
#define Vector3 vec3
#define Point3  vec3
#define Vector4 vec4

#define Color3  vec3
#define Radiance3 vec3
#define Biradiance3 vec3
#define Irradiance3 vec3
#define Radiosity3 vec3
#define Power3 vec3

#define Color4  vec4
#define Radiance4 vec4
#define Biradiance4 vec4
#define Irradiance4 vec4
#define Radiosity4 vec4
#define Power4 vec4

#define Vector2int32 int2
#define Vector3int32 int3
#define Matrix4      mat4
#define Matrix3      mat3
#define Matrix2      mat2


/////////////////////////////////////////////////////////////////////////////
// HLSL compatability

#define uint1 uint
#define uint2 uvec2
#define uint3 uvec3
#define uint4 uvec4

#define int1 int
#define int2 ivec2
#define int3 ivec3
#define int4 ivec4

#define float1 float
#define float2 vec2
#define float3 vec3
#define float4 vec4

#define bool1 bool
#define bool2 bvec2
#define bool3 bvec3
#define bool4 bvec4

#define half float
#define half1 float
#define half2 vec2
#define half3 vec3
#define half4 vec4

#define rsqrt inversesqrt

#define tex2D texture2D

#define lerp mix

#define ddx dFdx
#define ddy dFdy



float square(float x) {
	return x * x;
}
void swap(inout float a, inout float b) {
	float temp = a;
	a = b;
	b = temp;
}
void swapIfBigger( inout float aa, inout float bb)
{
	if( aa > bb)
	{
		float tmp = aa;
		aa = bb;
		bb = tmp;
	}
}
float distanceSquared(Point2 A, Point2 B) {
    A -= B;
    return dot(A, A);
}




float frac(float x) {
    return fract(x);
}

vec2 frac(vec2 x) {
    return fract(x);
}

vec3 frac(vec3 x) {
    return fract(x);
}

vec4 frac(vec4 x) {
    return fract(x);
}

float atan2(float y, float x) {
    return atan(y, x);
}

float saturate(float x) {
    return clamp(x, 0.0, 1.0);
}

vec2 saturate(vec2 x) {
    return clamp(x, vec2(0.0), vec2(1.0));
}

vec3 saturate(vec3 x) {
    return clamp(x, vec3(0.0), vec3(1.0));
}

vec4 saturate(vec4 x) {
    return clamp(x, vec4(0.0), vec4(1.0));
}
#endif
