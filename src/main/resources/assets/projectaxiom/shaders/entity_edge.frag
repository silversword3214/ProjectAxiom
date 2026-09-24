#version 450

layout(binding = 1) uniform sampler2D u_Texture;

layout(location = 0) in vec2 v_Uv;
layout(location = 1) in vec4 v_Color;
layout(location = 2) in float v_Param;
layout(location = 0) out vec4 FragColor;

float presence(vec2 uv) {
    vec4 c = texture(u_Texture, uv);
    return max(c.a, max(max(c.r, c.g), c.b));
}

void main() {
    ivec2 texSize = textureSize(u_Texture, 0);
    vec2 texel = 1.0 / vec2(texSize);

    // Paksuus v_Param (1.0 - 8.0), pyöristettynä kokonaisluvuksi
    int width = int(round(v_Param));
    width = clamp(width, 1, 8);

    float center = step(0.05, presence(v_Uv));

    float anyInside = 0.0;
    for (int dy = -width; dy <= width; dy++) {
        for (int dx = -width; dx <= width; dx++) {
            if (dx == 0 && dy == 0) continue;
            vec2 offset = vec2(float(dx), float(dy)) * texel;
            anyInside = max(anyInside, step(0.05, presence(v_Uv + offset)));
        }
    }

    float edge = (1.0 - center) * anyInside;
    if (edge < 0.5) discard;

    FragColor = v_Color;
}