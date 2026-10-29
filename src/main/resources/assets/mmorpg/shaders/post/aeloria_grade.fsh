#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform GradeConfig {
    float Vignette;
    float Saturation;
    float Contrast;
    float Warmth;
    float Bloom;
};

layout(location = 0) out vec4 fragColor;

vec3 bright(vec2 uv) {
    vec3 c = texture(InSampler, uv).rgb;
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    return c * smoothstep(0.65, 1.0, l);
}

void main() {
    vec3 col = texture(InSampler, texCoord).rgb;

    // Bloom leger : 8 echantillons de zones lumineuses autour du pixel.
    if (Bloom > 0.0) {
        vec2 px = 3.0 / InSize;
        vec3 glow = vec3(0.0);
        glow += bright(texCoord + vec2( px.x, 0.0));
        glow += bright(texCoord + vec2(-px.x, 0.0));
        glow += bright(texCoord + vec2(0.0,  px.y));
        glow += bright(texCoord + vec2(0.0, -px.y));
        glow += bright(texCoord + px);
        glow += bright(texCoord - px);
        glow += bright(texCoord + vec2( px.x, -px.y));
        glow += bright(texCoord + vec2(-px.x,  px.y));
        col += glow * (Bloom / 8.0);
    }

    float luma = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(vec3(luma), col, Saturation);
    col = (col - 0.5) * Contrast + 0.5;
    col *= vec3(1.0 + Warmth, 1.0, 1.0 - Warmth);

    vec2 d = texCoord - 0.5;
    col *= 1.0 - Vignette * smoothstep(0.35, 0.85, length(d) * 1.25);

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
