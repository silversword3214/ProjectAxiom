#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV1;
layout(location = 4) in ivec2 UV2;
layout(location = 5) in vec3 Normal;

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewProjectionMatrix;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMatrix;
};

void main() {
    gl_Position = ModelViewProjectionMatrix * vec4(Position + ModelOffset, 1.0);
}
