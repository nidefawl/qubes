#version 130

uniform sampler2DArray blockTextures;
uniform int renderWireFrame;

in vec4 color;
in vec4 lmcoord;
in vec3 normal;
in vec3 globalNormal;
in vec4 texcoord;
in vec3 vposition;
flat in int blockTexture;
in highp vec3 triangle;

out vec4 out_Color;


float getBrightness(vec2 b) {
	return (1-pow(1-b.x, 2))*(1-pow(1-b.y, 2));
}


void main() {
	vec4 tex = texture(blockTextures, vec3(texcoord.st, blockTexture)) * color;
	vec4 fogColor = vec4(gl_Fog.color.rgb, tex.a);
	//distance
	float dist = length(vposition);
	float fdistscale = 1.0f-clamp((dist - 10.0f) / 60.0f, 0.0f, 0.8f);
    float fogFactor = clamp( (dist - 200.0f) / 700.0f, 0.0f, 0.2f );
    fogFactor += clamp( (dist - 20.0f) / 420.0f, 0.0f, 0.06f );
    tex = mix(tex, fogColor, fogFactor);
	if (renderWireFrame) {
	    highp vec3 d = fwidth(triangle)*fdistscale;
	    highp vec3 tdist = smoothstep(vec3(0.0), d*3.0f, triangle);
	    tex.rgb = mix(vec3(1,0,0), tex.rgb, min(min(tdist.x, tdist.y), tdist.z));
	}

    out_Color = tex;
	// gl_FragData[0] = vec4(1, 1, 0, 1.0);
	//float brightness = getBrightness(wpos);
	//gl_FragData[0] = vec4(brightness, brightness, brightness, 1);
}