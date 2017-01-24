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

#define MAX_COLOR_RANGE 2.0//TONEMAP   
vec3 Uncharted2Tonemap2(vec3 x) {
float A2 = .8;    //brightness multiplier
float B2 = 0.37;   //black level (lower means darker and more constrasted, higher make the image whiter and less constrasted)
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