#version 150 core


#pragma define "RENDER_WIREFRAME"


in vec3 normal;
in vec4 color;
in vec3 vposition;
in highp vec3 triangle;
in float ftime; 

out vec4 out_Color;
uniform float thickness;

uniform float maxDistance;

float edgeFactor(){
    vec3 d = fwidth(triangle);
    vec3 a3 = smoothstep(vec3(0.0), d*1.5, triangle);
    return min(min(a3.x, a3.y), a3.z);
}

void main() {

    // float min_dist = min(min(triangle.x, triangle.y), triangle.z);
    // float edge = 1.0-smoothstep(fwidth(min_dist), 2 * fwidth(min_dist), min_dist);
    // out_Color = vec4(1,0,1, color.a*edge);

    // float edge = 1.-edgeFactor();
    // out_Color = vec4(triangle, color.a*edge);

    float dist = length(vposition);
    // if (dist > 200)
    //     discard;
    float fdistscale = 1.0f-clamp((dist - maxDistance) / 15.0f, 0.0f, 1.0f);
    vec3 d = fwidth(triangle)*fdistscale;
    vec3 tdist = smoothstep(vec3(0.0), d*2.0f, triangle);
    float mixF = min(min(tdist.x, tdist.y), tdist.z);
    if (mixF > thickness)
        discard;
#ifdef ALTERNATE 
    float fMod = floor(mod(ftime, 3));
    vec3 acolor = vec3(0);
    if (abs(normal.x)+abs(normal.z) < 0.1) {
        if (fMod != 0)
            discard;
        acolor+=vec3(1, 0, 0);
    }
    else if (abs(normal.y)+abs(normal.z) < 0.1) {
        if (fMod != 1)
            discard;
        acolor+=vec3(0, 1, 0);
    }
    else if (abs(normal.x)+abs(normal.y) < 0.1) {
        if (fMod != 2)
            discard;
        acolor+=vec3(0, 0, 1);
    }
#else

    // float fMod = floor(mod(ftime, 3));
    vec3 acolor = vec3(0);
    // if (abs(normal.x)+abs(normal.z) < 0.1) {
    //     // if (fMod != 0)
    //     //     discard;
    //     acolor+=vec3(1, 0, 0);
    // }
    // else if (abs(normal.y)+abs(normal.z) < 0.1) {
    //     // if (fMod != 1)
    //     //     discard;
    //     acolor+=vec3(0, 1, 0);
    // }
    // else if (abs(normal.x)+abs(normal.y) < 0.1) {
    //     // if (fMod != 2)
    //     //     discard;
    //     acolor+=vec3(0, 0, 1);
    // }
        acolor+=color.rgb;
#endif
    out_Color = vec4(vec3(acolor), color.a*fdistscale);
}
