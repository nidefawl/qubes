#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "util.glsl"

uniform sampler2D iChannel0;

uniform sampler2D iChannel1;

in vec2 pass_texcoord;
 
out vec4 out_Color;
uniform vec2 iMouse;

#define iGlobalTime FRAME_TIME*0.01
#define iResolution in_scene.viewport.xy
#define texture2D lookupTex
vec4 lookupTex(sampler2D sampler0, vec2 texcoord) {
  return texture(sampler0, vec2(texcoord.x, 1-texcoord.y));
}
vec4 lookupTex(sampler2D sampler0, vec2 texcoord, float offset) {
  return texture(sampler0, vec2(texcoord.x, 1-texcoord.y), offset);
}
const float cloudHeight = 6;
const float cloudBottom = -4.3;
const float cloudTop = cloudBottom+cloudHeight;

// Created by inigo quilez - iq/2013
// License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.

// Volumetric clouds. It performs level of detail (LOD) for faster rendering

float noise( in vec3 x )
{
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f*f*(3.0-2.0*f);
    vec2 uv = (p.xy+vec2(37.0,17.0)*p.z) + f.xy;
    vec2 rg = texture2D( iChannel0, (uv+ 0.5)/256.0, -100.0 ).yx;
    return -1.0+2.0*mix( rg.x, rg.y, f.z );
}

float map5( in vec3 p )
{
    vec3 q = p - vec3(0.5, 0.8, 1.5)*iGlobalTime;
    float f;
    f  = 0.50000*noise( q ); q = q*2.02;
    f += 0.25000*noise( q ); q = q*2.03;
    return clamp(  - p.y - cloudTop + f, 0.0, 1.0 );
}

float map4( in vec3 p )
{
    vec3 q = p - vec3(0.0,0.2,1.0)*iGlobalTime;
    float f;
    f  = 0.50000*noise( q ); q = q*2.02;
    f += 0.25000*noise( q ); q = q*2.03;
    f += 0.12500*noise( q ); q = q*2.01;
    f += 0.06250*noise( q );
    return clamp( 1.5 - p.y - 2.0 + 1.75*f, 0.0, 1.0 );
}
float map3( in vec3 p )
{
    vec3 q = p - vec3(0.0,0.1,1.0)*iGlobalTime;
    float f;
    f  = 0.50000*noise( q ); q = q*2.02;
    f += 0.25000*noise( q ); q = q*2.03;
    f += 0.12500*noise( q );
    return clamp( 1.5 - p.y - 2.0 + 1.75*f, 0.0, 1.0 );
}
float map2( in vec3 p )
{
    vec3 q = p - vec3(0.0,0.1,1.0)*iGlobalTime;
    float f;
    f  = 0.50000*noise( q ); q = q*2.02;
    f += 0.25000*noise( q );;
    return clamp( 1.5 - p.y - 2.0 + 1.75*f, 0.0, 1.0 );
}

vec3 sundir = normalize( vec3(-1.0,0.0,-1.0) );

vec4 integrate( in vec4 sum, in float dif, in float den, in vec3 bgcol, in float t )
{
    // lighting
    vec3 lin = vec3(0.65,0.7,0.75)*1.4 + vec3(1.0, 0.6, 0.3)*dif;        
    vec4 col = vec4( mix( vec3(1.0,0.95,0.8), vec3(0.25,0.3,0.35), den ), den );
    col.xyz *= lin;
    col.xyz = mix( col.xyz, bgcol, clamp(0.95-exp(-0.004*t*t),0,1) );
    // front to back blending    
    col.a *= 0.6;
    col.rgb *= col.a;
    return sum + col*(1.0-sum.a);
}
// #define MARCH(STEPS,MAPLOD) for(int i=0; i<STEPS; i++) { vec3  pos = ro + t*rd; if( pos.y<-3.0 || pos.y>2.0 || sum.a > 0.99 ) break; float den = MAPLOD( pos ); if( den>0.01 ) { float dif =  clamp((den - MAPLOD(pos+0.3*sundir))/0.6, 0.0, 1.0 ); sum = integrate( sum, dif, den, bgcol, t ); } t += max(0.05,0.02*t); }
void march5(int STEPS, inout float t, inout vec4 sum,  in vec3 ro, in vec3 rd, in vec3 bgcol) {
    for(int i=0; i<STEPS; i++) { 
        vec3  pos = ro + t*rd; 
        if( sum.a > 0.99 ) break;
        float den = map5( pos );
        if( den>0.01 ) { 
            float dif =  clamp((den - map5(pos+0.9*sundir))/1.0, 0.0, 1.0 );
            sum = integrate( sum, dif, den, bgcol, t );
        } 
        t += max(2.0,0.015*t);
    }
}
vec4 raymarch( in vec3 ro, in vec3 rd, in vec3 bgcol )
{
    vec4 sum = vec4(0.0);

    float t = 0.0;

    march5(15,t, sum, ro, rd, bgcol);
    // march4(30,t, sum, ro, rd, bgcol);
    // march3(30,t, sum, ro, rd, bgcol);
    // march2(30,t, sum, ro, rd, bgcol);

    return clamp( sum, 0.0, 1.0 );
}

mat3 setCamera( in vec3 ro, in vec3 ta, float cr )
{
    vec3 cw = normalize(ta-ro);
    vec3 cp = vec3(sin(cr), cos(cr),0.0);
    vec3 cu = normalize( cross(cw,cp) );
    vec3 cv = normalize( cross(cu,cw) );
    return mat3( cu, cv, cw );
}

vec4 render( in vec3 ro, in vec3 rd )
{
    // background sky     
    float sun = clamp( dot(sundir,rd), 0.0, 1.0 );
    // vec3 col=vec3()
    vec3 col = vec3(0.6,0.71,0.75) - rd.y*0.2*vec3(1.0,0.5,1.0) + 0.15*0.5;
    col += 0.2*vec3(1.0,.6,0.1)*pow( sun, 8.0 );

    // clouds    
    vec4 res = raymarch( ro, rd, col );
    col = col*(1.0-res.w) + res.xyz;
    
    // sun glare    
    col += 0.2*vec3(1.0,0.4,0.2)*pow( sun, 3.0 );

    return vec4( col, 1.0 );
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 p = (-iResolution.xy + 2.0*fragCoord.xy)/ iResolution.y;

    vec2 m = iMouse.xy/iResolution.xy;
    
    // camera
    vec3 ro = 4.0*normalize(vec3(sin(3.0*m.x), 0.4*m.y, cos(3.0*m.x)));
    vec3 ta = vec3(0.0, 1.0, 0.0);
    mat3 ca = setCamera( ro, ta, 0.0 );
    // ray
    vec3 rd = ca * normalize( vec3(p.xy,1.5));
    
    fragColor = render( ro, rd );
}

void main() {
	// out_Color=vec4(1);
	mainImage(out_Color, pass_texcoord*iResolution);
  out_Color.a=1.0;
}