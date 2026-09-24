#version 450

layout(binding = 1) uniform sampler2D u_Texture;

layout(location = 0) in vec2 v_Uv;
layout(location = 1) in vec4 v_Color;
layout(location = 0) out vec4 FragColor;

float presence(vec2 uv) {
    vec4 c = texture(u_Texture, uv);
    return max(c.a, max(max(c.r, c.g), c.b));
}

void main() {
    ivec2 texSize = textureSize(u_Texture, 0);
    vec2 texel = 1.0 / vec2(texSize);

    float center = step(0.05, presence(v_Uv));

    float anyInside = 0.0;
    for (int dy = -1; dy <= 1; dy++) {
        for (int dx = -1; dx <= 1; dx++) {
            if (dx == 0 && dy == 0) continue;
            vec2 offset = vec2(float(dx), float(dy)) * texel;
            anyInside = max(anyInside, step(0.05, presence(v_Uv + offset)));
        }
    }

    float edge = (1.0 - center) * anyInside;
    if (edge < 0.5) discard;

    FragColor = v_Color;
}