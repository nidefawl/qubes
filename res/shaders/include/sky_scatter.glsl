// uniform vec2 viewport;
// uniform mat4 inv_proj;
// uniform mat3 inv_view_rot;
// uniform vec3 lightdir, Kr;
vec3 Kr = vec3(0.28867780436772762, 0.4978442963618773, 0.6616065586417131); // air
vec3 Kr1 = vec3(0.09, 0.46, 0.7); // air
vec3 Kr2 = vec3(0.3, 0.3, 0.14); // air
// uniform float rayleigh_brightness, mie_brightness, spot_brightness, scatter_strength, rayleigh_strength, mie_strength;
// uniform float rayleigh_collection_power, mie_collection_power, mie_distribution;
 float spot_brightness = 1000;
 float rayleigh_brightness = 0.8;
float scatter_strength = 0.3;
 float rayleigh_strength = 0.95;
 float rayleigh_collection_power = 0.1;
 float mie_brightness = 0.55;
 float mie_strength = 0.27;
 float mie_collection_power = 0.035;
 float mie_distribution = 0.05;

const int step_count = 4;



float atmospheric_depth(vec3 position, vec3 dir){
    float a = dot(dir, dir);
    float b = 2.0*dot(dir, position);
    float c = dot(position, position)-1.0;
    float det = b*b-4.0*a*c;
    float detSqrt = sqrt(det);
    float q = (-b - detSqrt)/2.0;
    float t1 = c/q;
    return t1;
}

float phase(float alpha, float g){
    float a = 3.0*(1.0-g*g);
    float b = 2.0*(2.0+g*g);
    float c = 1.0+alpha*alpha;
    float d = pow(1.0+g*g-2.0*g*alpha, 1.5);
    return (a/b)*(c/d);
}

float horizon_extinction(vec3 position, vec3 dir, float radius){
    float u = dot(dir, -position);
    if(u<0.0){
        return 1.0;
    }
    vec3 near = position + u*dir;

        vec3 v2 = normalize(near)*radius - position;
        float diff = acos(dot(normalize(v2), dir));
        return smoothstep(0.0, 1.0, pow(diff*2.0, 3.0));
}

vec3 absorb(float dist, vec3 color, float factor){
    return color-color*pow(Kr, vec3(factor/dist));
}

#define NO_SCATTERING1
#ifdef NO_SCATTERING
vec3 skyAtmoScat(vec3 eyedir, vec3 lightdir, float moon){
    return vec3(0);
}
#else
vec3 skyAtmoScat(vec3 eyedir, vec3 lightdir, float moon, float miestr, float dayLightIntens, float dayNoon){
    float alpha = dot(eyedir, lightdir);
    Kr = mix(Kr1, Kr2, moon);
float intensity = 0.85;
    intensity = mix(intensity, 0.2, moon);
float surface_height = 0.7+0.27*clamp(dayLightIntens, 0, 1);
    mie_brightness = mix(mie_brightness, 0.05, moon)+miestr;
    rayleigh_brightness = mix(rayleigh_brightness, 0.4, moon);
    // rayleigh_collection_power = rayleigh_collection_power+(1.0-dayLightIntens*dayNoon)*2.0;
    float rayleigh_factor = phase(alpha, -0.01)*rayleigh_brightness;
    float mie_factor = phase(alpha, mie_distribution*(1.0f-miestr*0.15f))*mie_brightness;
    float spot = smoothstep(0.0, 18.0, phase(alpha, 0.9998))*spot_brightness;

    vec3 eye_position = vec3(0.0, surface_height, 0.0);
    float eye_depth = atmospheric_depth(eye_position, eyedir);
    float step_length = eye_depth/float(step_count);
    float eye_extinction = horizon_extinction(eye_position, eyedir, surface_height-2.15);
    
    vec3 rayleigh_collected = vec3(0.0, 0.0, 0.0);
    vec3 mie_collected = vec3(0.0, 0.0, 0.0);

    for(int i=0; i<step_count; i++){
        float sample_distance = step_length*float(i);
        vec3 position = eye_position + eyedir*sample_distance;
        float extinction = horizon_extinction(position, lightdir, surface_height-2.15);
        float sample_depth = atmospheric_depth(position, lightdir);
        vec3 influx = absorb(sample_depth, vec3(intensity), scatter_strength)*extinction;
        rayleigh_collected += absorb(sample_distance, Kr*influx, rayleigh_strength);
        mie_collected += absorb(sample_distance, influx, mie_strength);
    }

    rayleigh_collected = (rayleigh_collected*eye_extinction*pow(eye_depth, rayleigh_collection_power))/float(step_count);
    mie_collected = (mie_collected*eye_extinction*pow(eye_depth, mie_collection_power))/float(step_count);

    vec3 color = vec3(spot*mie_collected + mie_factor*mie_collected + rayleigh_factor*rayleigh_collected);

    // gl_FragColor = vec4(color, 1.0);
    return pow(color, vec3(4.2-clamp(dayLightIntens, 0, 1)*2.2));
}
#endif
