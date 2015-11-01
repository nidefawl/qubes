#version 150 core

uniform sampler2DArray blockTextures;

in vec2 texcoord;
flat in uvec4 blockinfo;

out vec4 out_Color;

void main() {
	vec4 tex=texture(blockTextures, vec3(texcoord.st, float(blockinfo.x)), -4);
	if (tex.a<1.0)
		discard;
    out_Color = vec4(tex.rgb, 1.0);
}
