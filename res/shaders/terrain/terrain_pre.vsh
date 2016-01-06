#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "ubo_constants.glsl"
#pragma include "vertex_layout.glsl"
#pragma include "blockinfo.glsl"
#pragma include "util.glsl"

out vec4 texcoord;
out vec4 position;
flat out uvec4 blockinfo;

void main() {
	vec4 pos = in_position;
	texcoord = in_texcoord;
	blockinfo = in_blockinfo;

	float distCam = length(in_position.xyz - CAMERA_POS);

	uint vertDir = BLOCK_VERTDIR(blockinfo);
	vec3 dir = vertexDir.dir[vertDir].xyz;
    float renderpass = BLOCK_RENDERPASS(blockinfo);

	if (renderpass>=6&&renderpass<=7) {
		float waveType = renderpass-6;
	    float sampleDist = 8.4;
	    vec2 p0 = vec2(0);//floor(pos.xz*0.1f);
	    vec2 floord = pos.xz+0.5;
	    float aniSpeed = FRAME_TIME*0.25;
	    aniSpeed+=floord.x*1+floord.y*1;
	    aniSpeed*=0.0015;
	    vec2 p2 = p0 + vec2(aniSpeed, 33)*sampleDist;
	    vec2 p1 = p0 + vec2(123, aniSpeed)*sampleDist;
	    
	    float s0 = snoise(p2);
	    float s1 = snoise(p1);
	    float s2 = s0*s1;
	    float wd = 0.83+(waveType*0.5);
	 //    // float frand = (s0+s1+s2)/16.0;
		pos.x += s0*wd*s2;
		pos.z += s1*wd*s2;
		// pos.z += f*2*cos(aniSpeed)*frand;
	}

	const float face_offset = 1/32.0;
	float distScale = face_offset*clamp(pow((distCam+8)/200, 1.35), 0.0008, 1);
	pos.x += dir.x*distScale;
	pos.y += dir.y*distScale;
	pos.z += dir.z*distScale;


	position = pos;
	gl_Position = in_matrix_3D.mvp * pos;
}
