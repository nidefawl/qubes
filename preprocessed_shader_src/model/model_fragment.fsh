#version 450
#line 2 0
#define VULKAN_GLSL
#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable

#line 1 1
#define VULKAN_GLSL
const float A = 0.15;
const float B = 0.50;
const float C = 0.10;
const float D = 0.20;
const float E = 0.02;
const float F = 0.30;
const float W = 11.2;



void srgb(inout float v)
{
    v = clamp(v, 0.0, 1.0);
    float K0 = 0.03928;
    float a = 0.055;
    float phi = 12.92;
    float gamma = 2.4;
    v = v <= K0 / phi ? v * phi : (1.0 + a) * pow(v, 1.0 / gamma) - a;
}
void linear(inout float v)
{
    v = clamp(v, 0.0, 1.0);
    float K0 = 0.03928;
    float a = 0.055;
    float phi = 12.92;
    float gamma = 2.4;
    v = v <= K0 ? v / phi : pow((v + a) / (1.0 + a), gamma);
}
void linearizeInput(inout vec3 srgb)
{
    #ifndef SRGB_TEXTURES
    linear(srgb.x);
    linear(srgb.y);
    linear(srgb.z);
    #endif
}
void linearizeInput2(inout vec3 srgb)
{
    linear(srgb.x);
    linear(srgb.y);
    linear(srgb.z);
}

void srgbToLin(inout vec3 srgb) {
    linear(srgb.x);
    linear(srgb.y);
    linear(srgb.z);
}
void linToSrgb(inout vec3 linear) {
    srgb(linear.x);
    srgb(linear.y);
    srgb(linear.z);
}

vec3 Uncharted2Tonemap(vec3 x)
{
    // http://www.gdcvault.com/play/1012459/Uncharted_2__HDR_Lighting
    // http://filmicgames.com/archives/75 - the coefficients are from here
    return ((x*(A*x+C*B)+D*E)/(x*(A*x+B)+D*F))-E/F; // E/F = Toe Angle
}

#define MAX_COLOR_RANGE 48.0//TONEMAP   
vec3 Uncharted2Tonemap2(vec3 x) {
float A2 = 1.0;    //brightness multiplier
float B2 = 0.5;   //black level (lower means darker and more constrasted, higher make the image whiter and less constrasted)
float C2 = 0.1;    //constrast level 
  float D2 = 0.2;    
  float E2 = 0.02;
  float F2 = 0.3;
  float W2 = MAX_COLOR_RANGE;
  return ((x*(A2*x+C2*B2)+D2*E2)/(x*(A2*x+B2)+D2*F2))-E2/F2;
}


vec3 ToneMap( in vec3 texColor, float exposure)
{

  vec3 curr = Uncharted2Tonemap2(exposure*texColor);
  
  vec3 whiteScale = 1.0f/Uncharted2Tonemap2(vec3(MAX_COLOR_RANGE));
 vec3 color = curr*whiteScale;
   // vec3 curr = Uncharted2Tonemap(exposure*texColor);

   // vec3 whiteScale = 1.0f/Uncharted2Tonemap(vec3(W));
   // vec3 color = curr*whiteScale;

   // vec3 retColor = pow(color,vec3(1/2.2));
   linToSrgb(color);
   return color;
}
#line 1 2
#define VULKAN_GLSL

#define BLOCK_ID_u32(blockinfo32) ((blockinfo32 >> 16u) & 0xFFFu)
#define BLOCK_RENDERPASS_u32(blockinfo32) ((blockinfo32 >> 28u) & 0xFu)
#define BLOCK_TEX_SLOT_u32(blockinfo32) float(blockinfo32&0xFFFu)
#define BLOCK_NORMAL_SLOT_u32(blockinfo32) float((blockinfo32&0xF000u)>>12u)

#define BLOCK_ID(blockinfo) (blockinfo.y&0xFFFu)
#define BLOCK_RENDERPASS(blockinfo) float((blockinfo.y&0xF000u)>>12u)
#define BLOCK_TEX_SLOT(blockinfo) float(blockinfo.x&0xFFFu)
#define BLOCK_NORMAL_SLOT(blockinfo) float((blockinfo.x&0xF000u)>>12u)
#define BLOCK_FACEDIR(blockinfo) (blockinfo.w&0x7u)
#define BLOCK_VERTDIR(blockinfo) ((blockinfo.w >> 3u) & 0x3Fu)
#define BLOCK_AO_IDX_0(blockinfo) (in_blockinfo.z)&AO_MASK
#define BLOCK_AO_IDX_1(blockinfo) (in_blockinfo.z>>2u)&AO_MASK
#define BLOCK_AO_IDX_2(blockinfo) (in_blockinfo.z>>4u)&AO_MASK
#define BLOCK_AO_IDX_3(blockinfo) (in_blockinfo.z>>6u)&AO_MASK
#define IS_SKY(blockid) float(blockid==0u)
#define IS_WATER(blockid) float(blockid==10u)

#define IS_LIGHT(blockid) float(blockid==2222u)

#define IS_ILLUM(renderpass) float(renderpass==4)
#define IS_BACKFACE(renderpass) float(renderpass==3)

#define ENCODE_RENDERPASS(renderpass) ((uint(renderpass)&0xFu)<<12u)
#line 1 3
#define VULKAN_GLSL

layout(set = 0, binding = 0, std140) uniform uboMatrix3D
{
    mat4 mvp;
    mat4 mv;
    mat4 view;
    mat4 vp;
    mat4 p;
    mat4 normal;
    mat4 mv_inv;
    mat4 proj_inv;
    mat4 mvp_inv;
} in_matrix_3D;

layout(set = 0, binding = 1, std140) uniform uboMatrix2D
{
    mat4 mvp;
    mat4 p3DOrtho;
    mat4 mv3DOrtho;
} in_matrix_2D;

layout(set = 0, binding = 2, std140) uniform uboSceneData
{
    vec4 cameraPosition;
    vec4 framePos;
    vec4 viewport;
    vec4 prevCameraPosition;
    vec4 sceneSettings;
} in_scene;

layout(set = 1, binding = 0, std140) uniform uboTransformStack
{
    vec4 pxoffset;
} in_transform_stack;



#define FRAME_TIME in_scene.framePos.w
#define CAMERA_POS in_scene.cameraPosition.xyz
#define PREV_CAMERA_POS in_scene.prevCameraPosition.xyz
#define RENDER_OFFSET in_scene.framePos.xyz
#define PX_OFFSET in_transform_stack.pxoffset
#define Z_FAR in_scene.viewport.w
#define Z_NEAR in_scene.viewport.z
#define RENDERER 1
#line 10 0
#define RENDERER_WORLD_MAIN 0
#define RENDERER_WORLD_SHADOW 1
#define RENDERER_MODELVIEWER 2
#define RENDERER_SCREEN 3
#define MODEL_SPEC_ROUGHNESS 0.9f

layout (set = 2, binding = 0) uniform sampler2D tex0;

#if RENDERER != RENDERER_WORLD_SHADOW

layout (location = 0)  in vec4 pass_color;
layout (location = 1)  in vec3 pass_normal;
layout (location = 2)  in vec2 pass_texcoord;
layout (location = 3)  in vec4 pass_position;

#endif


layout (location = 0) out vec4 out_Color;
#if RENDERER != RENDERER_WORLD_SHADOW
layout (location = 1) out vec4 out_Normal;
layout (location = 2) out uvec4 out_Material;
layout (location = 3) out vec4 out_Light;
#endif

#if RENDERER >= RENDERER_MODELVIEWER

	void main(void) {
		vec4 tex = texture(tex0, pass_texcoord.st);
		// if (tex.a<1)
		// 	discard;
		 // tex = vec4(vec3(1),1);
		vec4 color_adj1 = tex;
		vec4 color_adj2 = pass_color;
		srgbToLin(color_adj1.rgb);
		srgbToLin(color_adj2.rgb);
		vec4 color_adj = color_adj1*color_adj2;

		float alpha = tex.a*1;
		#if RENDERER == RENDERER_MODELVIEWER
		    out_Color = vec4(color_adj.rgb, alpha);
		    out_Normal = vec4(pass_normal.xyz * 0.5f + 0.5f, MODEL_SPEC_ROUGHNESS);
		    out_Material = uvec4(0u,1u+ENCODE_RENDERPASS(5),0u,0u);
		    out_Light = vec4(1, 0,  1, 1);
		#else
			vec3 toneMapped = ToneMap(color_adj.rgb*0.1, 130);
		    out_Color = vec4(toneMapped, alpha);
		#endif
	}
#else
	#if RENDERER == RENDERER_WORLD_SHADOW
		void main(void) {
		  out_Color = vec4(0, 0, 0, 1);
		}
	#else
		void main(void) {
			vec4 tex = texture(tex0, pass_texcoord.st);
			// if (tex.a<1)
			// 	discard;
			 // tex = vec4(vec3(1),1);
			vec3 color_adj = tex.rgb;
			color_adj *= pass_color.rgb;
			srgbToLin(color_adj.rgb);

			float alpha = tex.a*1;
		    out_Color = vec4(color_adj, alpha);
		    out_Normal = vec4((pass_normal.xyz) * 0.5f + 0.5f, MODEL_SPEC_ROUGHNESS);
		    out_Material = uvec4(0u,1u+ENCODE_RENDERPASS(5),0u,0u);
		    out_Light = vec4(1, 0,  1, 1);
		}
	#endif
#endif
