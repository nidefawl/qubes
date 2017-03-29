#version 150 core


precision highp float;

layout (set = 0, binding = 0) uniform sampler2D texColor;

#ifdef VULKAN_GLSL
    layout(push_constant) uniform PushConstantsBlurKaw {
      vec3 blurPassProp;
    } pushCBlurKaw;
    #define PASS_PROP pushCBlurKaw.blurPassProp
#else
    uniform vec3 blurPassProp;
    #define PASS_PROP blurPassProp
#endif

in vec2 pass_texcoord;

out vec4 out_Color;

///////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Developed by Masaki Kawase, Bunkasha Games
// Used in DOUBLE-S.T.E.A.L. (aka Wreckless)
// From his GDC2003 Presentation: Frame Buffer Postprocessing Effects in  DOUBLE-S.T.E.A.L (Wreckless)
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
vec3 KawaseBlurFilter( sampler2D tex, vec2 texCoord, vec2 pixelSize, float iteration )
{
    vec2 texCoordSample;
    vec2 halfPixelSize = pixelSize / 2.0f;
    vec2 dUV = ( pixelSize.xy * vec2( iteration, iteration ) ) + halfPixelSize.xy;

    vec3 cOut;

    // Sample top left pixel
    texCoordSample.x = texCoord.x - dUV.x;
    texCoordSample.y = texCoord.y + dUV.y;
    
    cOut = texture( tex, texCoordSample ).xyz;

    // Sample top right pixel
    texCoordSample.x = texCoord.x + dUV.x;
    texCoordSample.y = texCoord.y + dUV.y;

    cOut += texture( tex, texCoordSample ).xyz;

    // Sample bottom right pixel
    texCoordSample.x = texCoord.x + dUV.x;
    texCoordSample.y = texCoord.y - dUV.y;
    cOut += texture( tex, texCoordSample ).xyz;

    // Sample bottom left pixel
    texCoordSample.x = texCoord.x - dUV.x;
    texCoordSample.y = texCoord.y - dUV.y;

    cOut += texture( tex, texCoordSample ).xyz;

    // Average 
    cOut *= 0.25f;
    
    return cOut;
}

void main()
{
    // out_Color = texture(texColor, pass_texcoord);

    out_Color = vec4(KawaseBlurFilter( texColor, pass_texcoord.xy, PASS_PROP.xy, PASS_PROP.z ), 1);

    // // double-Kawase is also an option, but loses some quality
    // FragColor.xyz += KawaseBlurFilter( texColor, pass_texcoord.xy, blurPassProp.xy, blurPassProp.z*2.0 + 1.0 );
    // FragColor.xyz *= 0.5;

    // out_Color.a = 1.0;
}

