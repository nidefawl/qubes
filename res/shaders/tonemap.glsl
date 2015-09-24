
const float A = 0.15; // Shoulder Strength
const float B = 0.50; // Linear Strength
const float C = 0.15; // Linear Angle
const float D = 0.20; // Toe Strength
const float E = 0.02; // Toe Numerator
const float F = 0.30; // Toe Denominator

const float fMiddleGray = 0.2f;
const float fWhitePoint = 1.7f;

#define RGB_TO_LUMINANCE vec3(0.212671, 0.715160, 0.072169)
#define AUTO_EXPOSURE 0
 
vec3 Uncharted2Tonemap(vec3 x)
{
    // http://www.gdcvault.com/play/1012459/Uncharted_2__HDR_Lighting
    // http://filmicgames.com/archives/75 - the coefficients are from here
    return ((x*(A*x+C*B)+D*E)/(x*(A*x+B)+D*F))-E/F; // E/F = Toe Angle
}


float GetAverageSceneLuminance()
{
    float fAveLogLum =  0.08;
    fAveLogLum = max(0.05, fAveLogLum); // Average luminance is an approximation to the key of the scene
    return fAveLogLum;
}
vec3 ToneMap(in vec3 f3Color)
{
    float fAveLogLum = GetAverageSceneLuminance();
    
    //const float middleGray = 1.03 - 2 / (2 + log10(fAveLogLum+1));
    const float middleGray = fMiddleGray;
    // Compute scale factor such that average luminance maps to middle gray
    float fLumScale = middleGray / fAveLogLum;
    
    f3Color = max(f3Color, 0);
    float fInitialPixelLum = max(dot(RGB_TO_LUMINANCE, f3Color), 1e-10);
    float fScaledPixelLum = fInitialPixelLum * fLumScale;
    vec3 f3ScaledColor = f3Color * fLumScale;

    float whitePoint = fWhitePoint;
    // http://filmicgames.com/archives/75
    float ExposureBias = 2.0f;
    vec3 curr = Uncharted2Tonemap(ExposureBias*f3ScaledColor);
    vec3 whiteScale = 1.0f/Uncharted2Tonemap(vec3(whitePoint));
    return curr*whiteScale;
}
// vec3 ToneMap(vec3 color) {
//     vec3 toneMappedColor;
    
//     toneMappedColor = color;
//     toneMappedColor = Uncharted2Tonemap(toneMappedColor);
    
//     // float sunfade = 1.0-clamp(1.0-exp(-(sunPos.z/500.0)),0.0,1.0);
//     float sunfade = 1.0;
//     toneMappedColor = pow(toneMappedColor,vec3(1.0f/2.2f));
    
//     return toneMappedColor;
// }