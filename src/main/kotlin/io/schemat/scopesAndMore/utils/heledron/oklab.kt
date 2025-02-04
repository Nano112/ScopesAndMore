package io.schemat.scopesAndMore.utils.heledron


import org.bukkit.Color
import kotlin.math.cbrt
import kotlin.math.pow

class Oklab(
    val l: Double,
    val a: Double,
    val b: Double,
    val alpha: Int,
) {
    fun lerp(other: Oklab, t: Double): Oklab {
        return Oklab(
            l = l.lerp(other.l, t),
            a = a.lerp(other.a, t),
            b = b.lerp(other.b, t),
            alpha = (alpha * (1 - t) + other.alpha * t).toInt(),
        )
    }

    companion object {
        fun fromRGB(color: Color): Oklab {
            val r = color.red / 255.0;
            val g = color.green / 255.0;
            val b = color.blue / 255.0;

            val L = cbrt(
                0.41222147079999993 * r + 0.5363325363 * g + 0.0514459929 * b
            )
            val M = cbrt(
                0.2119034981999999 * r + 0.6806995450999999 * g + 0.1073969566 * b
            );
            val S = cbrt(
                0.08830246189999998 * r + 0.2817188376 * g + 0.6299787005000002 * b
            );

            return Oklab(
                l = 0.2104542553 * L + 0.793617785 * M - 0.0040720468 * S,
                a = 1.9779984951 * L - 2.428592205 * M + 0.4505937099 * S,
                b = 0.0259040371 * L + 0.7827717662 * M - 0.808675766 * S,
                alpha = color.alpha,
            )
        }
    }

    fun toRGB(): Color {
        val L = (l * 0.99999999845051981432 +
                0.39633779217376785678 * a +
                0.21580375806075880339 * b).pow(3);
        val M = (l * 1.0000000088817607767 -
                0.1055613423236563494 * a -
                0.063854174771705903402 * b).pow(3);
        val S = (l * 1.0000000546724109177 -
                0.089484182094965759684 * a -
                1.2914855378640917399 * b).pow(3);

        val r = +4.076741661347994 * L -
                3.307711590408193 * M +
                0.230969928729428 * S
        val g = -1.2684380040921763 * L +
                2.6097574006633715 * M -
                0.3413193963102197 * S
        val b = -0.004196086541837188 * L -
                0.7034186144594493 * M +
                1.7076147009309444 * S

        return Color.fromARGB(
            alpha,
            (r.coerceIn(0.0, 1.0) * 255).toInt(),
            (g.coerceIn(0.0, 1.0) * 255).toInt(),
            (b.coerceIn(0.0, 1.0) * 255).toInt(),
        )
    }
}