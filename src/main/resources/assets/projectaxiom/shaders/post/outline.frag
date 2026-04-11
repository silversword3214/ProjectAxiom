#version 330 core

uniform sampler2D u_Scene;

layout(std140) uniform OutlineData {
    vec4 u_OutlineColor;
    vec4 u_FillColor;
    vec4 u_Params;
};

in vec2 v_Uv;
out vec4 FragColor;

const int MAX_RADIUS = 8;

float sampleMask(vec2 uv) {
    return texture(u_Scene, clamp(uv, vec2(0.0), vec2(1.0))).r;
}

void main() {
    float center = sampleMask(v_Uv);
    float thickness = clamp(u_Params.z, 0.0, float(MAX_RADIUS));
    vec2 texel = 1.0 / max(u_Params.xy, vec2(1.0));

    bool inside = center > 0.5;
    bool outerEdge = false;
    bool innerEdge = false;

    for (int x = -MAX_RADIUS; x <= MAX_RADIUS; x++) {
        for (int y = -MAX_RADIUS; y <= MAX_RADIUS; y++) {
            vec2 offset = vec2(float(x), float(y));
            if (length(offset) > thickness) continue;

            float mask = sampleMask(v_Uv + offset * texel);
            if (!inside && mask > 0.5) outerEdge = true;
            if (inside && mask < 0.5) innerEdge = true;
        }
    }

    if (outerEdge || innerEdge) {
        FragColor = u_OutlineColor;
        return;
    }

    if (inside && u_Params.w > 0.5) {
        FragColor = u_FillColor;
        return;
    }

    FragColor = vec4(0.0);
}
