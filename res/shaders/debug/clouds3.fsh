#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "util.glsl"

uniform sampler2D iChannel0;

uniform sampler2D iChannel1;

in vec2 pass_texcoord;
 
out vec4 out_Color;
uniform vec2 iMouse;

#define iGlobalTime FRAME_TIME*0.05
#define iResolution in_scene.viewport.xy
#define texture2D lookupTex
vec4 lookupTex(sampler2D sampler0, vec2 texcoord) {
  return texture(sampler0, vec2(texcoord.x, 1-texcoord.y));
}
vec4 lookupTex(sampler2D sampler0, vec2 texcoord, float offset) {
  return texture(sampler0, vec2(texcoord.x, 1-texcoord.y), offset);
}


// maybe something between 0.5 and 3.0
const float CLOUD_HEIGHT = 2.0;

// scale of clouds
const float UV_FREQ = 0.005;

// cloudiness, bigger number = less clouds
const float CLOUD_FILTER = 0.4;

// parallax layers
const int PARALLAX_LAYERS = 8;


float filter(float f, float a)
{
   f = clamp(f - a, 0.0, 1.0);
   f /= (1.0 - a);    
   return f;
}

float fbm(vec2 uv)
{
    float f = (texture2D(iChannel0, uv * 2.0).r - 0.5) * 0.2;
    f += (texture2D(iChannel0, uv * 4.0).r - 0.5) * 0.125;
    f += (texture2D(iChannel0, uv * 8.0).r - 0.5) * 0.125 * 0.5;
    f += (texture2D(iChannel0, uv * 16.0).r - 0.5) * 0.125 * 0.25;
    f += (texture2D(iChannel0, uv * 32.0).r - 0.5) * 0.125 * 0.24;
    f += (texture2D(iChannel0, uv * 64.0).r - 0.5) * 0.125 * 0.22;
    f += (texture2D(iChannel0, uv * 128.0).r - 0.5) * 0.125 * 0.12;
    f += (texture2D(iChannel0, uv * 256.0).r - 0.5) * 0.125 * 0.1;
    f += 0.5;
    return clamp(f, 0.0, 1.0);
}



vec2 getuv(in vec2 uv, float l)
{
    vec3 rd = normalize(vec3(uv, 0.4));
    vec2 _uv = vec2(rd.x / abs(rd.y) * l, rd.z / abs(rd.y) * l);
    return _uv;
}

// cloud rendering
void clouds (vec2 uv, inout vec4 col, float t, float freq)
{
    vec2 _uv = getuv(uv, 1.0);
    _uv.y += t;
    float l = 1.0;
    
    vec2 mouse = (iMouse.xy - iResolution.xy * 0.5) / iResolution.xy;

    for (int i = 0; i < PARALLAX_LAYERS; ++i)
    {
        // 3 parallax layers of clouds
        float h = fbm(_uv * freq) * 0.5;
        h += fbm(vec2(-t * 0.001, t * 0.0015) + _uv * freq * 1.1) * 0.35;
        h += fbm(vec2(t * 0.001, -t * 0.0025) + _uv * freq * 1.2) * 0.15;
        
        float f = filter(h, CLOUD_FILTER + mouse.x * 0.1);
        f -= (l - 1.0) * CLOUD_HEIGHT; // height
        
        f = clamp(f, 0.0, 1.0);
        
        col += f * vec4(0.9, 0.9, 1.0, 1.0) * (1.0 - col.a);
        
        
        l *= 1.09 - h * (0.18 * (1.0 + (mouse.y + 0.5) * 0.2) ); // parallax control, offset uv by fbm density
       
        _uv = getuv(uv, l);
        _uv.y += t;
    }
}





void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    fragColor = vec4(0.0);
    
  vec2 uv = fragCoord.xy / iResolution.xy;
    uv -= vec2(0.5);
    uv.y /= iResolution.x / iResolution.y;
    
    vec4 dark = vec4(0.75, 0.75, 0.75, 0.0) * 1.5;
    vec4 light = vec4(0.75, 0.75, .75, 0.0) * 1.5;
    vec4 bg = vec4(0.8, 0.9, 1.0, 0);//mix(light, dark, abs(uv.y) * 6.5);
    vec4 col = vec4(0);
    
    vec2 _uv = uv;
    _uv.y -= iGlobalTime * 0.01;
    _uv.x *= 0.1;
    vec2 guv = vec2(0.0);
    
    if (uv.y > 0.0)
    {
    
      clouds(uv + guv * 0.015 * mix(-0.0, 1.0, clamp(abs(uv.y) * 5.0 - 0.04, 0.0, 1.0)  ), col, iGlobalTime * 0.02, UV_FREQ);

      fragColor = mix(bg, col, col.a);
            vec4 wcolor = light * 1.3;
      fragColor = mix(wcolor, fragColor, (smoothstep(0., .1, uv.y)));        
    // vec2 _uv = uv;  
    //     _uv.x *= 0.1;
    //     fragColor = mix(fragColor, vec4(1.0), 1.0 - smoothstep(0.0, 0.1, length(_uv)));

    // float contr = 0.1;
    // fragColor = mix(vec4(0.0), vec4(1.0), fragColor * contr + (1.0 - contr) * fragColor * fragColor * (3.0 - 2.0 * fragColor));

    }
    // uv.x *= 0.015;
    // fragColor = mix(fragColor, vec4(1.0), 1.0 - smoothstep(0., 0.01, length(uv)));
    // fragColor = mix(fragColor, vec4(1.0), 1.0 - smoothstep(0., 0.005, length(uv)));
    
    // contrast
}
void main() {
	// out_Color=vec4(1);
	mainImage(out_Color, pass_texcoord*iResolution);
  out_Color.a=1.0;
}