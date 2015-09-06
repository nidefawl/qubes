#version 150 core

// out vec4 out_Color;

// void main() {
//   out_Color = vec4(0, 0, 0, 1);
// }




uniform sampler2DArray blockTextures;


in vec4 color;
in vec3 normal;
in vec4 texcoord;
in vec4 blockinfo;

out vec4 out_Color;
out vec4 out_Normal;
out vec4 out_Material;


float getBrightness(vec2 b) {
	return (1-pow(1-b.x, 2))*(1-pow(1-b.y, 2));
}


void main() {

	vec4 tex = texture(blockTextures, vec3(texcoord.st, blockinfo.x));

    out_Color = tex*color;
    out_Normal = vec4((normal) * 0.5f + 0.5f, 1.0f);
    out_Material = vec4(blockinfo.x+1.0f, blockinfo.y, blockinfo.z, 1);
    // gl_FragData[0] = vec4(0,1,1,1);
}
