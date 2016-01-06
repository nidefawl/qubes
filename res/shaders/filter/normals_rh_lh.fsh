#version 150 core


#pragma include "tonemap.glsl"
#pragma include "ubo_scene.glsl"

uniform sampler2D texNormals;

in vec2 pass_texcoord;

out vec4 out_Color;

void correct(void) 
{
    vec3 normal = texture(texNormals, pass_texcoord, 0).xyz * 2.0f - 1.0f;
    normal = mat3(in_matrix_3D.view) * vec3(-normal.x, -normal.y, -normal.z);
    normal = mat3(inverse(in_matrix_3D.view)) * vec3(normal.x, -normal.y, -normal.z);
    normal = vec3(-normal.x, normal.y, -normal.z);
    out_Color = vec4( normal*0.5+0.5, 1.0 );
}

void simpler(void) 
{
    vec3 normal = texture(texNormals, pass_texcoord, 0).xyz * 2.0f - 1.0f;
    normal = mat3(in_matrix_3D.view) * normal;
    normal = vec3(normal.x, -normal.y, -normal.z);
    out_Color = vec4( normal*0.5+0.5, 1.0 );
}

void main() {
    simpler();
}