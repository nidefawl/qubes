#version 150 core

const float INF         = 1.0 / 0.0;

vec3 computeClipInfo(float zn, float zf) { 
    if (zf == -INF) {
        return vec3(zn, -1.0f, +1.0f);
    } else {
        return vec3(zn  * zf, zn - zf, zf);
    }
}

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

uniform mat4 inverseProj;
uniform mat4 pixelProj;

out vec2 pass_texcoord;
flat out vec3 clipInfo;
flat out vec2 renderBufferSize;
flat out float nearPlaneZ;
flat out float farPlaneZ;

void main() {

	clipInfo = computeClipInfo(in_matrix.viewport.z, in_matrix.viewport.w);
	nearPlaneZ = in_matrix.viewport.z;
	farPlaneZ = in_matrix.viewport.w;
	pass_texcoord = in_texcoord.st;
	renderBufferSize = in_matrix.viewport.xy;
	gl_Position = in_matrix.mvp * in_position;
}
