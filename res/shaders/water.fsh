#version 120

varying vec4 forFragColor;
varying vec4 v;

void main() {
  gl_FragColor = vec4(1, clamp(v.y/4.0+0.6, 0, 1),1,1);
}
