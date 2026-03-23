#version 330 core

uniform sampler2D u_Scene;
uniform sampler2D u_ID;

layout(std140) uniform OutlineData {
    vec4 u_OutlineColor;   // RGBA
    vec4 u_FillColor;      // RGBA
    float u_Thickness;     // in pixels
    float u_GlowStrength;  // blur amount (0–1)
    int u_Mode;            // 0=outline,1=fill,2=both
    int u_BlurRadius;      // radius for glow (0=no blur)
};

in vec2 v_TexCoord;
out vec4 FragColor;

// Simple Gaussian blur (3x3) for glow
vec4 blur3(sampler2D tex, vec2 uv, vec2 texelSize, float strength) {
    vec4 sum = vec4(0.0);
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            sum += texture(tex, uv + vec2(x, y) * texelSize);
        }
    }
    return sum / 9.0;
}

void main() {
    vec2 texelSize = 1.0 / textureSize(u_Scene, 0);
    vec4 scene = texture(u_Scene, v_TexCoord);
    vec4 id = texture(u_ID, v_TexCoord);

    // If no entity ID (black), just output scene
    if (id.rgb == vec3(0.0)) {
        FragColor = scene;
        return;
    }

    // Edge detection: sample neighbors to see if ID changes
    vec4 left   = texture(u_ID, v_TexCoord + vec2(-texelSize.x, 0.0));
    vec4 right  = texture(u_ID, v_TexCoord + vec2( texelSize.x, 0.0));
    vec4 up     = texture(u_ID, v_TexCoord + vec2(0.0, -texelSize.y));
    vec4 down   = texture(u_ID, v_TexCoord + vec2(0.0,  texelSize.y));
    bool isEdge = (id.rgb != left.rgb) || (id.rgb != right.rgb) ||
    (id.rgb != up.rgb)   || (id.rgb != down.rgb);

    vec4 result = scene;

    // Apply fill (if mode includes fill)
    if (u_Mode == 1 || u_Mode == 2) {
        result = mix(scene, u_FillColor, 0.5); // simple overlay, could be more advanced
    }

    // Apply outline (if mode includes outline)
    if ((u_Mode == 0 || u_Mode == 2) && isEdge) {
        // If glow strength > 0, blur the outline
        if (u_GlowStrength > 0.0) {
            vec4 glow = blur3(u_Scene, v_TexCoord, texelSize, u_GlowStrength);
            result = mix(glow, u_OutlineColor, 0.8);
        } else {
            result = u_OutlineColor;
        }
    }

    FragColor = result;
}