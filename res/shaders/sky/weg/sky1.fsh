#version 150 core
#pragma include "blockinfo.glsl"
#pragma include "ubo_scene.glsl"

#define Sky_Height 1.3									//Determines sky gradient position. [1.0 1.1 1.2 1.3 1.4 1.5 1.6]
	#define Fog_Height 72.0									//Set vertical position for fog. Insert average ground height for best result. [8.0 16.0 24.0 32.0 40.0 48.0 56.0 64.0 72.0 80.0 88.0 96.0 104.0 112.0 120.0 128.0]
#define Sky_OriginalColor 1

int isEyeInWater = 0;
vec3 skyColor = vec3(1.0, 0.6, 1.0);
vec2 eyeBrightnessSmooth = vec2(155.0, 155.0);
float eBS = eyeBrightnessSmooth.y/240.0;

in vec4 color;

out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;
in vec2 texcoord;

in vec3 lightVector;
in vec3 sunVec;
in vec3 moonVec;
in vec3 upVec;

in vec4 lightS;


in vec3 sunlight;
in vec3 moonlight;
in vec3 ambient_color;

in float handItemLight;
in float eyeAdapt;

in float SdotU;
in float MdotU;
in float sunVisibility;
in float moonVisibility;

uniform int worldTime;
uniform float rainStrength;

const float dimread = 0;
float time = float(worldTime);
float transition_fading = (clamp((time-12000.0)/500.0,0.0,1.0)-clamp((time-13500.0)/500.0,0.0,1.0) + clamp((time-22500.0)/500.0,0.0,1.0)-clamp((time-23500.0)/500.0,0.0,1.0));	//fading between sun/moon shadows
float night = clamp((time-13000.0)/500.0,0.0,1.0)-clamp((time-22500.0)/500.0,0.0,1.0);
float timebrightness = abs(sin(time/12000*22/7));

//Time of Day for VL
float TimeSunrise  = ((clamp(time, 23000.0, 24000.0) - 23000.0) / 1000.0) + (1.0 - (clamp(time, 0.0, 4000.0)/4000.0));
float TimeNoon     = ((clamp(time, 0.0, 4000.0)) / 4000.0) - ((clamp(time, 8000.0, 12000.0) - 8000.0) / 4000.0);
float TimeSunset   = ((clamp(time, 8000.0, 12000.0) - 8000.0) / 4000.0) - ((clamp(time, 12000.0, 12750.0) - 12000.0) / 750.0);
float TimeMidnight = ((clamp(time, 12000.0, 12750.0) - 12000.0) / 750.0) - ((clamp(time, 23000.0, 24000.0) - 23000.0) / 1000.0);

// const vec3 fogColor=vec3(0.84f, 0.84f, 0.96f);

const vec3 fogColor=vec3(0.43f, 0.54f, 0.86f);

vec3 getSkyColor(vec3 fposition) {
//sky gradient
/*----------*/
vec3 sky_color = vec3(0);
if (dimread == 0){
#ifdef Sky_OriginalColor
sky_color = vec3(0.1, 0.35, 1.0);
#endif
#ifdef Sky_AlternativeColor
sky_color = vec3(0.15, 0.25, 0.7);
#endif
#ifdef Sky_VanilaColor
sky_color = pow(fogColor,vec3(2.2))*0.8;
#endif

vec3 nsunlight = normalize(pow(sunlight,vec3(1.8)))*(1-dot(normalize(fposition),upVec)+skyColor*dot(normalize(fposition),upVec));
nsunlight *= 1-pow(timebrightness,0.2)*(0.8*(1-timebrightness))*(1-transition_fading);
vec3 sVector = normalize(fposition);

float Lz = 1.0;
float cosT = dot(sVector,upVec);
float absCosT = pow(max(cosT,0.0),Sky_Height);
float cosS = dot(sunVec,upVec);
float S = acos(cosS);
float cosY = dot(sunVec,sVector);
float absCosY = max(cosY,0.0);
float Y = acos(cosY);
float sidefog = pow(clamp(1-abs(cosT),0,0.9),3)*(1-absCosY*(0.5+0.4*pow(timebrightness,0.6)))*0.9;

float a = -1.;
float b = -0.2+0.1*max(pow(timebrightness,0.1),transition_fading);
float c = 4.0-2.0*timebrightness;
float d = -0.6;
float e = 0.1+(timebrightness+transition_fading/8)*0.8;
float rainStr = rainStrength;
#ifdef Sky_AlternativeColor
sidefog *= 0.75;
sky_color = mix(sky_color,vec3(0.15,0.25,1.5),pow(1.0-abs(cosT),2.5));
sky_color = mix(sky_color,vec3(0.26,0.34,0.6),pow(1.0-abs(cosT),3.0));
b -= 0.025;
#endif
#ifdef Sky_VanilaColor
sidefog *= 0.22;
float vsmix = max(pow(max(cosT,0.0),0.9)-sidefog,0.0);
sky_color = mix(sky_color,pow(skyColor,vec3(2.2))*0.8,vsmix*(1-rainStr));
b -= 0.05;
#endif

sky_color = mix(sky_color*(1-rainStr*0.05),vec3(0.25,0.3,0.4)*length(ambient_color)*pow(rainStr,1.5)*pow(1.0-absCosT*0.25*rainStr,4.0)*0.7,rainStr);

//sun sky color
float L = (1+a*exp(b/(absCosT+0.01)))*(1+c*exp(d*Y)+e*absCosY*absCosY) + sidefog*(1-transition_fading);
L = pow(L,1.0-rainStr)+sidefog*rainStr/2*(1-transition_fading); //modulate intensity when raining
float skymix = clamp(1-exp(-0.005*pow(L*1.1,4.)*(1-rainStr*0.5)),0,4);
float skymult = pow(1-skymix*(1-timebrightness),0.5);
vec3 skyColorSun = mix(sky_color*skymult, nsunlight,skymix)*(L*(1-cosY*cosY/2*transition_fading*(1-rainStr)))*0.5*(1.0-pow(max(cosY,0.0),2.2)*0.4*(1.0-rainStr));
skyColorSun *= sunVisibility * (1-transition_fading*0.2);

//moon sky color
float McosS = MdotU;
float MS = acos(McosS);
float McosY = dot(moonVec,sVector);
float MY = acos(McosY);

float L2 = (1+a*exp(b/(absCosT+0.01)))*(1+c*exp(d*MY)+e*McosY*McosY)+0.2;
L2 = pow(L2,1.0-rainStr*0.8)*(1.0-rainStr*0.2); //modulate intensity when raining
vec3 skyColormoon = mix(moonlight,normalize(vec3(0.25,0.3,0.4))*length(moonlight),rainStr*0.8)*L2*0.8;
skyColormoon *= moonVisibility;

sky_color = min(vec3(1),skyColormoon*2.0+skyColorSun);
sky_color *= pow(2.0-(eBS*(1-isEyeInWater)+isEyeInWater),1.5-0.5*eBS)*(1-rainStr)+rainStr;
//sky_color = vec3(Lc);
}

if (dimread == -1) sky_color = vec3(0.0005,0.00025,0.00012);
if (dimread == 1) sky_color = vec3(0.005,0,0.005);
return sky_color;
}
vec3 nvec3(vec4 pos) {
    return pos.xyz/pos.w;
}

vec4 nvec4(vec3 pos) {
    return vec4(pos.xyz, 1.0);
}
void main() {
vec3 fragpos = vec3(texcoord.st, 1.0);
	fragpos = nvec3(in_matrix_3D.proj_inv * nvec4(fragpos * 2.0 - 1.0));
	vec3 fogclr = getSkyColor(fragpos.xyz+vec3(0.0,(CAMERA_POS.y-Fog_Height)*(1-isEyeInWater),0.0));
  // skycolor.rgb = pow(skycolor.rgb, vec3(1.66f/3.0));
  out_Color = vec4(fogclr*0.2, 1.0);//*22222.2;
  out_Normal = vec4(0.5);
  uint renderData = 0u;
  renderData = ENCODE_RENDERPASS(8);
  out_Material = uvec4(0u,0u+renderData,0u,0u);
}
