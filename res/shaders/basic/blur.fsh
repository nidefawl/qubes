#version 150 core
#define BLUR_SCALE 2.0

#pragma include "ubo_scene.glsl"

const int KERNEL_SIZE = 25;
const float kernel_weights[KERNEL_SIZE] = float[]
(
	0.00048031, 0.00500493, 0.01093176, 0.00500493, 0.00048031,
	0.00500493, 0.05215252, 0.11391157, 0.05215252, 0.00500493,
	0.01093176, 0.11391157, 0.24880573, 0.11391157, 0.01093176,
	0.00500493, 0.05215252, 0.11391157, 0.05215252, 0.00500493,
	0.00048031, 0.00500493, 0.01093176, 0.00500493, 0.00048031
);
const float weights_factor = 1.01238;


//-----------------------------------------------------------------------------------------------
vec2[KERNEL_SIZE] GetOffsetArray( const vec2 pixelScale )
{
	vec2 offset[KERNEL_SIZE] = vec2[]
	(
		vec2 ( -pixelScale.s * 2.0, -pixelScale.t * 2.0 ),
		vec2 ( -pixelScale.s, -pixelScale.t * 2.0 ),
		vec2 ( 0.0, -pixelScale.t * 2.0 ),
		vec2 ( pixelScale.s, -pixelScale.t * 2.0 ),
		vec2 ( pixelScale.s * 2.0, -pixelScale.t * 2.0 ),
		
		vec2 (	-pixelScale.s * 2.0, -pixelScale.t ),
		vec2 (	-pixelScale.s, -pixelScale.t ),
		vec2 (	0.0, -pixelScale.t ),
		vec2 (	pixelScale.s, -pixelScale.t ),
		vec2 (	pixelScale.s * 2.0, -pixelScale.t ),
		
		vec2 (	-pixelScale.s * 2.0, 0.0 ),
		vec2 (	-pixelScale.s, 0.0 ),
		vec2 (	0.0, 0.0 ),
		vec2 (	pixelScale.s, 0.0 ),
		vec2 (	pixelScale.s * 2.0, 0.0 ),
		
		vec2 (	-pixelScale.s * 2.0, pixelScale.t ),
		vec2 (	-pixelScale.s, pixelScale.t ),
		vec2 (	0.0, pixelScale.t ),
		vec2 (	pixelScale.s, pixelScale.t ),
		vec2 (	pixelScale.s * 2.0, pixelScale.t ),
		
		vec2 (	-pixelScale.s * 2.0, pixelScale.t * 2.0 ),
		vec2 (	-pixelScale.s, pixelScale.t * 2.0 ),
		vec2 (	0.0, pixelScale.t * 2.0 ),
		vec2 (	pixelScale.s, pixelScale.t * 2.0 ),
		vec2 (	pixelScale.s * 2.0, pixelScale.t * 2.0 )
	);

	return offset;
}

uniform sampler2D texColor;

in vec2 pass_texcoord;
 
out vec4 out_Color;
 
vec4 blur13(sampler2D image, vec2 uv, vec2 resolution, vec2 direction) {
  vec4 color = vec4(0.0);
  vec2 off1 = vec2(1.411764705882353) * direction;
  vec2 off2 = vec2(3.2941176470588234) * direction;
  vec2 off3 = vec2(5.176470588235294) * direction;
  color += texture2D(image, uv) * 0.1964825501511404;
  color += texture2D(image, uv + (off1 / resolution)) * 0.2969069646728344;
  color += texture2D(image, uv - (off1 / resolution)) * 0.2969069646728344;
  color += texture2D(image, uv + (off2 / resolution)) * 0.09447039785044732;
  color += texture2D(image, uv - (off2 / resolution)) * 0.09447039785044732;
  color += texture2D(image, uv + (off3 / resolution)) * 0.010381362401148057;
  color += texture2D(image, uv - (off3 / resolution)) * 0.010381362401148057;
  return color;
}

void main() {
	vec2 pixelScale;
	pixelScale.x = BLUR_SCALE * ( 1.0 / in_matrix.viewport.x );
	pixelScale.y = BLUR_SCALE * ( 1.0 / in_matrix.viewport.y );
	vec2[KERNEL_SIZE] offset = GetOffsetArray( pixelScale );

	vec3 color = vec3( 0.0, 0.0, 0.0 );
	for ( int i = 0; i < KERNEL_SIZE; ++i )
	{
		color += texture2D( texColor, pass_texcoord + offset[i] ).rgb * kernel_weights[i] * weights_factor;
	}

	out_Color = vec4( color, 1.0 );
}