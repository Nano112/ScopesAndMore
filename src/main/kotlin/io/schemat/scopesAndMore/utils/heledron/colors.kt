package io.schemat.scopesAndMore.utils.heledron


import org.bukkit.Color
import kotlin.math.abs


fun Color.blendAlpha(other: Color): Color {
    val alpha = this.alpha / 255.0
    val otherAlpha = other.alpha / 255.0
    val blendedAlpha = alpha + otherAlpha * (1 - alpha)
    val r = (this.red * alpha + other.red * otherAlpha * (1 - alpha)) / blendedAlpha
    val g = (this.green * alpha + other.green * otherAlpha * (1 - alpha)) / blendedAlpha
    val b = (this.blue * alpha + other.blue * otherAlpha * (1 - alpha)) / blendedAlpha
    return Color.fromARGB((blendedAlpha * 255).toInt(), r.toInt(), g.toInt(), b.toInt())
}

fun Color.lerpRGB(other: Color, t: Double): Color {
    return Color.fromARGB(
        this.alpha.lerpSafely(other.alpha, t),
        this.red.lerpSafely(other.red, t),
        this.green.lerpSafely(other.green, t),
        this.blue.lerpSafely(other.blue, t),
    )
}

fun Color.toHSV(): Triple<Double, Double, Double> {
    val r = red / 255.0
    val g = green / 255.0
    val b = blue / 255.0

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)

    val delta = max - min

    val h = when {
        delta == 0.0 -> 0.0
        max == r -> 60 * (((g - b) / delta) % 6)
        max == g -> 60 * ((b - r) / delta + 2)
        max == b -> 60 * ((r - g) / delta + 4)
        else -> error("Unreachable")
    }

    val s = if (max == 0.0) 0.0 else delta / max
    val v = max

    return Triple(h, s, v)
}

fun List<Pair<Double, Color>>.interpolate(t: Double, lerpFunction: (Color, Color, Double) -> Color): Color {
    val index = this.indexOfLast { it.first <= t }
    if (index == this.size - 1) return this.last().second
    val start = this[index]
    val end = this[index + 1]
//    return start.second.lerp(end.second, (t - start.first) / (end.first - start.first))
    return lerpFunction(start.second, end.second, (t - start.first) / (end.first - start.first))
}

fun List<Pair<Double, Color>>.interpolateRGB(t: Double): Color {
    return interpolate(t) { start, end, fraction -> start.lerpRGB(end, fraction) }
}

fun List<Pair<Double, Color>>.interpolateOkLab(t: Double): Color {
    return interpolate(t) { start, end, fraction -> start.lerpOkLab(end, fraction) }
}

fun Color.lerpOkLab(other: Color, t: Double): Color {
    val start = Oklab.fromRGB(this)
    val end = Oklab.fromRGB(other)
    val result = start.lerp(end, t).toRGB()
    return result
}

//fun Color.hsvLerp(other: Color, t: Double): Color {
//    val (h1, s1, v1) = this.toHSV()
//    val (h2, s2, v2) = other.toHSV()
//
//    val h = h1.lerp(h2, t)
//    val s = s1.lerp(s2, t)
//    val v = v1.lerp(v2, t)
//    val a = this.alpha.toDouble().lerp(other.alpha.toDouble(), t).toInt()
//
//    return hsv(h, s, v).setAlpha(a)
//}

/**
 * Converts an HSV color to RGB.
 * @param h The hue in degrees.
 * @param s The saturation as a percentage.
 * @param v The value as a percentage.
 * @return The RGB color.
 */
fun hsv(h: Double, s: Double, v: Double): Color {
    val c = v * s
    val x = c * (1 - abs((h / 60) % 2 - 1))
    val m = v - c
    val (r, g, b) = when {
        h < 60 -> Triple(c, x, .0)
        h < 120 -> Triple(x, c, .0)
        h < 180 -> Triple(.0, c, x)
        h < 240 -> Triple(.0, x, c)
        h < 300 -> Triple(x, .0, c)
        else -> Triple(c, .0, x)
    }
    return Color.fromRGB(((r + m) * 255).toInt(), ((g + m) * 255).toInt(), ((b + m) * 255).toInt())
}