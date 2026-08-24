#version 330 core

layout(location = 0) in vec2 aPos;   // 2D position
layout(location = 1) in vec2 aUV;    // UV coordinates
layout(location = 2) in vec4 aColor; // RGBA color

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewProjectionMatrix;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMatrix;
};

out vec2 v_UV;
out vec4 v_Color;

void main() {
    gl_Position = ModelViewProjectionMatrix * vec4(aPos.x, aPos.y, 0.0, 1.0);
    // Transform UV by the texture matrix (if needed)
    v_UV = (TextureMatrix * vec4(aUV, 0.0, 1.0)).xy;
    v_Color = aColor * ColorModulator;
}