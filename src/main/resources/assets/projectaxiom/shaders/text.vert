#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV;
layout(location = 2) in vec4 Color;

uniform DynamicTransforms {
    mat4 MVP;
    vec4 Tint;
    vec3 LightDir0;
    mat4 Model;
};

out vec2 texCoord;
out vec4 vertexColor;

void main() {
    texCoord = UV;
    vertexColor = Color;
    gl_Position = MVP * vec4(Position, 1.0);
}