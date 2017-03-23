#version 450
#line 2 0
#define VULKAN_GLSL
#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable

#define NOISE_TEX_SIZE 64

#line 1 1
#define VULKAN_GLSL

layout(set = 0, binding = 0, std140) uniform uboMatrix3D
{
    mat4 mvp;
    mat4 mv;
    mat4 view;
    mat4 vp;
    mat4 p;
    mat4 normal;
    mat4 mv_inv;
    mat4 proj_inv;
    mat4 mvp_inv;
} in_matrix_3D;

layout(set = 0, binding = 1, std140) uniform uboMatrix2D
{
    mat4 mvp;
    mat4 p3DOrtho;
    mat4 mv3DOrtho;
} in_matrix_2D;

layout(set = 0, binding = 2, std140) uniform uboSceneData
{
    vec4 cameraPosition;
    vec4 framePos;
    vec4 viewport;
    vec4 prevCameraPosition;
    vec4 sceneSettings;
} in_scene;

layout(set = 1, binding = 0, std140) uniform uboTransformStack
{
    vec4 pxoffset;
} in_transform_stack;



#define FRAME_TIME in_scene.framePos.w
#define CAMERA_POS in_scene.cameraPosition.xyz
#define PREV_CAMERA_POS in_scene.prevCameraPosition.xyz
#define RENDER_OFFSET in_scene.framePos.xyz
#define PX_OFFSET in_transform_stack.pxoffset
#define Z_FAR in_scene.viewport.w
#define Z_NEAR in_scene.viewport.z
#line 1 2
#define VULKAN_GLSL
const float A = 0.15;
const float B = 0.50;
const float C = 0.10;
const float D = 0.20;
const float E = 0.02;
const float F = 0.30;
const float W = 11.2;



void srgb(inout float v)
{
    v = clamp(v, 0.0, 1.0);
    float K0 = 0.03928;
    float a = 0.055;
    float phi = 12.92;
    float gamma = 2.4;
    v = v <= K0 / phi ? v * phi : (1.0 + a) * pow(v, 1.0 / gamma) - a;
}
void linear(inout float v)
{
    v = clamp(v, 0.0, 1.0);
    float K0 = 0.03928;
    float a = 0.055;
    float phi = 12.92;
    float gamma = 2.4;
    v = v <= K0 ? v / phi : pow((v + a) / (1.0 + a), gamma);
}
void linearizeInput(inout vec3 srgb)
{
    #ifndef SRGB_TEXTURES
    linear(srgb.x);
    linear(srgb.y);
    linear(srgb.z);
    #endif
}
void linearizeInput2(inout vec3 srgb)
{
    linear(srgb.x);
    linear(srgb.y);
    linear(srgb.z);
}

void srgbToLin(inout vec3 srgb) {
    linear(srgb.x);
    linear(srgb.y);
    linear(srgb.z);
}
void linToSrgb(inout vec3 linear) {
    srgb(linear.x);
    srgb(linear.y);
    srgb(linear.z);
}

vec3 Uncharted2Tonemap(vec3 x)
{
    // http://www.gdcvault.com/play/1012459/Uncharted_2__HDR_Lighting
    // http://filmicgames.com/archives/75 - the coefficients are from here
    return ((x*(A*x+C*B)+D*E)/(x*(A*x+B)+D*F))-E/F; // E/F = Toe Angle
}

#define MAX_COLOR_RANGE 48.0//TONEMAP   
vec3 Uncharted2Tonemap2(vec3 x) {
float A2 = 1.0;    //brightness multiplier
float B2 = 0.5;   //black level (lower means darker and more constrasted, higher make the image whiter and less constrasted)
float C2 = 0.1;    //constrast level 
  float D2 = 0.2;    
  float E2 = 0.02;
  float F2 = 0.3;
  float W2 = MAX_COLOR_RANGE;
  return ((x*(A2*x+C2*B2)+D2*E2)/(x*(A2*x+B2)+D2*F2))-E2/F2;
}


vec3 ToneMap( in vec3 texColor, float exposure)
{

  vec3 curr = Uncharted2Tonemap2(exposure*texColor);
  
  vec3 whiteScale = 1.0f/Uncharted2Tonemap2(vec3(MAX_COLOR_RANGE));
 vec3 color = curr*whiteScale;
   // vec3 curr = Uncharted2Tonemap(exposure*texColor);

   // vec3 whiteScale = 1.0f/Uncharted2Tonemap(vec3(W));
   // vec3 color = curr*whiteScale;

   // vec3 retColor = pow(color,vec3(1/2.2));
   linToSrgb(color);
   return color;
}
#line 1 3
#define VULKAN_GLSL

layout(set = 3, binding = 0, std140) uniform VertexDirections
{
    vec4 dir[64];
} vertexDir;

layout(set = 3, binding = 1, std140) uniform TBNMatrix
{
    mat4 mat[6];
} matrix_tbn;
#line 1 4
#define VULKAN_GLSL

#define BLOCK_ID_u32(blockinfo32) ((blockinfo32 >> 16u) & 0xFFFu)
#define BLOCK_RENDERPASS_u32(blockinfo32) ((blockinfo32 >> 28u) & 0xFu)
#define BLOCK_TEX_SLOT_u32(blockinfo32) float(blockinfo32&0xFFFu)
#define BLOCK_NORMAL_SLOT_u32(blockinfo32) float((blockinfo32&0xF000u)>>12u)

#define BLOCK_ID(blockinfo) (blockinfo.y&0xFFFu)
#define BLOCK_RENDERPASS(blockinfo) float((blockinfo.y&0xF000u)>>12u)
#define BLOCK_TEX_SLOT(blockinfo) float(blockinfo.x&0xFFFu)
#define BLOCK_NORMAL_SLOT(blockinfo) float((blockinfo.x&0xF000u)>>12u)
#define BLOCK_FACEDIR(blockinfo) (blockinfo.w&0x7u)
#define BLOCK_VERTDIR(blockinfo) ((blockinfo.w >> 3u) & 0x3Fu)
#define BLOCK_AO_IDX_0(blockinfo) (in_blockinfo.z)&AO_MASK
#define BLOCK_AO_IDX_1(blockinfo) (in_blockinfo.z>>2u)&AO_MASK
#define BLOCK_AO_IDX_2(blockinfo) (in_blockinfo.z>>4u)&AO_MASK
#define BLOCK_AO_IDX_3(blockinfo) (in_blockinfo.z>>6u)&AO_MASK
#define IS_SKY(blockid) float(blockid==0u)
#define IS_WATER(blockid) float(blockid==10u)

#define IS_LIGHT(blockid) float(blockid==2222u)

#define IS_ILLUM(renderpass) float(renderpass==4)
#define IS_BACKFACE(renderpass) float(renderpass==3)

#define ENCODE_RENDERPASS(renderpass) ((uint(renderpass)&0xFu)<<12u)
#line 12 0



layout (set = 2, binding = 0) uniform sampler2DArray blockTextures;
layout (set = 2, binding = 1) uniform sampler2D waterNoiseTexture;
#define noisetex waterNoiseTexture
#line 1 5
#define VULKAN_GLSL




//ADJUSTABLE VARIABLES//
	
	// #define Watercolor_Vanila								//Pure texture based water. Only enable one.
	// #define Watercolor_Clear								//Clear-ish water. Only enable one.
	// #define Watercolor_Tropical							//Weak green-ish water. Only enable one.
	// #define Watercolor_Vanila								//Strong blue water. Only enable one.
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
#line 19 0
const float rainStrength = 0;


layout (location = 0)  in vec4 color;
layout (location = 1)  in vec2 texcoord;
layout (location = 2)  in vec3 normal;
layout (location = 3) flat  in uvec4 blockinfo;
layout (location = 4) flat  in uint faceDir;
layout (location = 5) flat  in vec4 faceLight;
layout (location = 6) flat  in vec4 faceLightSky;
layout (location = 7)  in vec2 texPos;
layout (location = 8)  in vec4 vpos;
layout (location = 9)  in vec3 vwpos;
layout (location = 10)  in float isWater;
layout (location = 11)  in float roughness;


layout (location = 0) out vec4 out_Color;
layout (location = 1) out vec4 out_Normal;
layout (location = 2) out uvec4 out_Material;
layout (location = 3) out vec4 out_Light;

void main() {
	
	#ifdef Watercolor_Vanila
	vec3 watercolor = vec3(1.0);
	float wateropacity = 0.8;
	#endif
	
	#ifdef Watercolor_Clear
	vec3 watercolor = vec3(0.02,0.08,0.14);
	float wateropacity = 0.3;
	#endif
	
	#ifdef Watercolor_Tropical
	vec3 watercolor = vec3(0.1,0.6,0.6);
	float wateropacity = 0.41;
	#endif
	
	#ifdef Watercolor_Legacy
	vec3 watercolor = vec3(0.0,0.3,0.7);
	float wateropacity = 0.7;
	#endif
	
	#ifdef Watercolor_Classic
	vec3 watercolor = vec3(0.1,0.4,0.7);
	float wateropacity = 0.4;
	#endif
	
	#ifdef Watercolor_Original
	vec3 watercolor = vec3(0.02,0.08,0.14);
	float wateropacity = 0.8;
	#endif
	
	vec3 posxz = vwpos.xyz;

	posxz.x += sin(posxz.z+frametime)*0.25;
	posxz.z += cos(posxz.x+frametime)*0.25;
	
	float deltaPos = 0.5;
	float bumpmult = 0.02;
	float h0 = waterH(posxz);
	float h1 = waterH(posxz + vec3(deltaPos,0.0,0.0));
	float h2 = waterH(posxz + vec3(-deltaPos,0.0,0.0));
	float h3 = waterH(posxz + vec3(0.0,0.0,deltaPos));
	float h4 = waterH(posxz + vec3(0.0,0.0,-deltaPos));
	
	float xDelta = ((h1-h0)+(h0-h2))/deltaPos;
	float yDelta = ((h3-h0)+(h0-h4))/deltaPos;
	
	vec3 newnormal = normalize(vec3(xDelta,yDelta,1.0-xDelta*xDelta-yDelta*yDelta));
	newnormal = newnormal + (xDelta*yDelta) / (sin(xDelta) + cos(yDelta)+frametime);
	vec2 texCoordWater = texcoord.st;
	vec3 waterNormal = normal;
	texCoordWater.s+=h0*0.08f;
	texCoordWater.t+=h1*0.08f;
	texCoordWater.s+=h3*0.08f;
	texCoordWater.t+=h4*0.08f;
    if (isWater > 0 && faceDir != 0u) {
		mat3 tbnMat = mat3(matrix_tbn.mat[faceDir-1u]);
		vec3 bump = newnormal;
			
		
		
		bump = 	bump * vec3(bumpmult, bumpmult, bumpmult) + vec3(0.0f, 0.0f, 1.0f - bumpmult);
		
		waterNormal = normalize(tbnMat * bump.xzy);
	}
	
	

	vec4 raw = texture(blockTextures, vec3(texCoordWater, BLOCK_TEX_SLOT(blockinfo)));
	// raw.a=1;
    uint blockid = BLOCK_ID(blockinfo);
    float iswater = IS_WATER(blockid);
	float isEyeInWater = 0;
	// vec4 raw = texture(texture, texcoord.xy);
	vec4 tex = vec4(vec3(raw.b + (raw.r+raw.g)),max(wateropacity*(1-isEyeInWater),0.2));
	tex *= vec4(watercolor,1);
	
	#ifdef Watercolor_Vanila
	tex.rgb = raw.rgb*color.rgb;
	#endif
	tex.a = 1;
	
	if (iswater < 0.9) tex = raw*color;

	

	float xPos2 = texPos.x;
	float xPos = 1-texPos.x;
	float yPos2 = texPos.y;
	float yPos = 1-texPos.y;
	float lightLevelBlock =  0.0;
	lightLevelBlock += faceLight.x * xPos  * yPos;
	lightLevelBlock += faceLight.y * xPos2 * yPos;
	lightLevelBlock += faceLight.z * xPos2 * yPos2;
	lightLevelBlock += faceLight.w * xPos  * yPos2;
	float lightLevelSky =  0.0;
	lightLevelSky += faceLightSky.x * xPos  * yPos;
	lightLevelSky += faceLightSky.y * xPos2 * yPos;
	lightLevelSky += faceLightSky.z * xPos2 * yPos2;
	lightLevelSky += faceLightSky.w * xPos  * yPos2;

    out_Color = tex;
    out_Normal = vec4(waterNormal.xyz* 0.5f + 0.5f, roughness);
    out_Material = blockinfo;
    out_Light = vec4(lightLevelSky, lightLevelBlock, 1, 1);
}
