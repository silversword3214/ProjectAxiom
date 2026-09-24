#version 450

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;

layout(std140, binding = 0) uniform DynamicTransforms {
    mat4 modelViewProjection;
    vec4 tint;
    vec3 lightDir0;
    mat4 model;
};

layout(location = 0) out vec2 v_Uv;
layout(location = 1) out vec4 v_Color;
layout(location = 2) out float v_Param;   // ← uusi: Z-koordinaatti

void main() {
    gl_Position = modelViewProjection * vec4(Position.xy, 0.0, 1.0);
    v_Uv = UV0;
    v_Color = Color;
    v_Param = Position.z;   // paksuus tai mikä tahansa parametri
}