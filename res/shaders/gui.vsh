#version 150 core

#pragma include "ubo_scene.glsl"
#pragma include "vertex_layout.glsl"

#ifdef VULKAN_GLSL
layout(push_constant) uniform PushConsts {
  vec4 box;
  vec4 color;
  float sigma;
  float corner;
  float fade;
  float zpos;
  int colorwheel;
  float valueH;
  float valueS;
  float valueL;
} pushConsts;
#define G_BOX pushConsts.box
#define G_SIGMA pushConsts.sigma
#define G_ZPOS pushConsts.zpos
#else
uniform vec4 box;
uniform float sigma;
uniform float zpos;
#define G_BOX box
#define G_SIGMA sigma
#define G_ZPOS zpos
#endif
out vec2 vertex;
out vec2 texcoord;
 
void main(void) {
	float padding = 2.0 * G_SIGMA;
	texcoord = in_texcoord.st;
	vec2 topleft = G_BOX.xy +PX_OFFSET.xy - padding;
	vec2 bottomright = G_BOX.zw+PX_OFFSET.xy + padding;
    vertex = mix(topleft, bottomright, in_texcoord.st);
    gl_Position = in_matrix_2D.mvp * vec4(vertex.x, vertex.y, G_ZPOS+PX_OFFSET.z, 1.0);
}