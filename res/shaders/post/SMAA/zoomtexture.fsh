#version 150 core

#pragma include "ubo_scene.glsl"
uniform sampler2D texColor;
uniform vec2 mousePixelPos;

in vec2 pass_texcoord;
out vec4 out_Color;
 
void main(void) {
	vec2 pixelPos = pass_texcoord.st*in_scene.viewport.xy;
	float scale = 8;
	int pixels = 64;
	vec2 rect = vec2(pixels*scale);
	vec2 pos = vec2(in_scene.viewport.x-1-rect.x, 0);
	vec4 minmax = vec4(pos.xy, pos.xy+rect.xy);
	if (pixelPos.x < minmax.x || pixelPos.y < minmax.y) {
		discard;
	}
	if (pixelPos.x >= minmax.z || pixelPos.y >= minmax.w) {
		discard;
	}
	pixelPos.xy -= minmax.xy+(minmax.zw-minmax.xy)*0.5;
	float scale2 = 1.0/8.0;
	ivec2 pixel = ivec2(pixelPos*scale2+mousePixelPos);
	// vec2 newTexcoord = vec2(pixel/in_scene.viewport.xy);
	vec4 tex = vec4(0);
	if (pixel.x >= 0 && pixel.y >= 0) {
		tex = texelFetch (texColor, pixel, 0);
	}
    out_Color = vec4(tex);
}