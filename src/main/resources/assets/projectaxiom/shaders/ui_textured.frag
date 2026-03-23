#version 330 core

uniform sampler2D u_Texture;

in vec2 v_UV;
in vec4 v_Color;
out vec4 FragColor;

void main() {
    FragColor = texture(u_Texture, v_UV) * v_Color;
}