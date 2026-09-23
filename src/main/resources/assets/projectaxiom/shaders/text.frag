#version 450

layout(binding = 0) uniform sampler2D u_Texture;

layout(location = 0) in vec2 v_Uv;
layout(location = 1) in vec4 v_Color;
layout(location = 0) out vec4 FragColor;

void main() {
    float alpha = texture(u_Texture, v_Uv).r;
    FragColor = vec4(v_Color.rgb, v_Color.a * alpha);
}