#pragma once

#if APPLY_COLOR_FILTER

#include <uniforms/global.glsl>

#include <utils/constants.glsl>
#include <utils/color_utils.glsl>

#define COLOR_FILTER_NONE 0
#define COLOR_FILTER_GREYSCALE 1
#define COLOR_FILTER_SEPIA 2
#define COLOR_FILTER_HIGH_CONTRAST 3
#define COLOR_FILTER_CARTOON 4
#define COLOR_FILTER_INVERT 5
#define COLOR_FILTER_BLACK_AND_WHITE 6
#define COLOR_FILTER_CEL_SHADING 7
#define COLOR_FILTER_CLASSIC_HD 8

float perceptualLuminance(vec3 srgb) {
    return linearToSrgb(linearSrgbLuminance(srgbToLinear(srgb)));
}

vec3 applySingleColorFilter(int filterIndex, vec3 srgb) {
    switch (filterIndex) {
        case COLOR_FILTER_GREYSCALE:
            return vec3(perceptualLuminance(srgb));
        case COLOR_FILTER_SEPIA:
            return srgb * mat3(
                0.393, 0.769, 0.189,
                0.349, 0.686, 0.168,
                0.272, 0.534, 0.131
            );
        case COLOR_FILTER_HIGH_CONTRAST: {
            float intensity = perceptualLuminance(srgb);
            float modifier = 2.2;
            return clamp(intensity + (srgb - intensity) * modifier, 0.0, 1.0);
        }
        case COLOR_FILTER_CARTOON: {
            float quantizationLevels = 7.0;
            return floor(srgb * quantizationLevels) / quantizationLevels;
        }
        case COLOR_FILTER_INVERT:
            return 1 - srgb;
        case COLOR_FILTER_BLACK_AND_WHITE:
            return perceptualLuminance(srgb) > 0.4 ? vec3(1) : vec3(0);
        case COLOR_FILTER_CEL_SHADING: {
            float intensity = perceptualLuminance(srgb);
            float quantizationLevels = 8.0;
            float quantizedIntensity = floor(intensity * quantizationLevels + 0.5) / quantizationLevels;
            return clamp(srgb + quantizedIntensity - intensity, 0.0, 1.0);
        }
        case COLOR_FILTER_CLASSIC_HD: {
            // Warm, punchy grade in the style of 2008-2009 RuneScape HD
            float luma = perceptualLuminance(srgb);
            // Vibrance: boost muted colors more than ones that are already saturated
            float maxC = max(srgb.r, max(srgb.g, srgb.b));
            float minC = min(srgb.r, min(srgb.g, srgb.b));
            float sat = maxC - minC;
            srgb = luma + (srgb - luma) * (1.0 + 0.35 * (1.0 - sat));
            // Gentle S-curve for deeper shadows and brighter midtones
            vec3 x = clamp(srgb, 0.0, 1.0);
            srgb = mix(x, x * x * (3.0 - 2.0 * x), 0.3);
            // Split toning: golden highlights, slightly cool shadows
            float hl = smoothstep(0.35, 0.95, luma);
            srgb += vec3(0.045, 0.02, -0.035) * hl;
            srgb += vec3(-0.01, 0.0, 0.02) * (1.0 - hl) * (1.0 - smoothstep(0.0, 0.35, luma));
            return clamp(srgb, 0.0, 1.0);
        }
    }

    return srgb;
}

vec3 applyColorFilter(vec3 srgb) {
    srgb = clamp(srgb, 0.0, 1.0);
    vec3 previous = applySingleColorFilter(uboGlobal.colorFilterPrevious, srgb);
    vec3 current = applySingleColorFilter(uboGlobal.colorFilter, srgb);
    return linearToSrgb(mix(
        srgbToLinear(previous),
        srgbToLinear(current),
        smoothstep(0, 1, uboGlobal.colorFilterFade)
    ));
}
#endif
