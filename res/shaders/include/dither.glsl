
#define FRAME_TIME_DITHER (in_scene.framePos.w*20.0)
#define SS_DITHER (in_scene.sceneSettings.x)
vec3 dither8BitSS()
{

    vec3 vDither = vec3(dot(vec2(131.0, 312.0), gl_FragCoord.xy + FRAME_TIME_DITHER));
    return SS_DITHER*((fract(vDither.rgb / vec3(103.0, 71.0, 97.0)) - vec3(0.5, 0.5, 0.5)) / 255.0);
}



float dither8Bit( vec2 n )
{
	float t = fract( FRAME_TIME_DITHER );
	float nrnd0 = fract(sin(dot(( n + 0.07*t ).xy, vec2(12.9898, 78.233)))* 43758.5453);

    // Convert uniform distribution into triangle-shaped distribution.
    float orig = nrnd0*2.0-1.0;
    nrnd0 = orig*inversesqrt(abs(orig));
    nrnd0 = max(-1.0,nrnd0); // Nerf the NaN generated by 0*rsqrt(0). Thanks @FioraAeterna!
    nrnd0 = nrnd0-sign(orig)+0.5;
    
    // convert to [0,1] for histogram.
    // return (nrnd0+0.5) * 0.5;

    // Result is range [-0.5,1.5] which is
    // useful for actual dithering.
    return nrnd0/255.0;
}