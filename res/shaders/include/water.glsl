



//ADJUSTABLE VARIABLES//
	
	// #define Watercolor_Vanila								//Pure texture based water. Only enable one.
	// #define Watercolor_Clear								//Clear-ish water. Only enable one.
	// #define Watercolor_Tropical							//Weak green-ish water. Only enable one.
	// #define Watercolor_Legacy								//Strong blue water. Only enable one.
	// #define Watercolor_Classic							//Weak light blue water. Only enable one.
	#define Watercolor_Original								//Strong dark blue water. Only enable one.
	
	#define WaterRipple									//Creates ripple effect near the player. Doesn't affect other players.
	
	//#define RPSupport
	
	//#define WorldTimeAnimation
	
//ADJUSTABLE VARIABLES//

const int MAX_OCCLUSION_POINTS = 20;
const float MAX_OCCLUSION_DISTANCE = 100.0;
const float bump_distance = 64.0;				//Bump render distance: tiny = 32, short = 64, normal = 128, far = 256
const float pom_distance = 32.0;				//POM render distance: tiny = 32, short = 64, normal = 128, far = 256
const float fademult = 0.1;
const float PI = 3.1415927;




#ifdef WorldTimeAnimation
float frametime = worldTime/20.0;
#else
float frametime = FRAME_TIME*0.05;
#endif


float waterH2(vec3 posxz, float spd, float doRipple) {
	float isEyeInWater = 0;
float speed = 1*spd;
float size = 4;

//Big Noise
float noise = 0;

float noisesize = 32*size;
float noiseweight = 0;
float noiseneg = 1;

float noisea = 1;
for (int i = 0; i < 2; i++) {
noisea += texture2D(noisetex,vec2(posxz.x,posxz.z)/noisesize*0.1+vec2(frametime/1000*speed,0)).r*i*noiseneg;
noiseweight += i;
noiseneg *= -1;
noisesize /= 2;
}
noisea /= noiseweight;

noisesize = 16*size;
noiseweight = 0;

float noiseb = 1;
for (int i = 0; i < 2; i++) {
noiseb += texture2D(noisetex,vec2(-posxz.x,posxz.z)/noisesize*0.1+vec2(0,frametime/1000*speed)).r*i*noiseneg;
noiseweight += i;
noiseneg *= -1;
noisesize /= 2;
}
noiseb /= noiseweight;

noisesize = 64*size;
noiseweight = 0;

float noisec = 1;
for (int i = 0; i < 2; i++) {
noisec += texture2D(noisetex,vec2(posxz.x,-posxz.z)/noisesize*0.1+vec2(-frametime/1000*speed,0)).r*i*noiseneg;
noiseweight += i;
noiseneg *= -1;
noisesize /= 2;
}
noisec /= noiseweight;

noisesize = 48*size;
noiseweight = 0;

float noised = 1;
for (int i = 0; i < 2; i++) {
noised += texture2D(noisetex,vec2(-posxz.x,-posxz.z)/noisesize*0.1+vec2(0,-frametime/1000*speed)).r*i*noiseneg;
noiseweight += i;
noiseneg *= -1;
noisesize /= 2;
}
noised /= noiseweight;

noise = (noisea*noiseb + noiseb*noisec + noisec*noised + noised*noisea) * (1- noisea*noiseb*noisec*noised)*0.8;

//Wave
float wave = 0;
	wave = sin(posxz.x+frametime*0.5*speed)*cos(posxz.z+frametime*0.5*speed);
	wave += sin(posxz.x/1.5-frametime*speed)*cos(posxz.z/1.5+frametime*speed);
	wave += sin(posxz.x/2+frametime*1.5*speed)*cos(posxz.z/2-frametime*1.5*speed);
	wave += sin(posxz.x/2.5-frametime*2*speed)*cos(posxz.z/2.5-frametime*2*speed);
	wave /= 4;

//Ripple
float ripple = 0;
#ifdef WaterRipple
vec3 camtrail = (CAMERA_POS-PREV_CAMERA_POS)*2;
float rpos = length((posxz-CAMERA_POS)*vec3(1.0,2.5,1.0)-vec3(0.0,1.5,0.0));
float rpos0 = rpos;
for(int i = 0; i < 8; i++){
	float temp = length((posxz-CAMERA_POS+camtrail*i/4)*vec3(1.0-i*0.025,2.5,1.0-i*0.025)-vec3(0.0,1.5,0.0))+i/10;
	rpos0 = min(rpos0,temp);
	}

ripple = sin(rpos0*10-FRAME_TIME*5*speed)/pow(length(rpos),3.5);
ripple = clamp(ripple*min(length(rpos),1.0),-1.0,1.0)*(1.0-isEyeInWater)*doRipple;
#endif

float final = (noise + wave)*max(1 - abs(ripple),0.0) + ripple*0.5;

return final;
}


float waterH(vec3 posxz) {
float speed = 2;
float size = 4;

//Big Noise
float noise = 0;

float noisesize = 32*size;
float noiseweight = 0;
float noiseneg = 1;

float noisea = 1;
for (int i = 0; i < 2; i++) {
noisea += texture(noisetex,vec2(posxz.x,posxz.z)/noisesize*0.1+vec2(frametime/1000*speed,0)).r*i*noiseneg;
noiseweight += i;
noiseneg *= -1;
noisesize /= 2;
}
noisea /= noiseweight;

noisesize = 16*size;
noiseweight = 0;

float noiseb = 1;
for (int i = 0; i < 2; i++) {
noiseb += texture(noisetex,vec2(-posxz.x,posxz.z)/noisesize*0.1+vec2(0,frametime/1000*speed)).r*i*noiseneg;
noiseweight += i;
noiseneg *= -1;
noisesize /= 2;
}
noiseb /= noiseweight;

noisesize = 64*size;
noiseweight = 0;

float noisec = 1;
for (int i = 0; i < 2; i++) {
noisec += texture(noisetex,vec2(posxz.x,-posxz.z)/noisesize*0.1+vec2(-frametime/1000*speed,0)).r*i*noiseneg;
noiseweight += i;
noiseneg *= -1;
noisesize /= 2;
}
noisec /= noiseweight;

noisesize = 48*size;
noiseweight = 0;

float noised = 1;
for (int i = 0; i < 2; i++) {
noised += texture(noisetex,vec2(-posxz.x,-posxz.z)/noisesize*0.1+vec2(0,-frametime/1000*speed)).r*i*noiseneg;
noiseweight += i;
noiseneg *= -1;
noisesize /= 2;
}
noised /= noiseweight;

noise = (noisea*noiseb + noiseb*noisec + noisec*noised + noised*noisea) * (1- noisea*noiseb*noisec*noised)*0.8;

//Wave
float wave = 0;
	wave = sin(posxz.x+frametime*0.5)*cos(posxz.z+frametime*0.5);
	wave += sin(posxz.x/1.5-frametime)*cos(posxz.z/1.5+frametime);
	wave += sin(posxz.x/2+frametime*1.5)*cos(posxz.z/2-frametime*1.5);
	wave += sin(posxz.x/2.5-frametime*2)*cos(posxz.z/2.5-frametime*2);
	wave /= 4;

//Ripple
float ripple = 0;
#ifdef WaterRipple
vec3 camtrail = (CAMERA_POS-PREV_CAMERA_POS)*2;
float rpos = length((posxz-CAMERA_POS)*vec3(1.0,2.5,1.0)-vec3(0.0,1.5,0.0));
float rpos0 = rpos;
for(int i = 0; i < 8; i++){
	float temp = length((posxz-CAMERA_POS+camtrail*i/4)*vec3(1.0-i*0.025,2.5,1.0-i*0.025)-vec3(0.0,1.5,0.0))+i/10;
	rpos0 = min(rpos0,temp);
	}

ripple = sin(rpos0*10-FRAME_TIME*5)/pow(length(rpos),3.5);
ripple = clamp(ripple*min(length(rpos),1.0),-1.0,1.0);
#endif

float final = (noise + wave)*max(1 - abs(ripple)*2,0.0) + ripple;

return final;
}