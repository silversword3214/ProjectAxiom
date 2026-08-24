#version 330 core

uniform sampler2D u_Scene;

in vec2 v_Uv;
out vec4 FragColor;

void main() {
    FragColor = texture(u_Scene, clamp(v_Uv, vec2(0.0), vec2(1.0)));
}
