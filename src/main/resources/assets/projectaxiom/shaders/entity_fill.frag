#version 450

layout(binding = 1) uniform sampler2D u_Texture;

layout(location = 0) in vec2 v_Uv;
layout(location = 1) in vec4 v_Color;
layout(location = 2) in float v_Param;
layout(location = 0) out vec4 FragColor;

void main() {
    vec4 c = texture(u_Texture, v_Uv);
    float presence = max(c.a, max(max(c.r, c.g), c.b));
    if (presence < 0.05) discard;
    FragColor = v_Color;
}