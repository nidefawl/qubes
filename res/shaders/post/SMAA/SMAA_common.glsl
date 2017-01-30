#define SMAA_RT_METRICS float4(1.0 / in_scene.viewport.x, 1.0 / in_scene.viewport.y, in_scene.viewport.x, in_scene.viewport.y)
#define SMAA_GLSL_3 1 
#pragma define "SMAA_QUALITY"
#pragma define "SMAA_PREDICATION"
#pragma define "SMAA_REPROJECTION"
#define SMAA_DEPTH_THRESHOLD (0.01 * SMAA_THRESHOLD)