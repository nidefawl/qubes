#version 150 core




uniform sampler2DArray blockTextures;
uniform int renderWireFrame;


in vec4 color;
in vec3 normal;
in vec4 texcoord;
in vec3 vposition;
in vec4 blockinfo;
in highp vec3 triangle;

out vec4 out_Color;
out vec4 out_Normal;
out vec4 out_Material;


float getBrightness(vec2 b) {
	return (1-pow(1-b.x, 2))*(1-pow(1-b.y, 2));
}


void main() {

	vec4 tex = texture(blockTextures, vec3(texcoord.st, blockinfo.x));
	if (renderWireFrame > 0) {
		float dist = length(vposition);
		float fdistscale = 1.0f-clamp((dist - 10.0f) / 60.0f, 0.0f, 0.9f);
	    vec3 d = fwidth(triangle)*fdistscale;
	    vec3 tdist = smoothstep(vec3(0.0), d*3.0f, triangle);
	    tex.rgb = mix(vec3(1,0,0), tex.rgb, min(min(tdist.x, tdist.y), tdist.z));
	}
    out_Color = tex*color;
    out_Normal = vec4((normal) * 0.5f + 0.5f, 1.0f);
    out_Material = vec4(blockinfo.x+1.0f, blockinfo.y, blockinfo.z, 1);
    // gl_FragData[0] = vec4(0,1,1,1);
}
