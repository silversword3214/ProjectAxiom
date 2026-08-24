#version 330 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec4 aColor;

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewProjectionMatrix;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMatrix;
};

out vec4 v_Color;

void main() {
    gl_Position = ModelViewProjectionMatrix * vec4(aPos + ModelOffset, 1.0);
    v_Color = aColor * ColorModulator;
}