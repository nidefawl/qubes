#version 150 core


uniform sampler2D tex0;
uniform float iGlobalTime;

in vec4 pass_Color;
in vec2 pass_texcoord;
 
out vec4 out_Color;
 

// Simplex 2D noise
// Unknown author
vec3 permute(vec3 x) { return mod(((x*34.0)+1.0)*x, 289.0); }

float snoise(vec2 v){
  const vec4 C = vec4(0.211324865405187, 0.366025403784439,
           -0.577350269189626, 0.024390243902439);
  vec2 i  = floor(v + dot(v, C.yy) );
  vec2 x0 = v -   i + dot(i, C.xx);
  vec2 i1;
  i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
  vec4 x12 = x0.xyxy + C.xxzz;
  x12.xy -= i1;
  i = mod(i, 289.0);
  vec3 p = permute( permute( i.y + vec3(0.0, i1.y, 1.0 ))
  + i.x + vec3(0.0, i1.x, 1.0 ));
  vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy),
    dot(x12.zw,x12.zw)), 0.0);
  m = m*m ;
  m = m*m ;
  vec3 x = 2.0 * fract(p * C.www) - 1.0;
  vec3 h = abs(x) - 0.5;
  vec3 ox = floor(x + 0.5);
  vec3 a0 = x - ox;
  m *= 1.79284291400159 - 0.85373472095314 * ( a0*a0 + h*h );
  vec3 g;
  g.x  = a0.x  * x0.x  + h.x  * x0.y;
  g.yz = a0.yz * x12.xz + h.yz * x12.yw;
  return 130.0 * dot(m, g);
}

vec3 pal( in float t, in vec3 a, in vec3 b, in vec3 c, in vec3 d )
{
    return a + b*cos( 6.28318*(c*t+d) );
}

void main4(void) 
{
	vec2 p = pass_texcoord*1.7;
	// p+=vec2(4);
    
    float sinTime = sin(iGlobalTime*2.52);
    float pos = 0.5+clamp((sinTime)*4.0, -1.0, 1.0)*0.5;
    float f4 = snoise(p*3.0);
    vec3 a = vec3(f4+0.4,0.2,0.2);
    vec3 b = vec3(1.15,1.15, 1.1)*f4;
    vec3 c = vec3(3.9,0.1+f4*1,2.1)*f4;
    vec3 d = vec3(f4*0.1);
    float f1 = snoise(p);
    float f2 = snoise(dot(p, vec2(2.3))*f1+p*1.03-p*6.4+vec2(0.3,0.3)*iGlobalTime*0.44);
    float f3 = snoise((dot(p, vec2(f2))+p*0.42-p*10.0-vec2(0.3,0.3)*iGlobalTime*0.84)*0.4);
    vec3 col1 = pal(f2*f1,a,b,c,d)*f3;
    vec3 col2 = pal(f2*f3,b,a,c, d)*f1;
    col1 = pow(col1+vec3(1.7)*pos*0.1, col2);
	out_Color = vec4( col1, 1.0 );
}

void main22(void) 
{
	vec4 tex = texture(tex0, pass_texcoord.st);
    float n = snoise(pass_texcoord*32.0+vec2(1)*iGlobalTime);
    out_Color = tex*pass_Color*n;

}
float pow4(float x) {
	x *= x;
	return x * x;
}
float pow3(float x) {
	return x * x * x;
}
void main(void) {
	vec4 tex = texture(tex0, pass_texcoord.st);
    out_Color = vec4(tex.rgb,1);
}
 
void main3(void) 
{
	// const int len = 1024;
	// int a = int(tex.y*3);
	// for (int i = 0; i < len; i++) {
		// tex.x = mod(tex.x*i, 1.0f); //slow 4.35ms
		// tex.x = sqrt(tex.x*i); //slow 4.35ms
		// tex.x = mix(tex.x*i, i, 0.5); //fast 2.5ms
		// tex.x = clamp(1+tex.x*i, 0.0f, 1.0f); //very fast (free to 0.3ms)
		// tex.x = fract(tex.x+i); // slow 4.35ms
		// tex.x = floor(tex.x+i); // fast 2.16ms
		// tex.x = int(tex.x+i); // very slow 6.513ms
		// tex.x = float(tex.x+i); // very fast (free)
		// tex.x = uint(tex.x+i); // very slow 6.513ms
		// tex.x = float((tex.x+i)==3); // slow 4.35ms
		// tex.x = radians(tex.x+i); // very fast 0.75ms
		// tex.x = degrees(tex.x+i); // very fast 0.75ms
		// tex.x = sin(tex.x+i); // fast 2.18ms
		// tex.x = atan(tex.x+i, tex.x-i); // very slow 12.88ms
		// tex.x = pow(tex.x+i, 12); // slow 4.35ms
		// tex.x = pow(tex.x+i, 1); // free
		// tex.x = pow(tex.x+i, 0); // free
		// tex.x = pow(tex.x+i, 0.5); // slow 4.35ms
		// tex.x = pow(tex.x+i, 2); // very fast 0.615ms
		// tex.x = pow(tex.x+i, 4); // slow 4.35ms
		// tex.x = pow4(tex.x+i); // fast 2.464ms
		// tex.x = pow(tex.x+i, 3); // fast 2.2ms
		// tex.x = pow3(tex.x+i); // fast 2.2ms

		// tex.x = exp(tex.x+i); // slow 4.35ms
		// tex.x = log(tex.x+i); // fast 2.2ms
		// tex.x = exp2(tex.x+i); // fast 2.2ms
		// tex.x = log2(tex.x+i); // fast 2.2ms
		// tex.x = sqrt(tex.x+i); // slow 4.35ms
		// tex.x = inversesqrt(tex.x+i); // fast 2.2ms
		// tex.x = abs(tex.x+i); // very fast 0.55ms
		// tex.x = sign(tex.x+i); // very slow 8.35ms
		// tex.x = trunc(tex.x+i); // fast 2.2ms
		// tex.x = round(tex.x+i); // fast 2.2ms
		// tex.x = roundEven(tex.x+i); // fast 2.2ms
		// tex.x = ceil(tex.x+i); // fast 2.2ms
		// tex.x = min(tex.x+i, tex.y+i); // fast 2.2ms
		// tex.x = max(tex.x+i, tex.y+i); // fast 2.5ms
		// tex.x = step(1, tex.x+i); // slow 4.35ms
		// tex.x = step(0, tex.x+i); // slow 4.32ms
		// tex.x = step(-1.23, tex.x+i); // slow 4.35ms
		// tex.x = step(14.23333, tex.x+i); // slow 4.35ms
		// tex.x = smoothstep(0, 1, tex.x+i); // fast 2.85ms
		// tex.x = smoothstep(0, 12, tex.x+i); // fast 2.7ms
		// tex.x = smoothstep(4, 12, tex.x+i); // fast 3.3ms
		// tex.x = smoothstep(-4, 12, tex.x+i); // fast 3.3ms
		// tex.x = length(tex)+i; // slow 4.9ms
		// tex.x = distance(tex, tex.bgra)+i; // slow 4.8ms
		// tex.x = distance(tex, tex.bgra)+i; // slow 4.8ms
		// tex.rgb = cross(tex.rgb, tex.brg)*i; // slow 4.3ms
		// tex.x = uint(tex.x+i); //6.42ms
		// tex.x = uint(tex.x)+i; //6.42ms
		// tex.x = int(tex.x)+i; //4.36ms

	// }
	// tex /= float(len);
 //    out_Color = tex*pass_Color*noise1(pass_texcoord);
    // out_Color = vec4(tex.rgb,1);
}
// void main2(void) {
//     int i=0;
//     int a=28;
//     float f1 = 0;
//     for (i=0; i < a; i++) {
//     	aa[i]=aa[i+1];
//     	f1+=1;
// 	}
// 	f1 /= a;
//     out_Color = vec4(vec3(clamp(f1,0,1)),1)*pass_Color;
// }