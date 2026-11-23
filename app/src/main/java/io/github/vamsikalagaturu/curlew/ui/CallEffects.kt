package io.github.vamsikalagaturu.curlew.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

// Seconds since first composition, advanced once per frame. Read it only in draw lambdas so a frame costs a redraw,
// not a recomposition. withInfiniteAnimationFrameNanos honours the system "animation duration scale" setting.
@Composable
fun rememberFrameTime(): State<Float> = produceState(0f) {
    var start = -1L
    while (true) {
        withInfiniteAnimationFrameNanos { now ->
            if (start < 0) start = now
            value = (now - start) / 1_000_000_000f
        }
    }
}

/**
 * The two places the aurora pools and brightens: the caller's avatar and the answer handle. Centres and radii are in
 * the aurora's own coordinates (root coordinates on the call screen); a strength of 0 turns a pool off.
 */
class AuroraPools {
    var avatar by mutableStateOf(Offset.Zero)
    var avatarRadius by mutableFloatStateOf(0f)
    var avatarStrength by mutableFloatStateOf(0f)
    var handle by mutableStateOf(Offset.Zero)
    var handleRadius by mutableFloatStateOf(0f)
    var handleStrength by mutableFloatStateOf(0f)
    var handleTint by mutableStateOf(Color.Transparent)
}

/** Frosted glass over the aurora: a faint top-lit fill and a hairline rim in [light], so the colours behind show through. */
fun Modifier.glass(shape: Shape, light: Color): Modifier = this
    .background(Brush.verticalGradient(listOf(light.copy(alpha = 0.16f), light.copy(alpha = 0.05f))), shape)
    .border(1.dp, Brush.verticalGradient(listOf(light.copy(alpha = 0.32f), light.copy(alpha = 0.06f))), shape)

/** A soft pool of [color] behind a control, brightest at its centre and gone just past its edge, so it sits in the aurora. */
fun Modifier.softOrb(color: () -> Color, strength: () -> Float): Modifier = drawBehind {
    val r = size.minDimension / 2f * 1.15f
    val s = strength()
    drawCircle(
        Brush.radialGradient(
            0f to color().copy(alpha = 0.24f * s),
            0.65f to color().copy(alpha = 0.12f * s),
            1f to Color.Transparent,
            center = center,
            radius = r,
        ),
        radius = r,
    )
}

// AGSL needs Android 13 (RuntimeShader); older phones get static soft gradients instead.
private val shaders = Build.VERSION.SDK_INT >= 33

/**
 * A slowly flowing aurora in [tint] and [accent] over [base], brighter near the top, that pools into glowing orbs at
 * [pools] in its own flowing texture, so an orb and the sky around it are one surface. [energy] 0..1 sets its pace.
 */
fun Modifier.aurora(
    time: () -> Float,
    energy: () -> Float,
    base: Color,
    tint: () -> Color,
    accent: () -> Color,
    pools: AuroraPools,
): Modifier = if (shaders) {
    auroraShader(time, energy, base, tint, accent, pools)
} else {
    drawBehind { auroraFallback(base, tint()) }
}

private fun DrawScope.auroraFallback(base: Color, tint: Color) {
    drawRect(base)
    drawRect(
        Brush.radialGradient(
            listOf(tint.copy(alpha = 0.18f), Color.Transparent),
            center = Offset(size.width / 2f, size.height * 0.2f),
            radius = size.width * 0.9f,
        ),
    )
}

// RuntimeShader throws on a software canvas, and Android can draw any window in software: a frame where the
// hardware renderer failed to initialise, or a software snapshot of the view. A crash there kills the InCallService.
private fun DrawScope.hardwareAccelerated() = drawContext.canvas.nativeCanvas.isHardwareAccelerated

enum class HaloPattern {
    /** Three wobbling rings that thin out and fade along their length, with sparkles drifting outward. */
    Strands,

    /** Ripples pulsing outward from the edge, with a few lights orbiting it. */
    Ripples,

    /** Sound-wave arcs pulsing out to the right, rippling along their length: a speaker playing. */
    Waves,

    /** Rings drawing inward and fading as they reach the edge: sound pulled in, for mute. */
    Hush,

    /** A steady rim with one soft arc of light circling it slowly: present, but never pulsing. */
    Orbit,
}

/**
 * A liquid [pattern] around a round element, plus a shockwave ring while [burst] runs 0..1. Draws out to [reach]
 * times the element's radius. With [fill], the inside gets its own swirl, for places without an aurora behind them.
 */
fun Modifier.liquidHalo(
    time: () -> Float,
    energy: () -> Float,
    burst: () -> Float,
    tint: () -> Color,
    accent: () -> Color,
    reach: Float = 3f,
    fill: Boolean = false,
    pattern: HaloPattern = HaloPattern.Strands,
): Modifier = if (shaders) {
    haloShader(time, energy, burst, tint, accent, reach, fill, pattern)
} else {
    drawBehind { haloFallback(tint(), energy()) }
}

private fun DrawScope.haloFallback(tint: Color, energy: Float) {
    val inner = size.minDimension / 2f
    val outer = inner * (1.8f + 0.3f * energy)
    drawCircle(
        Brush.radialGradient(
            0f to tint.copy(alpha = 0.45f),
            inner / outer to tint.copy(alpha = 0.3f),
            1f to Color.Transparent,
            center = center,
            radius = outer,
        ),
        radius = outer,
    )
}

@RequiresApi(33)
private fun Modifier.auroraShader(
    time: () -> Float,
    energy: () -> Float,
    base: Color,
    tint: () -> Color,
    accent: () -> Color,
    pools: AuroraPools,
) = drawWithCache {
    val shader = RuntimeShader(AURORA)
    val brush = ShaderBrush(shader)
    onDrawBehind {
        if (!hardwareAccelerated()) return@onDrawBehind auroraFallback(base, tint())
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("time", time())
        shader.setFloatUniform("energy", energy())
        shader.setColorUniform("base", base.toArgb())
        shader.setColorUniform("tint", tint().toArgb())
        shader.setColorUniform("accent", accent().toArgb())
        shader.setFloatUniform("poolA", pools.avatar.x, pools.avatar.y, pools.avatarRadius.coerceAtLeast(1f), pools.avatarStrength)
        shader.setFloatUniform("poolB", pools.handle.x, pools.handle.y, pools.handleRadius.coerceAtLeast(1f), pools.handleStrength)
        shader.setColorUniform("poolBTint", pools.handleTint.toArgb())
        drawRect(brush)
    }
}

@RequiresApi(33)
private fun Modifier.haloShader(
    time: () -> Float,
    energy: () -> Float,
    burst: () -> Float,
    tint: () -> Color,
    accent: () -> Color,
    reach: Float,
    fill: Boolean,
    pattern: HaloPattern,
) = drawWithCache {
    val shader = RuntimeShader(HALO)
    val brush = ShaderBrush(shader)
    val radius = size.minDimension / 2f
    val extent = radius * reach
    onDrawBehind {
        if (!hardwareAccelerated()) return@onDrawBehind haloFallback(tint(), energy())
        shader.setFloatUniform("center", center.x, center.y)
        shader.setFloatUniform("radius", radius)
        shader.setFloatUniform("reach", reach)
        shader.setFloatUniform("fill", if (fill) 1f else 0f)
        shader.setFloatUniform("mode", pattern.ordinal.toFloat())
        shader.setFloatUniform("time", time())
        shader.setFloatUniform("energy", energy())
        shader.setFloatUniform("burst", burst())
        shader.setColorUniform("tint", tint().toArgb())
        shader.setColorUniform("accent", accent().toArgb())
        drawRect(brush, topLeft = Offset(center.x - extent, center.y - extent), size = Size(extent * 2f, extent * 2f))
    }
}

// Domain-warped sine bands: two warps of the plane, then two interfering waves picked out in tint and accent.
// Each pool is a gaussian around its centre, displaced by the same warp so its edge flows with the bands, and
// screen-blended in so it brightens the sky rather than covering it.
private const val AURORA = """
uniform float2 size;
uniform float time;
uniform float energy;
layout(color) uniform half4 base;
layout(color) uniform half4 tint;
layout(color) uniform half4 accent;
uniform float4 poolA;
uniform float4 poolB;
layout(color) uniform half4 poolBTint;

float pool(float2 xy, float4 p, float2 warp) {
    if (p.w <= 0.0) return 0.0;
    float d = length(xy - p.xy + warp * p.z) / p.z;
    return p.w * exp(-d * d * 0.35);
}

half4 main(float2 xy) {
    float2 uv = xy / size.y;
    float t = time * (0.18 + 0.22 * energy);
    float2 q = uv * 2.4;
    q += 0.65 * float2(sin(q.y * 1.7 + t * 1.3), cos(q.x * 1.3 - t * 1.1));
    q += 0.35 * float2(sin(q.y * 3.1 - t * 0.9), cos(q.x * 2.7 + t * 1.2));
    float band = 0.5 + 0.5 * sin(q.x * 1.6 + q.y * 1.1 + t * 1.7);
    float band2 = 0.5 + 0.5 * sin(-q.x * 1.2 + q.y * 2.2 - t * 1.4);
    float top = 1.0 - smoothstep(0.0, 0.8, xy.y / size.y);
    half3 col = base.rgb;
    col = mix(col, tint.rgb, half(band * band * top * (0.30 + 0.15 * energy)));
    col = mix(col, accent.rgb, half(band2 * band2 * top * 0.22));

    float2 warp = 0.35 * float2(sin(q.y * 2.3 + t * 2.0), cos(q.x * 2.1 - t * 1.7));
    float a = pool(xy, poolA, warp);
    float b = pool(xy, poolB, warp);
    half3 glow = mix(tint.rgb, accent.rgb, half(band)) * half(a) + mix(poolBTint.rgb, accent.rgb, half(band2 * 0.5)) * half(b);
    col = col + glow * (1.0 - col) * 0.9;
    return half4(col, 1.0);
}
"""

// Strands: signed distance to a circle whose radius wobbles with three angular sine harmonics; each ring is a thin
// rim whose brightness also varies along the angle, so it reads as drifting strands rather than a closed outline,
// and sparkles are 24 particles on golden-angle rays, each looping outward and fading. Colours are premultiplied.
private const val HALO = """
uniform float2 center;
uniform float radius;
uniform float reach;
uniform float fill;
uniform float mode;
uniform float time;
uniform float energy;
uniform float burst;
layout(color) uniform half4 tint;
layout(color) uniform half4 accent;

float blob(float2 p, float r, float t, float seed, float amp) {
    float a = atan(p.y, p.x);
    float wobble = sin(a * 3.0 + t * 1.3 + seed) * 0.5
        + sin(a * 5.0 - t * 1.7 + seed * 2.0) * 0.3
        + sin(a * 2.0 + t * 0.9 + seed * 3.0) * 0.2;
    return length(p) - (r + wobble * amp);
}

float hash(float n) { return fract(sin(n) * 43758.5453); }

half4 main(float2 xy) {
    float2 p = xy - center;
    float rr = length(p);
    float ang = atan(p.y, p.x);
    float amp = radius * (0.05 + 0.13 * energy);
    float speed = 0.5 + 0.7 * energy;
    half3 col = half3(0.0);
    float alpha = 0.0;

    if (mode > 3.5) {
        // Orbit: the arc's head leads, its tail fades behind it; the head is softened so it has no hard front edge.
        float rim = exp(-abs(rr - radius * 1.08) / (radius * 0.05)) * 0.3;
        float behind = mod(time * 0.9 - ang, 6.2831853);
        float trail = exp(-behind / 1.4) * smoothstep(0.0, 0.25, behind);
        float arc = exp(-abs(rr - radius * 1.1) / (radius * 0.06)) * trail * 0.9;
        col += tint.rgb * half(rim) + mix(accent.rgb, tint.rgb, half(behind / 6.2831853)) * half(arc);
        alpha += rim + arc;
    } else if (mode > 2.5) {
        // Hush: rings start out wide and draw inward, brightening as they close in, gone as they reach the edge.
        for (int i = 0; i < 3; i++) {
            float fi = float(i);
            float phase = fract(time * 0.4 + fi / 3.0);
            float r = radius * (1.9 - 0.85 * phase);
            float a = exp(-abs(rr - r) / (radius * 0.04)) * smoothstep(0.0, 0.35, phase) * (1.0 - phase) * 0.9;
            col += mix(tint.rgb, accent.rgb, half(phase)) * half(a);
            alpha += a;
        }
    } else if (mode > 1.5) {
        // Waves: arcs on the right-hand side only, leaving quickly, each rippling along its length.
        float side = smoothstep(-0.1, 0.7, cos(ang));
        for (int i = 0; i < 4; i++) {
            float fi = float(i);
            float phase = fract(time * 0.9 + fi / 4.0);
            float r = radius * (1.05 + phase * 1.0) + sin(ang * 8.0 - time * 7.0 + fi * 1.5) * radius * 0.03;
            float a = exp(-abs(rr - r) / (radius * 0.035)) * (1.0 - phase) * side * 0.8;
            col += mix(tint.rgb, accent.rgb, half(phase)) * half(a);
            alpha += a;
        }
    } else if (mode > 0.5) {
        // Three ripples leave the edge in turn, growing and fading, each with a slight wobble.
        for (int i = 0; i < 3; i++) {
            float fi = float(i);
            float phase = fract(time * (0.35 + 0.25 * energy) + fi / 3.0);
            float r = radius * (1.02 + phase * 1.0) + sin(ang * 4.0 + time * 2.0 + fi * 2.0) * radius * 0.04 * energy;
            float a = exp(-abs(rr - r) / (radius * 0.045)) * (1.0 - phase) * 0.6;
            half3 c = mix(tint.rgb, accent.rgb, half(phase));
            col += c * half(a);
            alpha += a;
        }
        // Six lights orbit just outside the edge at slightly different speeds and heights.
        for (int k = 0; k < 6; k++) {
            float fk = float(k);
            float orbit = time * (0.8 + 0.12 * fk) + fk * 1.0472;
            float2 pos = float2(cos(orbit), sin(orbit)) * radius * (1.22 + 0.08 * sin(time * 1.3 + fk * 2.0));
            float s = radius * 0.05;
            float2 dp = p - pos;
            float a = exp(-dot(dp, dp) / (s * s)) * (0.4 + 0.6 * energy);
            col += mix(accent.rgb, half3(1.0), 0.5) * half(a);
            alpha += a;
        }
    } else {
        for (int i = 0; i < 3; i++) {
            float fi = float(i);
            float d = blob(p, radius * (1.08 + 0.14 * fi), time * speed * (1.0 + 0.3 * fi), fi * 2.1, amp * (1.0 + 0.5 * fi));
            float strand = 0.25 + 0.75 * (0.5 + 0.5 * sin(ang * (2.0 + fi) + time * (0.7 + 0.3 * fi) + fi * 2.0));
            float a = exp(-abs(d) / (radius * 0.03)) * strand * (0.6 - 0.15 * fi);
            half3 c = mix(tint.rgb, accent.rgb, half(0.5 + 0.5 * sin(time * 0.8 + fi * 1.7 + ang * 2.0)));
            col += c * half(a);
            alpha += a;
        }
        for (int k = 0; k < 24; k++) {
            float fk = float(k);
            float life = fract(time * (0.12 + 0.18 * hash(fk * 12.9898)) + hash(fk * 78.233));
            float dir = fk * 2.399963 + time * 0.08;
            float2 pos = float2(cos(dir), sin(dir)) * radius * (1.2 + life * 1.5);
            float s = radius * (0.025 + 0.02 * hash(fk * 3.7)) * (1.0 - 0.5 * life);
            float2 dp = p - pos;
            float a = exp(-dot(dp, dp) / (s * s)) * (1.0 - life) * energy;
            col += mix(accent.rgb, half3(1.0), 0.6) * half(a);
            alpha += a;
        }
    }

    if (burst > 0.0 && burst < 1.0) {
        float r = radius * (1.0 + burst * 2.0);
        float ring = exp(-abs(rr - r) / (radius * 0.07)) * (1.0 - burst);
        col += tint.rgb * half(ring);
        alpha += ring;
    }

    // Strands fade out as they cross into the element, so its icon stays clear without a hard circular cut, and
    // fade to zero before the edge of the drawn square so its border never shows.
    float keep = smoothstep(radius * 0.75, radius * 1.05, rr) * (1.0 - smoothstep(radius * reach * 0.7, radius * reach, rr));
    float swirl = 0.5 + 0.5 * sin(ang * 2.0 + time * (0.6 + 0.6 * energy) - rr / radius * 3.0);
    float orb = fill * (1.0 - smoothstep(radius * 0.9, radius * 1.05, rr)) * (0.18 + 0.16 * swirl) * (0.6 + 0.4 * energy);
    alpha = clamp(alpha * keep + orb, 0.0, 1.0);
    col = col * half(keep) + mix(tint.rgb, accent.rgb, half(swirl)) * half(orb);
    return half4(min(col, half3(alpha)), half(alpha));
}
"""
