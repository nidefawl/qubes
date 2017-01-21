// Gardner textured ellipsoids - https://www.cs.drexel.edu/~david/Classes/Papers/p297-gardner.pdf + bib

vec3 L = normalize(vec3(0.,-1.,0.));  // light source
float AMBIENT= .6;					  // ambient luminosity

#define ANIM true
float t = iGlobalTime;
#define PI 3.1415927
vec4 FragColor;
vec4 ambColor = vec4(1);

// --- noise functions from https://www.shadertoy.com/view/XslGRr
// Created by inigo quilez - iq/2013
// License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.

mat3 m = mat3( 0.00,  0.80,  0.60,
              -0.80,  0.36, -0.48,
              -0.60, -0.48,  0.64 );

float hash( float n )    // in [0,1]
{
    return fract(sin(n)*43758.5453);
}

float noise( in vec3 x ) // in [0,1]
{
    vec3 p = floor(x);
    vec3 f = fract(x);

    f = f*f*(3.0-2.0*f);

    float n = p.x + p.y*57.0 + 113.0*p.z;

    float res = mix(mix(mix( hash(n+  0.0), hash(n+  1.0),f.x),
                        mix( hash(n+ 57.0), hash(n+ 58.0),f.x),f.y),
                    mix(mix( hash(n+113.0), hash(n+114.0),f.x),
                        mix( hash(n+170.0), hash(n+171.0),f.x),f.y),f.z);
    return res;
}

float fbm( vec3 p )    // in [0,1]
{
	if (ANIM) p += iGlobalTime*0.5;
	if (ANIM) p.y += iGlobalTime;
    float f;
    f  = 0.5000*noise( p ); p = m*p*2.02;
    f += 0.2500*noise( p ); p = m*p*2.03;
    f += 0.1250*noise( p ); p = m*p*2.01;
    f += 0.0625*noise( p );
    return f;
}
// --- End of Created by inigo quilez

float snoise( in vec3 x ) // in [-1,1]
{ return 2.*noise(x)-1.; }

float sfbm( vec3 p )      // in [-1,1]
{
	if (ANIM) p += iGlobalTime*0.5;
	if (ANIM) p.y += iGlobalTime;
    float f;
    f  = 0.5000*snoise( p ); p = m*p*2.02;
    f += 0.2500*snoise( p ); p = m*p*2.03;
    f += 0.1250*snoise( p ); p = m*p*2.01;
    f += 0.0625*snoise( p );
    return f;
}



// --- ray -  ellipsoid intersection
// if true, return P,N and thickness l

bool intersect_ellipsoid(vec3 R, vec3 EP, vec3 O, vec3 D, out vec3 P, out vec3 N, out float l) {
	O -= EP;
	vec3 OR = O/R, DR = D/R; // to space where ellipsoid is a sphere 
		// P=O+tD & |P|=1 -> solve t in O^2 +2(O.D)t + D^2.t^2 = 1
	float OD = dot(OR,DR), OO=dot(OR,OR), DD=dot(DR,DR);
	float d = OD*OD - (OO-1.)*DD;
	
	if (!((d >=0.)&&(OD<0.)&&(OO>1.))) return false;
	// ray intersects the ellipsoid (and not in our back)
	// note that t>0 <=> -OD>0 &  OD^2 > OD^ -(OO-1.)*DD -> |O|>1
		
	float t = (-OD-sqrt(d))/DD;
	// return intersection point, normal and thickness
	P = O+t*D;
	N=normalize(P/(R*R));
	l = 2.*sqrt(d)/DD;

	return true;
}

// --- Gardner textured ellipsoids (sort of)

// 's' index corresponds to Garner faked silhouette
// 'i' index corresponds to interior term faked by mid-surface

float ks,ps, ki,pi;  // smoothness/thichness parameters

float l;
void draw_obj(vec3 R, vec3 EP, vec3 S, vec3 O, vec3 D, int mode) {
	
	vec3 P,N; 
	if (! intersect_ellipsoid(R, EP, O,D, P,N,l)) return;
	
	vec3 Pm = P+.5*l*D,                		// .5: deepest point inside cloud. 
		 Nm = normalize(Pm/(R*R)),     		// it's normal
	     Nn = normalize(P/R);
	float nl = clamp( dot(N,L),0.,1.), 		// ratio of light-facing (for lighting)
		  nd = clamp(-dot(Nn,D),0.,1.); 	// ratio of camera-facing (for silhouette)


	float ns = fbm(P+S), ni = fbm(Pm+S+10.);
	float A, l0 = 3.;
	// l += l*(l/l0-1.)/(1.+l*l/(l0*l0));     // optical depth modified at silhouette
	l = clamp(l-6.*ni,0.,1e10);
	float As = pow(ks*nd, ps), 			 	 // silhouette
		  Ai = 1.-pow(.7,pi*l);              // interior


	As =clamp(As-ns,0.,1.)*2.; // As = 2.*pow(As ,.6);
	if (mode>=2) 
		A = 1.- (1.-As)*(1.-Ai);  			// mul Ti and Ts
	else
		A = (mode==0) ? Ai : As; 
	A = clamp(A,0.,1.); 
	vec4 nlCol = vec4(nl, nl*0.7, 0.4*nl, 1.0);
	if (mode != 3){
		nlCol = vec4(1);
	}
	nl = .8*( nl + ((mode==0) ? fbm(Pm-10.) : fbm(P+10.) ));

	float brightness = dot(FragColor, vec4(1))/2.0;
	vec4 col = mix(vec4(nl),nlCol,AMBIENT*0.6-brightness*0.4);
	FragColor = mix(FragColor,col,A*0.8);
}

// === main =============================================

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
	vec2 uv = 2.*(fragCoord.xy / iResolution.y-vec2(.85,.5));
	vec2 mouse = 2.*(iMouse.xy / iResolution.xy - vec2(.85,.5));
	float z = .2;
	ks = 1.+mouse.x;
	ps = mouse.y*8.;

	ks = 1.15;
	ps = 1.2;
	ki = .9;
	pi = 0.2;
	t = 10;
	if (iMouse.z>0.) {
		t = -PI/2.*mouse.x;
		z = -PI/2.*mouse.y;
	}
	vec3 O = CAMERA_POS;//vec3(-15.*cos(t)*cos(z),15.*sin(t)*cos(z),15.*sin(z));	// camera

	//float compas = t-.2*uv.x;
	//vec2 dir = vec2(cos(compas),sin(compas));

	FragColor = vec4(0);//clamp(vec4(.6,.7+.3*pass_rayDir.xz,1.)*(uv.y+1.6)/1.8,0.,1.); 		// sky

	AMBIENT = 0.05;
	vec3 EP = vec3(0, -1, 0);
	vec3 R1 = vec3(4,4,4);              // ellipsoid radius
	vec3 RS1 = vec3(1);              // samplepoint
	int iter = 2;
		draw_obj(R1, EP, RS1, pass_rayOrigin,pass_rayDir, 2);	
	float iterF = float(iter);
	for (int i = 0; i < 6; i++) {
		float pw = pow(1-i/iterF, 2);
	 EP = vec3(0, 3+(iter-(pw)*iter)*2, 0);
	 R1 = vec3(3,5.4,3)*(pow(1-i/iterF,4)*0.3+0.5f);              // ellipsoid radius
	 RS1 = vec3(i*23)+vec3(-3, 23, -12);

	ks = 1.15;
	ps = 1.2;
	ki = .9;
	pi = 2.7;
		draw_obj(R1, EP, RS1, pass_rayOrigin,pass_rayDir, 2);	
		// EP.y+=1;
		// pi = 0.2;
		pi*=1.2;
		R1*=1.02;
		RS1+=vec3(4,-4,8);
		draw_obj(R1, EP, RS1, pass_rayOrigin,pass_rayDir, 3);	
		pi*=0.9;
		R1*=1.04;
		RS1+=vec3(4,-5,8);
		EP.y-=0.1;
		// draw_obj(R1, EP, RS1, pass_rayOrigin,pass_rayDir, 2);	
		AMBIENT+=0.05/iterF;
	}
	AMBIENT = 2.;
	 EP = vec3(0, 10, 0);
	 R1 = vec3(5,2,5);              // ellipsoid radius
	 RS1 = vec3(1);              // ellipsoid radius
	ks = 0.7;
	ps = 1.2;
	ki = 2.9;
	pi = .4;
	draw_obj(R1, EP, RS1, pass_rayOrigin,pass_rayDir, 3);
	AMBIENT = 0.3;
	// EP.y+=1;
	// pi = 0.2;
	pi*=0.7;
	R1*=1.2;
	RS1+=vec3(4,-4,8);
	draw_obj(R1, EP, RS1, pass_rayOrigin,pass_rayDir, 3);	
	pi*=0.98;
	R1*=1.04;
	RS1+=vec3(4,-5,8);
	EP.y-=0.3;
	// draw_obj(R1, EP, RS1, pass_rayOrigin,pass_rayDir, 2);	
		// draw_obj(vec3(1, 1, 2)*2.4, vec3(0, 1, 1), pass_rayOrigin,pass_rayDir, 2);	
		// draw_obj(R2, vec3(0, 0, 2), pass_rayOrigin,pass_rayDir, 2);	
	/*else {
		draw_obj(O,M, 1.5*(uv+dx), 0);	
		draw_obj(O,M, 1.5*(uv-dx), 1);	
	}*/
	FragColor = pow(FragColor, vec4(0.7))*1.2;
    vec4 fragOut = clamp(FragColor, vec4(0), vec4(1));
   fragColor = FragColor; 
}
