#ifdef VULKAN_GLSL
#pragma define "VK_VERTEX_ATTRIBUTES"
#else 
in vec4 in_position; 
in vec4 in_normal; 
in vec4 in_texcoord; 
in vec4 in_color; 
in uvec4 in_blockinfo;
in uvec2 in_light;
#endif