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
	float dists = 100;
	float scale = in_position.w;
	float rot = in_color.w;
	vec3 nPos = normalize(in_position.xyz);

	float d5 = nPos.x * dists;
	float d6 = nPos.y * dists;
	float d7 = nPos.z * dists;

	float d8 = atan(nPos.x, nPos.z);
	float fd = sqrt(nPos.x*nPos.x+nPos.z*nPos.z);
	float d11 = atan(fd, nPos.y);

	float d9 = sin(d8);
	float d10 = cos(d8);

	float d12 = sin(d11);
	float d13 = cos(d11);
	
	float d15 = sin(rot);
	float d16 = cos(rot);
  	vec2 offsetxy = (in_texcoord.xy-vec2(0.5));
  	float d18 = offsetxy.y * 2.0 * scale;
  	float d19 = offsetxy.x * 2.0 * scale;
  	float d21 = d18 * d16 - d19 * d15;
  	float d23 = d19 * d16 + d18 * d15;
  	float d25 = -d21 * d13;
  	float d26 = d25 * d9 - d23 * d10;
  	float d27 = d21 * d12;
  	float d28 = d23 * d9 + d25 * d10;
  	vec3 oPos = vec3(d5 + d26, d6 + d27, d7 + d28);
	vec4 pos = in_matrix_3D.view * vec4(oPos, 1.0);
	pos /= pos.w;
	position = vec4(oPos, 1.0);
    gl_Position = in_matrix_3D.p * pos;
	vec4 camNormal = in_matrix_3D.normal * vec4(0, 0, -1, 1);
	normal = normalize(camNormal.xyz);
	texcoord = in_texcoord;
	color = vec4(in_color.rgb, 0.1);
}
void main2(void) {
	// mat4 normalmat = transpose(inverse(in_modelMat));
	float size = in_position.w;
	float rot = in_color.w;
	vec4 camNormal = in_matrix_3D.normal * vec4(0, 0, -1, 1);
	normal = normalize(camNormal.xyz);
	texcoord = in_texcoord;
	color = vec4(in_color.rgb, 0.1);
	vec3 inPos = in_position.xyz;
	// inPos = vec3(0);
        // create orientation vectors
  vec3 up = vec3(in_matrix_3D.mv[0][1], 
                 in_matrix_3D.mv[1][1], 
                 in_matrix_3D.mv[2][1]);
        vec3 vPlaneNormal = normalize(in_position.xyz);
        vec3 right = normalize(cross(vPlaneNormal, up));
         up = normalize(cross(right, vPlaneNormal));
	// vec3 right = vec3(in_matrix_3D.view[0][0], az
 //                    in_matrix_3D.view[1][0], 
 //                    in_matrix_3D.view[2][0]);
 
  vec2 offsetxy = (in_texcoord.xy-vec2(0.5));
	inPos = inPos + (right*offsetxy.x+up*offsetxy.y) * size;
	vec4 pos = in_matrix_3D.view*vec4(inPos - RENDER_OFFSET + PX_OFFSET.xyz, 1);
	pos /= pos.w;
	// vec2 offset = size*(in_texcoord.xy-vec2(0.5));
	// float c=cos(rot);
	// float s=sin(rot);
	// pos.y+=s*offset.x+c*offset.y;
	// pos.x+=c*offset.x-s*offset.y;
	// pos.xy+=offset.xy;
	position = pos;
    gl_Position = in_matrix_3D.p * position;

    
}