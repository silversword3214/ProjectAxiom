#version 330 core

in vec2 v_TexCoord;
uniform sampler2D u_Texture;
uniform sampler2D u_TextureI;
uniform ImageData {
    vec4 u_Color;
};
out vec4 color;

void main() {
    vec4 mask = texture(u_Texture, v_TexCoord);
    if (mask.a == 0.0) discard;
    color = texture(u_TextureI, v_TexCoord) * u_Color;
}