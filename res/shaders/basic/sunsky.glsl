// uniform float turbidity; slider[1,2,16]
const float turbidity = 1.4f;


// uniform vec2 SunPos; slider[(0,0),(0,0.2),(1,1)]
// uniform float time;

const float mieCoefficient = 0.20;
const float mieDirectionalG = 0.70;


// constants for atmospheric scattering
const float e  = 2.718281828459045235360287471352662497757247;
const float pi = 3.141592653589793238462643383279502884197169;

const float n = 1.0003; // refractive index of air
const float N = 2.545E25; // number of molecules per unit volume for air at
						// 288.15K and 1013mb (sea level -45 celsius)

// wavelength of used primaries, according to preetham
const vec3 primaryWavelengths = vec3(680E-9, 550E-9, 450E-9);

// mie stuff
// K coefficient for the primaries
const vec3 K = vec3(0.686, 0.78, 0.666);
const float v = 4.0;

// optical length at zenith for molecules
const float rayleighZenithLength = 8.4E3;
const float mieZenithLength = 1.25E3;
const vec3 up = vec3(0.0, 0.3, 0.0);

const float sunIntensity = 185.0;

// earth shadow hack
const float cutoffAngle = pi/1.35;
const float steepness = 1.47;

float RayleighPhase(float cosViewSunAngle)
{
	return (3.0 / (16.0*pi)) * (1 + pow(max(0,cosViewSunAngle), 2.0));
}

vec3 totalMie(vec3 primaryWavelengths, vec3 K, float T)
{
	float c = (0.2 * T ) * 10E-18;
	return 0.434 * c * pi * pow((2.0 * pi) / primaryWavelengths, vec3(v - 2.0)) * K;
}

float hgPhase(float cosViewSunAngle, float g)
{
	return (1.0 / (4.0*pi)) * ((1.0 - pow(g, 2.0)) / pow(1.0 - 2.0*g*cosViewSunAngle + pow(g, 2.0), 1.5));
}

float SunIntensity(float zenithAngleCos)
{
	return sunIntensity * max(0.0, 1.0 - exp(-((cutoffAngle - acos(zenithAngleCos))/steepness)));
}

vec3 fromSpherical(vec2 p) {
	return vec3(
		cos(p.x)*sin(p.y),
		sin(p.x)*sin(p.y),
		cos(p.y));
}

vec3 sunsky(vec3 viewDir, vec3 sunDirection)
{
	viewDir = normalize(viewDir);
	// Cos Angles
	float cosViewSunAngle = dot(viewDir, sunDirection);
	float cosSunUpAngle = dot(sunDirection, up);
	float cosUpViewAngle = dot(up, viewDir);

	float sunE = SunIntensity(cosSunUpAngle);  // Get sun intensity based on how high in the sky it is
	// extinction (asorbtion + out scattering)
	// rayleigh coeficients
	vec3 rayleighAtX = vec3(5.176821E-6, 1.2785348E-5, 2.8530756E-5) * 00344.0;
	
	// mie coefficients
	vec3 mieAtX = totalMie(primaryWavelengths, K, turbidity) * mieCoefficient;
	
	// optical length
	// cutoff angle at 90 to avoid singularity in next formula.
	float zenithAngle = max(0.0, cosUpViewAngle);
	zenithAngle = mix(zenithAngle, pi, 0.05);
	
	float rayleighOpticalLength = rayleighZenithLength / zenithAngle;
	float mieOpticalLength = mieZenithLength / zenithAngle;
	
	
	// combined extinction factor
	vec3 Fex = exp(-(rayleighAtX * rayleighOpticalLength + mieAtX * mieOpticalLength));
	
	// in scattering
	vec3 rayleighXtoEye = rayleighAtX * RayleighPhase(cosViewSunAngle);
	vec3 mieXtoEye = mieAtX *  hgPhase(cosViewSunAngle, mieDirectionalG);
	
	vec3 totalLightAtX = rayleighAtX + mieAtX;
	vec3 lightFromXtoEye = rayleighXtoEye + mieXtoEye;
	
	vec3 somethingElse = sunE * (lightFromXtoEye / totalLightAtX);
	
	vec3 sky = somethingElse * (1.0 - Fex);
	sky *= mix(vec3(1.0),pow(somethingElse * Fex,vec3(0.5)),clamp(pow(1.0-dot(up, sunDirection),5.0),0.0,1.0));
	// composition + solar disc
	
	// float sundisk = smoothstep(sunAngularDiameterCos,sunAngularDiameterCos+0.00002,cosViewSunAngle);
	// vec3 sun = (sunE * 19000.0 * Fex)*sundisk;
	
	return (sky)*0.15;
}