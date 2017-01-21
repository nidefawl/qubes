const int NUM_BUCKETS = 32;
const int ITER_PER_BUCKET = 1024;
const float HIST_SCALE = 8.0;

const float NUM_BUCKETS_F = float(NUM_BUCKETS);
const float ITER_PER_BUCKET_F = float(ITER_PER_BUCKET);


//note: uniformly distributed, normalized rand, [0;1[
float nrand( vec2 n )
{
	return fract(sin(dot(n.xy, vec2(12.9898, 78.233)))* 43758.5453);
}
//note: remaps v to [0;1] in interval [a;b]
float remap( float a, float b, float v )
{
	return clamp( (v-a) / (b-a), 0.0, 1.0 );
}
//note: quantizes in l levels
float trunc( float a, float l )
{
	return floor(a*l)/l;
}

float n1rand( vec2 n )
{
	float t = fract( iGlobalTime );
	float nrnd0 = nrand( n + 0.07*t );
	return nrnd0;
}
float n2rand( vec2 n )
{
	float t = fract( iGlobalTime );
	float nrnd0 = nrand( n + 0.07*t );
	float nrnd1 = nrand( n + 0.11*t );
	return (nrnd0+nrnd1) / 2.0;
}
float n3rand( vec2 n )
{
	float t = fract( iGlobalTime );
	float nrnd0 = nrand( n + 0.07*t );
	float nrnd1 = nrand( n + 0.11*t );
	float nrnd2 = nrand( n + 0.13*t );
	return (nrnd0+nrnd1+nrnd2) / 3.0;
}
float n4rand( vec2 n )
{
	float t = fract( iGlobalTime );
	float nrnd0 = nrand( n + 0.07*t );
	float nrnd1 = nrand( n + 0.11*t );	
	float nrnd2 = nrand( n + 0.13*t );
	float nrnd3 = nrand( n + 0.17*t );
	return (nrnd0+nrnd1+nrnd2+nrnd3) / 4.0;
}
float n5rand( vec2 n )
{
	float t = fract( iGlobalTime );
	float nrnd0 = nrand( n + 0.07*t );
	float nrnd1 = nrand( n + 0.11*t );	
	float nrnd2 = nrand( n + 0.13*t );
	float nrnd3 = nrand( n + 0.17*t );
	return 1.0-pow((nrnd0+nrnd1+nrnd2+nrnd3) / 4.0, 2.0);
}
/*
//alternate Gaussian,
//thanks to @self_shadow
float n4rand( vec2 n )
{
	float nrnd0 = nrand( n + 0.07*fract( iGlobalTime ) );
	float nrnd1 = nrand( n + 0.11*fract( iGlobalTime + 0.573953 ) );	
	return 0.23*sqrt(-log(nrnd0+0.00001))*cos(2.0*3.141592*nrnd1)+0.5;
}
*/
/*
//Mouse Y give you a curve distribution of ^1 to ^8
//thanks to Trisomie21
float n4rand( vec2 n )
{
	float t = fract( iGlobalTime );
	float nrnd0 = nrand( n + 0.07*t );
	
	float p = 1. / (1. + iMouse.y * 8. / iResolution.y);
	nrnd0 -= .5;
	nrnd0 *= 2.;
	if(nrnd0<0.)
		nrnd0 = pow(1.+nrnd0, p)*.5;
	else
		nrnd0 = 1.-pow(nrnd0, p)*.5;
	return nrnd0; 
}
*/

float histogram( int iter, vec2 uv, vec2 interval, float height, float scale )
{
	float t = remap( interval.x, interval.y, uv.x );
	vec2 bucket = vec2( trunc(t,NUM_BUCKETS_F), trunc(t,NUM_BUCKETS_F)+1.0/NUM_BUCKETS_F);
	float bucketval = 0.0;
	for ( int i=0;i<ITER_PER_BUCKET;++i)
	{
		float seed = float(i)/ITER_PER_BUCKET_F;
		
		float r;
		if ( iter < 2 )
			r = n1rand( vec2(uv.x,0.5) + seed );
		else if ( iter<3 )
			r = n2rand( vec2(uv.x,0.5) + seed );
		else if ( iter<4 )
			r = n3rand( vec2(uv.x,0.5) + seed );
		else if ( iter<5 )
			r = n4rand( vec2(uv.x,0.5) + seed );
		else
			r = n5rand( vec2(uv.x, 0.5) + seed );
		
		bucketval += step(bucket.x,r) * step(r,bucket.y);
	}
	bucketval /= ITER_PER_BUCKET_F;
	bucketval *= scale;
	return step( bucketval, uv.y / height );
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
	vec2 uv = fragCoord.xy / iResolution.xy;
	
	float o;
	if ( uv.x < 1.0/5.0 )
	{
		o = n1rand( uv );
		if ( uv.y < 1.0 / 4.0 )
			o = 0.85 - 0.7 * histogram( 1, uv, vec2(0.0/5.0,1.0/5.0), 1.0/4.0, HIST_SCALE );
	}
	else if ( uv.x < 2.0 / 5.0 )
	{
		o = n2rand( uv );
		if ( uv.y < 1.0 / 4.0 )
			o = 0.85 - 0.7 * histogram( 2, uv, vec2(1.0/5.0,2.0/5.0), 1.0/4.0, HIST_SCALE );
	}
	else if ( uv.x < 3.0 / 5.0 )
	{
		o = n3rand( uv );
		if ( uv.y < 1.0 / 4.0 )
			o = 0.85 - 0.7 * histogram( 3, uv, vec2(2.0/5.0,3.0/5.0), 1.0/4.0, HIST_SCALE );
	}
	else if ( uv.x < 4.0 / 5.0 )
	{
		o = n4rand( uv );
		if ( uv.y < 1.0 / 4.0 )
			o = 0.85 - 0.7 * histogram( 4, uv, vec2(3.0/5.0,4.0/5.0), 1.0/4.0, HIST_SCALE );
	}
	else
	{
		o = n5rand( uv );
		if ( uv.y < 1.0 / 4.0 )
			o = 0.85 - 0.7 * histogram( 5, uv, vec2(4.0/5.0,5.0/5.0), 1.0/4.0, HIST_SCALE );
	}
		
	
	//display lines
	if ( abs(uv.x - 1.0/5.0) < 0.002 ) o = 0.0;
	if ( abs(uv.x - 2.0/5.0) < 0.002 ) o = 0.0;
	if ( abs(uv.x - 3.0/5.0) < 0.002 ) o = 0.0;
	if ( abs(uv.x - 4.0/5.0) < 0.002 ) o = 0.0;
	if ( abs(uv.y - 1.0/4.0) < 0.002 ) o = 0.0;

	
	fragColor = vec4( vec3(o), 1.0 );
}