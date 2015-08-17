#version 130


attribute vec4 blockinfo;


    
out Data{
    vec3 normal;
    vec4 position;
} vdata;

void main(void){
    vdata.position = gl_Vertex;
    vdata.normal = gl_Normal;
	// vec4 v = gl_Vertex;
	// gl_Position = gl_ModelViewProjectionMatrix * v;
}

// void mai234n() {
// 	normal = normalize(gl_NormalMatrix * gl_Normal);
// 	globalNormal = normalize(gl_Normal);
// 	color = gl_Color;
// 	// color = vec4(0,0,0,1);
// 	// color.r = blockinfo.x/255.0f;
// 	blockTexture = int(blockinfo.x);
// 	vec4 v = gl_Vertex;
// 	vposition = (gl_ModelViewMatrix * v).xyz;
// 	gl_FogFragCoord = gl_Position.z;
// 	gl_Position = gl_ModelViewProjectionMatrix * v;
// }