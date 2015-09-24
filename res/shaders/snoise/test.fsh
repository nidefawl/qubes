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

void main(void) 
{
	vec2 p = pass_texcoord;
    
    float sinTime = sin(iGlobalTime*0.1);

    float fc = iGlobalTime/50.0;
    float fc2 = sin(iGlobalTime/2.0);
    float sinTime2 = sin(iGlobalTime*0.04*6.28318);
    float sinTime3 = sin(iGlobalTime*0.01*6.28318);
    vec3 a = vec3(0.76,0.72,0.782);
    vec3 b = vec3(0.25,0.35, 0.4);
    vec3 c = vec3(0.8);
    vec3 d = vec3(0);
    p*=214.0+sinTime3*10.0;
    float sd=0.3+0.8*fc;
    float s4 = snoise(p+iGlobalTime*0.141);
    p+=vec2(sin(fc*3.14), cos(fc*3.14))*422.0;
    p*=0.015;
    float s0 = snoise(p);
    float s1 = snoise(p+vec2(0,1)*sd);
    float s2 = snoise(p+vec2(1,0)*sd);
    float s6 = snoise(p+vec2(s1,s2)-vec2(0.5));
    float s = (s0+s1+s2)/1.4;
    vec3 col = pal( s*7.0*sinTime*sinTime*sinTime*sinTime3+s*fc2, a, b, c,d );
    vec3 col2 = pal( col.g*s6+fc2, vec3(1.14)*min(0.5,dot(col,vec3(0.333))), b*2.2, col,d );
     col *= col2;
   col = pow(col, vec3(4));
	out_Color = vec4( col, 1.0 );
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