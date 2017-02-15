#version 150 core


uniform sampler2D tex0;

 
in vec2 inUV;
in float inLodBias;
in vec3 inNormal;
in vec3 inViewVec;
in vec3 inLightVec;

out vec4 out_Color;

void main(void) {
	vec4 color = texture(tex0, inUV, inLodBias);

	vec3 N = normalize(inNormal);
	vec3 L = normalize(inLightVec);
	vec3 V = normalize(inViewVec);
	vec3 R = reflect(-L, N);
	vec3 diffuse = max(dot(N, L), 0.0) * vec3(1.0);
	float specular = pow(max(dot(R, V), 0.0), 16.0) * color.a;

	out_Color = vec4(diffuse * color.rgb + specular, 1.0);	
}