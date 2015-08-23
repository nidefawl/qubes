#version 150 core

in Data{
    vec3 color;
} gdata;

out vec3 fragment;

void main(){
    fragment = gdata.color;
}