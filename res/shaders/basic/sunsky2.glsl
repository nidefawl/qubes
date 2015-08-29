
float turbidity = 1.0;
float rayleighCoefficient = 1.5;

const float mieCoefficient = 0.50;
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
const vec3 up = vec3(0.0, 1.0, 0.0);

const float sunIntensity = 575.0;
const float sunAngularDiameterCos = 0.9998993194915; // 66 arc seconds -> degrees, and the cosine of that

// earth shadow hack
const float cutoffAngle = pi/1.95;
const float steepness = 1.5;

//Sky funtions

vec3 TotalRayleigh(vec3 primaryWavelengths)
{
	vec3 rayleigh = (8.0 * pow(pi, 3.0) * pow(pow(n, 2.0) - 1.0, 2.0)) / (3.0 * N * pow(primaryWavelengths, vec3(4.0)));   // The rayleigh scattering coefficient
 
    return rayleigh; 

    //  8PI^3 * (n^2 - 1)^2 * (6 + 3pn)     8PI^3 * (n^2 - 1)^2
    // --------------------------------- = --------------------  
    //    3N * Lambda^4 * (6 - 7pn)          3N * Lambda^4         
}

float RayleighPhase(float cosViewSunAngle)
{	 
	return (3.0 / (1.0*pi)) * (1.0 + pow(cosViewSunAngle, 2.0));
}

vec3 totalMie(vec3 primaryWavelengths, vec3 K, float T)
{
	float c = (0.2 * T ) * 10E-18;
	return 0.434 * c * pi * pow((2.0 * pi) / primaryWavelengths, vec3(v - 2.0)) * K;
}

float SchlickPhase(float cosViewSunAngle, float g)
{
	float k = (1.55 * g) - (5.55 * (g * g * g));
	return (1.0 / (4.0 * pi)) * ((1.0 - (k * k)) / ( pow( 1.0 + k * cosViewSunAngle, 2.0)));
}

float SunIntensity(float zenithAngleCos)
{
	return sunIntensity * max(0.0, 1.0 - exp(-((cutoffAngle - acos(zenithAngleCos))/steepness)));
}


//Sun Ray funtion

float rand(int seed, float ray) 
{
	return mod(sin(float(seed)*363.5346+ray*674.2454)*6743.4365, 1.0);
}

vec3 SunRay(in SurfaceProperties pos)
{	
	float pi = 3.14159265359;
	vec2 position = vec2(0);
	position.y *= 1.0;
	position.y += 0.33;
	float ang = atan(position.x, position.y);
	float dist = length(position);
	vec3 col = vec3(1.7, 1.5, 1.0) * (pow(dist, -1.0) * 0.006);
	for (float ray = 0.5; ray < 10.0; ray += 0.097) 
	{
		float rayang = rand(5, ray)*6.2+(1*0.02)*20.0*(rand(2546, ray)-rand(5785, ray))-(rand(3545, ray)-rand(5467, ray));
		rayang = mod(rayang, pi*2.0);
		if (rayang < ang - pi) {rayang += pi*2.0;}
		if (rayang > ang + pi) {rayang -= pi*2.0;}
		float brite = 0.3 - abs(ang - rayang);
		brite -= dist * 0.5;
		
		if (brite > 0.0) 
		{
			col += vec3(0.1+1.7*rand(8644, ray), 0.55+1.3*rand(4567, ray), 0.7+0.5*rand(7354, ray)) * brite * 0.025;
		}
	}
	
	return col;
}


vec3 sunsky(vec3 viewDir, vec3 sunDirection, in SurfaceProperties prop)
{
	
	vec3 sunRay = SunRay(prop);
    // Cos Angles
    float cosViewSunAngle = dot(viewDir, sunDirection);
    float cosSunUpAngle = dot(sunDirection, up);
    float cosUpViewAngle = dot(up, viewDir);
    
    float sunE = SunIntensity(cosSunUpAngle);  // Get sun intensity based on how high in the sky it is

	// extinction (absorbtion + out scattering)
	// rayleigh coefficients
//	vec3 rayleighAtX = TotalRayleigh(primaryWavelengths) * rayleighCoefficient;
    vec3 rayleighAtX = vec3(5.176821E-6, 1.2785348E-5, 2.8530756E-5) * rayleighCoefficient;
    
	// mie coefficients
	vec3 mieAtX = totalMie(primaryWavelengths, K, turbidity) * mieCoefficient;  
    
	// optical length
	// cutoff angle at 90 to avoid singularity in next formula.
	float zenithAngle = max(0.0, cosUpViewAngle);
    
	float rayleighOpticalLength = rayleighZenithLength / zenithAngle;
	float mieOpticalLength = mieZenithLength / zenithAngle;


	// combined extinction factor	
	vec3 Fex = exp(-(rayleighAtX * rayleighOpticalLength + mieAtX * mieOpticalLength));

	// in scattering
	vec3 rayleighXtoEye = rayleighAtX * RayleighPhase(cosViewSunAngle);
	vec3 mieXtoEye = mieAtX *  SchlickPhase(cosViewSunAngle, mieDirectionalG);
     
    vec3 totalLightAtX = rayleighAtX + mieAtX;
    vec3 lightFromXtoEye = rayleighXtoEye + mieXtoEye; 
    
    vec3 somethingElse = sunE * (lightFromXtoEye / totalLightAtX);
    
    vec3 sky = somethingElse * (1.0 - Fex);
    sky *= mix(vec3(1.0),pow(somethingElse * Fex,vec3(0.5)),clamp(pow(1.0-dot(up, sunDirection),5.0),0.0,1.0));

	//vec4 cloud = CalculateClouds(ro, rd, sky, cosViewSunAngle);
    
	// composition + solar disc

    float sundisk = 1.0;//smoothstep(sunAngularDiameterCos,sunAngularDiameterCos+0.00002,cosViewSunAngle);
    vec3 sun = (sunE * 0.1 * Fex)*sundisk;
    vec3 final = ToneMap(sky+sun) * 1.0;
	//final = mix( final, (cloud.xyz), cloud.w );

	
    return  sunRay*10000.0f;
}