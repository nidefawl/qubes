#version 150 core

uniform sampler2D texSSR;

uniform vec2 _TexelOffsetScale;
const float _DepthBias = 0.305;
const float _NormalBias = 0.29;
const float _BlurQuality = 7.0; //2.0 - 4.0
			
in vec2 pass_texcoord;

out vec4 out_Color;
float weights[8] = float[]( 0.071303, 0.131514, 0.189879, 0.321392, 0.452906,  0.584419, 0.715932, 0.847445 );


void processSample( vec2 uv,
						   float i,
						   float _BlurQuality, //sampleCount
						   vec2 stepSize, 
					  	   inout vec4 accumulator, 
					  	   inout float denominator)
{
	vec2 offsetUV = stepSize * i + uv;
	int weightIdx = int(_BlurQuality - abs( i));
    float coefficient = weights[ weightIdx ];
    accumulator += texture( texSSR, offsetUV, 0) * coefficient;
    denominator += coefficient;
}
void main(void) 
{
    vec4 srcalbedo = texture(texSSR, pass_texcoord, 0);

    vec2 stepSize = _TexelOffsetScale.xy * 0.04;
    vec4 accumulator = srcalbedo * 0.214607;
    float denominator = 0.214607;
	processSample( pass_texcoord, 1, _BlurQuality, stepSize, accumulator, denominator);
	processSample( pass_texcoord, 1 * 0.2, _BlurQuality, stepSize, accumulator, denominator);
	processSample( pass_texcoord, 1 * 0.4, _BlurQuality, stepSize, accumulator, denominator);
	processSample( pass_texcoord, 1 * 0.6, _BlurQuality, stepSize, accumulator, denominator);
	processSample( pass_texcoord, 1 * 0.8, _BlurQuality, stepSize, accumulator, denominator);
	processSample( pass_texcoord, 1 * 1.2, _BlurQuality, stepSize, accumulator, denominator);
	processSample( pass_texcoord, 1 * 1.4, _BlurQuality, stepSize, accumulator, denominator);
	processSample( pass_texcoord, 1 * 1.6, _BlurQuality, stepSize, accumulator, denominator);
	processSample( pass_texcoord, 1 * 1.8, _BlurQuality, stepSize, accumulator, denominator);
	processSample( pass_texcoord, 1 * 2.0, _BlurQuality, stepSize, accumulator, denominator);

	processSample( pass_texcoord, -1, _BlurQuality, stepSize, accumulator, denominator);
	processSample( pass_texcoord, -1 * 0.2, _BlurQuality, stepSize, accumulator, denominator);
	processSample( pass_texcoord, -1 * 0.4, _BlurQuality, stepSize, accumulator, denominator);
	processSample( pass_texcoord, -1 * 0.6, _BlurQuality, stepSize, accumulator, denominator);
	processSample( pass_texcoord, -1 * 0.8, _BlurQuality, stepSize, accumulator, denominator);
	processSample( pass_texcoord, -1 * 1.2, _BlurQuality, stepSize, accumulator, denominator);
	processSample( pass_texcoord, -1 * 1.4, _BlurQuality, stepSize, accumulator, denominator);
	processSample( pass_texcoord, -1 * 1.6, _BlurQuality, stepSize, accumulator, denominator);
	processSample( pass_texcoord, -1 * 1.8, _BlurQuality, stepSize, accumulator, denominator);
	processSample( pass_texcoord, -1 * 2.0, _BlurQuality, stepSize, accumulator, denominator);

	vec4 blurred = accumulator / denominator;
    out_Color = vec4(blurred.rgb, 1.0);
}
