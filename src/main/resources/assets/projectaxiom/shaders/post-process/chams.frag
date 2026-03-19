#version 330 core

in vec2 v_TexCoord;
uniform sampler2D u_Texture;
uniform ChamsData {
    int renderMode;
    int color;      // ei käytetä textured-tilassa
    int throughWalls;
    int padding;
} u_Chams;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(u_Texture, v_TexCoord);
    if (texColor.a == 0.0) discard;

    if (u_Chams.renderMode == 0) {
        // Solid – käytetään väriä
        float a = float((u_Chams.color >> 24) & 0xFF) / 255.0;
        float r = float((u_Chams.color >> 16) & 0xFF) / 255.0;
        float g = float((u_Chams.color >> 8) & 0xFF) / 255.0;
        float b = float(u_Chams.color & 0xFF) / 255.0;
        fragColor = vec4(r, g, b, a);
    } else {
        // Textured – alkuperäinen tekstuuri
        fragColor = texColor;
    }
}