#version 120

varying vec4 color;
void main() {

	gl_FragData[0] = vec4(gl_Fog.color.rgb, color.a);
	gl_FragData[1] = vec4(0.0f, 0.0f, 1.0f, 1.0f);
	//gl_FragData[2] = vec4(0.0f, 0.0f, 0.0f, 1.0f);
	//gl_FragData[3] = vec4(0.0f, 0.0f, 0.0f, 1.0f);
}