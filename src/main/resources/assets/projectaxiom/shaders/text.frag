#version 330 core

out vec4 color;

uniform sampler2D u_Texture;

in vec2 v_TexCoord;
in vec4 v_Color;

void main() {

    float alpha = texture(u_Texture, v_TexCoord).r;
    color = vec4(1.0, 1.0, 1.0, alpha) * v_Color;
}
