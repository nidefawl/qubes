#version 420

#pragma include "ubo_scene.glsl"
layout(binding = 0, offset = 0) uniform atomic_uint buffer_index_counter;

layout(binding = 0, r32ui) coherent uniform uimage2D ex_image;

uniform uint clear_value;

out vec4 out_Color;

void main()
{
	// uint curVal = atomicCounter(buffer_index_counter);
	// out_Color = vec4(vec3(curVal)/32.0, 1.0);
	// float viewPortPixels = in_scene.viewport.x*in_scene.viewport.y;
	// float br = float(count)/float(viewPortPixels);
	ivec2 pixel_coords = ivec2(int(gl_FragCoord.x), int(gl_FragCoord.y));
	uint count = atomicCounterIncrement(buffer_index_counter);
	uint val = imageAtomicExchange(ex_image, pixel_coords, uint(gl_FragCoord.x));
	if((val == uint(gl_FragCoord.x)) && (val != clear_value)) {
		out_Color = vec4(vec3(1), 1.0);
	}
	else {
		out_Color = vec4(vec3(0), 1.0);
	}
}