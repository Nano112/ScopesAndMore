package io.schemat.scopesAndMore.utils.gui

import kotlin.math.abs
import kotlin.math.roundToInt


data class BrailleCharacter(val dots: Array<BooleanArray> = Array(4) { BooleanArray(2) }) {
    fun toBraille(): Char {
        var code = 0x2800  // Braille pattern start
        // Top to bottom, left to right
        if (dots[0][0]) code += 0x1   // ⠁
        if (dots[0][1]) code += 0x8   // ⠈
        if (dots[1][0]) code += 0x2   // ⠂
        if (dots[1][1]) code += 0x10  // ⠐
        if (dots[2][0]) code += 0x4   // ⠄
        if (dots[2][1]) code += 0x20  // ⠠
        if (dots[3][0]) code += 0x40  // ⡀
        if (dots[3][1]) code += 0x80  // ⢀
        return code.toChar()
    }
}
class TextDisplay {
    companion object {
        data class Canvas(
            val width: Int,
            val height: Int,
            private val grid: Array<Array<BrailleCharacter>> = Array(height) {
                Array(width) { BrailleCharacter() }
            }
        ) {
            fun setDot(x: Int, y: Int) {
                val charX = x / 2
                val charY = y / 4
                val dotX = x % 2
                val dotY = y % 4
                if (charX in 0 until width && charY in 0 until height) {
                    grid[charY][charX].dots[dotY][dotX] = true
                }
            }

            override fun toString(): String = buildString {
                grid.forEach { row ->
                    row.forEach { char ->
                        append(char.toBraille())
                    }
                    append('\n')
                }
            }
        }

        data class LinePlotConfig(
            val useBraille: Boolean = false,
            val interpolate: Boolean = false,
            val color: String? = null,
            val width: Int = 30,
            val height: Int = 5,
            val maxValue: Float = 16.0f,
            val minValue: Float = 0.0f
        )

        fun linePlot(values: List<Number>, config: LinePlotConfig = LinePlotConfig()): String {
            return if (config.useBraille) {
                braillePlot(values, config)
            } else {
                simplePlot(values, config)
            }
        }

        private fun braillePlot(values: List<Number>, config: LinePlotConfig): String {
            val canvas = Canvas(config.width, config.height / 4 + 1)
            val min = values.minOf { it.toDouble() }
            val max = values.maxOf { it.toDouble() }
            val range = max - min

            if (config.interpolate) {
                for (i in 0 until values.size - 1) {
                    val x1 = (i.toDouble() / (values.size - 1) * (config.width * 2 - 1)).toInt()
                    val x2 = ((i + 1).toDouble() / (values.size - 1) * (config.width * 2 - 1)).toInt()
                    val y1 = ((values[i].toDouble() - min) / range * (config.height - 1)).toInt()
                    val y2 = ((values[i + 1].toDouble() - min) / range * (config.height - 1)).toInt()
                    drawBrailleLine(canvas, x1, y1, x2, y2)
                }
            } else {
                values.forEachIndexed { x, value ->
                    val y = ((value.toDouble() - min) / range * (config.height - 1)).toInt()
                    canvas.setDot(x, y)
                }
            }

            val result = canvas.toString()
            return config.color?.let { "$it$result§r" } ?: result
        }

        private fun simplePlot(values: List<Number>, config: LinePlotConfig): String {
            val canvas = Array(config.height) { CharArray(config.width) { '\u2003' } }
            val min = values.minOf { it.toDouble() }
            val max = values.maxOf { it.toDouble() }
            val range = max - min

            if (config.interpolate) {
                for (i in 0 until values.size - 1) {
                    val x1 = (i.toDouble() / (values.size - 1) * (config.width - 1)).toInt()
                    val x2 = ((i + 1).toDouble() / (values.size - 1) * (config.width - 1)).toInt()
                    val y1 = ((values[i].toDouble() - min) / range * (config.height - 1)).toInt()
                    val y2 = ((values[i + 1].toDouble() - min) / range * (config.height - 1)).toInt()
                    drawSimpleLine(canvas, x1, y1, x2, y2)
                }
            } else {
                val dx = config.width.toDouble() / values.size  // Space between points


                values.forEachIndexed { i, value ->
                    val x = (i * dx).toInt()
                    val normalizedValue = value.toDouble().coerceIn(config.minValue.toDouble(),
                        config.maxValue.toDouble()
                    )
                    val y = ((normalizedValue - config.minValue) / (config.maxValue - config.minValue) * (config.height - 1)).roundToInt()
                    if (y in canvas.indices && x in canvas[0].indices) {
                        canvas[canvas.size - 1 - y][x] = '█'
                    }
                }
            }

            val result = canvas.joinToString("\n") { it.joinToString("") }
            return config.color?.let { "$it$result§r" } ?: result
        }

        private fun drawBrailleLine(canvas: Canvas, x1: Int, y1: Int, x2: Int, y2: Int) {
            val dx = x2 - x1
            val dy = y2 - y1
            val steps = maxOf(abs(dx), abs(dy))

            for (i in 0..steps) {
                val t = if (steps == 0) 0.0 else i.toDouble() / steps
                val x = (x1 + dx * t).toInt()
                val y = (y1 + dy * t).toInt()
                canvas.setDot(x, y)
            }
        }

        private fun drawSimpleLine(canvas: Array<CharArray>, x1: Int, y1: Int, x2: Int, y2: Int) {
            val dx = x2 - x1
            val dy = y2 - y1
            val steps = maxOf(abs(dx), abs(dy))

            for (i in 0..steps) {
                val t = if (steps == 0) 0.0 else i.toDouble() / steps
                val x = (x1 + dx * t).toInt()
                val y = canvas.size - 1 - (y1 + dy * t).toInt()
                if (y in canvas.indices && x in canvas[0].indices) {
                    canvas[y][x] = '█'
                }
            }
        }
    }
}