#version 150 core


#pragma define "RENDER_WIREFRAME"


in vec4 color;
in vec3 vposition;
in highp vec3 triangle;

out vec4 out_Color;


void main() {

    float dist = length(vposition);
    if (dist > 200)
        discard;
    float fdistscale = 1.0f-clamp((dist - 40.0f) / 120.0f, 0.0f, 1.0f);
    vec3 d = fwidth(triangle)*fdistscale;
    vec3 tdist = smoothstep(vec3(0.0), d*4.0f, triangle);
    float mixF = min(min(tdist.x, tdist.y), tdist.z);
    if (mixF > 0.3)
        discard;
    out_Color = vec4(color.rbg, color.a*fdistscale);
}
