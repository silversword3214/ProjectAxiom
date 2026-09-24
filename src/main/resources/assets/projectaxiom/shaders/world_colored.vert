#version 450

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;

layout(std140, binding = 0) uniform DynamicTransforms {
    mat4 modelViewProjection;
    vec4 tint;
    vec3 lightDir0;
    mat4 model;
};

layout(location = 0) out vec4 v_Color;

void main() {
    gl_Position = modelViewProjection * vec4(Position, 1.0);
    v_Color = Color;
}