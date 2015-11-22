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

void colorizeLeaves(inout vec3 color_adj, in vec3 position) {


    float idOffset = 0;

      float sampleDist = 4.4;
      vec2 p0 = position.xz *0.02;
      // float fSin = sin(FRAME_TIME*0.0003)*0.5+0.5;
      // p0 += vec2(fSin*110.3);
      vec2 p1 = p0 + vec2(1, 0)*sampleDist;
      vec2 p2 = p0 + vec2(0, 1)*sampleDist;
      float s0 = snoise(p0);
      float s1 = snoise(p1);
      float s2 = snoise(p2);
      color_adj*=pal((s0+s1+s2)/3.0, 
              vec3(0.4+(idOffset/5.0)*0.5,0.88,0.08)*(0.27+(idOffset/10.0)),
              vec3(0.1-clamp(1-idOffset/4.0,0,1)*0.07),
              vec3(0.15),
              vec3(0.15)  )*1.2;
}