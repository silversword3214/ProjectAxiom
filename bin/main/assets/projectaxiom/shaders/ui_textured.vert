#version 330 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUv;
layout(location = 2) in vec4 aColor;

layout(std140) uniform DynamicTransforms {
    mat4 modelViewProjection;
    vec4 tint;
    vec3 lightDir0;
    mat4 model;
};

out vec2 v_Uv;
out vec4 v_Color;

void main() {
    gl_Position = modelViewProjection * vec4(aPos, 1.0);
    v_Uv = aUv;
    v_Color = aColor * tint;
}