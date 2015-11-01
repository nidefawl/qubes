#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "util.glsl"


uniform sampler2DArray blockTextures;
uniform sampler2D noisetex;


in vec4 color;
in vec3 normal;
in vec4 texcoord;
in vec4 position;
// in vec2 light;
// flat in vec4 faceAO;
// flat in vec4 faceLight;
// flat in vec4 faceLightSky;
in float blockside;

in vec2 texPos;
flat in uvec4 blockinfo;


out vec4 out_Color;
 
void main(void) {
	vec4 tex=texture(blockTextures, vec3(texcoord.st, float(blockinfo.x)));
	if (tex.a<1)
		discard;
	vec3 color_adj = tex.rgb;
	color_adj *= color.rgb;
	//MINECRAFTISH BLOCK LIGHTING
	color_adj *= blockside;


	float f = float(blockinfo.y&0xFFFu);

	//TODO: figure out something better 
	if (f >= 12&&f <=16) { //EXPENSIVE LEAVE

		f-=12;

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
              vec3(0.4+(f/5.0)*0.5,0.78,0.1)*(0.27+(f/10.0)),
              vec3(0.15-clamp(1-f/4.0,0,1)*0.07),
              vec3(0.15),
              vec3(0.15)  )*1.2;
	}

	float alpha = tex.a*1;
    out_Color = vec4(color_adj, alpha);
}