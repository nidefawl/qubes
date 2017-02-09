#pragma define "Z_INVERSE" "0"


#if Z_INVERSE
#define DEPTH_FAR 0.00000001
#define DEPTH_NEAR 1.0
#else
#define DEPTH_FAR 1.0
#define DEPTH_NEAR -1.0
#endif
vec4 unprojectPos(in vec2 coord, in float depth) { 
#if Z_INVERSE
    vec4 fragposition = inverse(in_matrix_3D.vp) * vec4(coord.s * 2.0f - 1.0f, coord.t * 2.0f - 1.0f, depth, 1.0f);
#else
    vec4 fragposition = inverse(in_matrix_3D.vp) * vec4(coord.s * 2.0f - 1.0f, coord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
#endif
    fragposition /= fragposition.w;
    return fragposition;
}
vec4 screencoord(vec2 texcoord, float depth) {
#if Z_INVERSE
    return vec4(texcoord.s * 2.0f - 1.0f, texcoord.t * 2.0f - 1.0f, depth, 1.0f);
#else
    return vec4(texcoord.s * 2.0f - 1.0f, texcoord.t * 2.0f - 1.0f, 2.0f * depth - 1.0f, 1.0f);
#endif
}


vec4 unprojectScreenCoord(in vec4 screcrd) { 
    vec4 fragposition = in_matrix_3D.proj_inv * screcrd;
    fragposition /= fragposition.w;
    return fragposition;
}


float linearizeDepth(in float depth)
{	
#if Z_INVERSE
    // return (Z_NEAR * Z_FAR) / (depth * (Z_FAR - Z_NEAR)); // with far plane
    return Z_NEAR / depth; // without far plane (far at infinity)
#else
    return 2.0 * Z_NEAR * Z_FAR / (Z_FAR + Z_NEAR - (2.0 * depth - 1.0) * (Z_FAR - Z_NEAR));
#endif
}