#version 450

uniform sampler2D u_Texture;

layout(location = 0) in vec2 v_Uv;
layout(location = 1) in vec4 v_Color;
layout(location = 0) out vec4 FragColor;

void main() {
    FragColor = texture(u_Texture, v_Uv) * v_Color;
}