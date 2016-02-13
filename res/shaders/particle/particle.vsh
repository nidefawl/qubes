#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "tonemap.glsl"
#pragma attributes "particle"

in vec4 in_texcoord; 
in vec4 in_position; 
in vec4 in_color; 


out vec4 color;
out vec3 normal;
out vec4 texcoord;
out vec4 position;

 
void main(void) {
	// mat4 normalmat = transpose(inverse(in_modelMat));
	float size = in_position.w;
	float rot = in_color.w;
	vec4 camNormal = in_matrix_3D.normal * vec4(0, 0, -1, 1);
	normal = normalize(camNormal.xyz);
	texcoord = in_texcoord;
	color = vec4(in_color.rgb, 0.1);
	vec3 inPos = in_position.xyz;
	// inPos = vec3(0);
	vec4 pos = in_matrix_3D.mv*vec4(inPos - RENDER_OFFSET + PX_OFFSET.xyz, 1);
	pos /= pos.w;
	vec2 offset = size*(in_texcoord.xy-vec2(0.5));
	float c=cos(rot);
	float s=sin(rot);
	pos.y+=s*offset.x+c*offset.y;
	pos.x+=c*offset.x-s*offset.y;
	// pos.xy+=offset.xy;
	position = pos;
    gl_Position = in_matrix_3D.p * position;

    
}