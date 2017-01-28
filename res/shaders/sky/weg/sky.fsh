#version 150 core


in vec4 color;
in vec3 vposition;

out vec4 out_Color;
out vec4 out_Normal;
out uvec4 out_Material;

// const vec3 fogColor=vec3(0.84f, 0.84f, 0.96f);

const vec3 fogColor=vec3(0.43f, 0.54f, 0.86f);

void main() {
  float dist = length(vposition);
  float fogFactor = clamp( (dist - 30.0f) / 120.0f, 0.0f, 0.7f );
  // fogFactor += clamp( (dist - 20.0f) / 420.0f, 0.0f, 0.06f );
  vec4 skycolor = mix(vec4(fogColor, 1), vec4(1,1.1,1, 1), fogFactor);
  // skycolor.rgb = pow(skycolor.rgb, vec3(1.66f/3.0));
  out_Color = skycolor*1.2;
  out_Normal = vec4(0.5);
  out_Material = uvec4(0);
}
