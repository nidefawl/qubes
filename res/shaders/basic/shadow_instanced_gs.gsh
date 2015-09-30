#version 150
#extension GL_EXT_geometry_shader4 : enable
#extension GL_ARB_viewport_array : enable

#pragma include "ubo_scene.glsl"

layout (lines_adjacency) in;
layout (triangle_strip, max_vertices = 18) out;


in Data{
    vec4 position;
} vdata[4];
// out Data{
//     vec3 color;
// } gdata;



uniform vec4 viewports[4]; // max 4 frustum segments, viewport bounds in pixels, x, y, w, h
uniform vec2 shadowMapSize;

// converts clip coordinates into window coordinates for a given viewport
vec2 getWindowPos(vec4 clip_Pos, uint viewport) {
    vec2 ndc_Pos = (clip_Pos.xy / clip_Pos.w); // -1 to 1
    vec2 blend_factor = (ndc_Pos + 1.0) * 0.5; // 0 to 1
    vec2 view_Pos = (viewports[viewport].zw * blend_factor) + viewports[viewport].xy;
    return view_Pos;
}

// checks if two 2d bounding boxes intersect
bool checkIntersection(vec4 bbox0, vec4 bbox1) {
    bool xmiss = bbox0.x > bbox1.z || bbox0.z < bbox1.x;
    bool ymiss = bbox0.y > bbox1.w || bbox0.w < bbox1.y;
    return !xmiss && !ymiss;
}
const ivec4 vIdx = ivec4(0, 1, 3, 2);
// vec3 vcolors[4] =vec3[](
// 	vec3(1, 0, 0),
// 	vec3(0, 1, 0),
// 	vec3(0, 0, 1),
// 	vec3(1, 1, 0)
//   );

#define CULL_GEOMETRY
void main(void){
#ifdef CULL_GEOMETRY
    vec4 mapBounds = vec4(0, 0, shadowMapSize);
#endif
    vec4 tmp[4];
    for (int segment = 0; segment < 3; ++segment) {
    	for (int i = 0; i < 4; i++) {
    		int idx = vIdx[i];
    		tmp[i] = in_matrix_shadow.shadow_split_mvp[segment] * vdata[idx].position;
    	}
#ifdef CULL_GEOMETRY
        vec2 start_Pos = getWindowPos(tmp[0], segment);
        vec4 primBounds = vec4(start_Pos, start_Pos); // minx, miny, maxx, maxy
        for (int i = 1; i < 4; i++) {
            vec2 window_Pos = getWindowPos(tmp[i], segment);
            primBounds.x = min(primBounds.x, window_Pos.x);
            primBounds.y = min(primBounds.y, window_Pos.y);
            primBounds.z = max(primBounds.x, window_Pos.x);
            primBounds.w = max(primBounds.y, window_Pos.y);
        }
        // we should only emit the primitive if its bounding box intersects the current viewport
        if (checkIntersection(primBounds, mapBounds)) {
#endif
		    for (int i = 0; i < 4; ++i) {
		        gl_Position = tmp[i];
		        gl_ViewportIndex = segment;
		        gl_Layer = segment;
		        // gdata.color = vcolors[i];
		        EmitVertex();
		    }
		    EndPrimitive();
#ifdef CULL_GEOMETRY
        }
#endif
    }
}