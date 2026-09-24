#version 450

layout(binding = 1) uniform sampler2D u_Texture;

layout(location = 0) in vec2 v_Uv;
layout(location = 1) in vec4 v_Color;
layout(location = 0) out vec4 FragColor;

void main() {
    ivec2 texSize = textureSize(u_Texture, 0);
    vec2 texel = 1.0 / vec2(texSize);

    // Vanilla outline kirjoittaa alfakanavaan 1.0 siellä missä entity on
    float center = texture(u_Texture, v_Uv).a;
    float inside = step(0.1, center);

    float anyInside = 0.0;
    for (int dy = -1; dy <= 1; dy++) {
        for (int dx = -1; dx <= 1; dx++) {
            if (dx == 0 && dy == 0) continue;
            vec2 offset = vec2(float(dx), float(dy)) * texel;
            float n = texture(u_Texture, v_Uv + offset).a;
            anyInside = max(anyInside, step(0.1, n));
        }
    }

    // 1-pikselin ulkoreuna
    float edge = (1.0 - inside) * anyInside;
    if (edge < 0.5) discard;

    FragColor = v_Color;
}