#version 150 core
 
#headerdef


layout(std140) uniform LightInfo {
  vec4 Position; // Light position in eye coords.
  vec4 La; // Ambient light intensity
  vec4 Ld; // Diffuse light intensity
  vec4 Ls; // Specular light intensity
} Light;

layout(std140) uniform MaterialInfo {
  vec4 Ka; // Ambient reflectivity
  vec4 Kd; // Diffuse reflectivity
  vec4 Ks; // Specular reflectivity
  float Shininess; // Specular shininess factor
} Material;

uniform int renderWireFrame;



out vec4 color;
out vec4 lmcoord;
out vec4 texcoord;
out vec3 normal;
out vec3 globalNormal;
out vec3 vposition;
out vec3 bposition;
out vec4 lposition;
out highp vec3 triangle;
out vec4 blockinfo;

void main() {
	texcoord = in_texcoord;
	lmcoord = in_brightness;
	mat3 normMat = mat3(transpose(inverse(in_matrix.mv)));
	normal = normalize(normMat * in_normal.xyz);
	globalNormal = normalize(in_normal.xyz);
	color = in_color;
	// color = vec4(0,0,0,1);
	// color.r = blockinfo.x/255.0f;
	blockinfo = in_blockinfo;
	vposition = (in_matrix.mv * in_position).xyz;
	bposition = in_position.xyz;
	// gl_FogFragCoord = vposition.z;
	gl_Position = in_matrix.mvp * in_position;
	lposition = Light.Position;

	if (renderWireFrame) {
		if (in_blockinfo.y == 0) {
	    	triangle = vec3(0, 0, 255);
		}
		if (in_blockinfo.y == 1) {
	    	triangle = vec3(0, 255, 0);
		}
		if (in_blockinfo.y == 2) {
	    	triangle = vec3(255, 0, 0);
		}
		if (in_blockinfo.y == 3) {
	    	triangle = vec3(0, 0, 255);
		}
	}
}
