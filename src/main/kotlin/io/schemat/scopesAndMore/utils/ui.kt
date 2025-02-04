package io.schemat.scopesAndMore.utils

import org.bukkit.Color

enum class ButtonState {
    NORMAL,
    HOVERED
}

enum class MinecraftColor(val code: String, val color: Color) {
    BLACK("§0", Color.fromRGB(0, 0, 0)),
    DARK_BLUE("§1", Color.fromRGB(0, 0, 170)),
    DARK_GREEN("§2", Color.fromRGB(0, 170, 0)),
    DARK_AQUA("§3", Color.fromRGB(0, 170, 170)),
    DARK_RED("§4", Color.fromRGB(170, 0, 0)),
    DARK_PURPLE("§5", Color.fromRGB(170, 0, 170)),
    GOLD("§6", Color.fromRGB(255, 170, 0)),
    GRAY("§7", Color.fromRGB(170, 170, 170)),
    DARK_GRAY("§8", Color.fromRGB(85, 85, 85)),
    BLUE("§9", Color.fromRGB(85, 85, 255)),
    GREEN("§a", Color.fromRGB(85, 255, 85)),
    AQUA("§b", Color.fromRGB(85, 255, 255)),
    RED("§c", Color.fromRGB(255, 85, 85)),
    LIGHT_PURPLE("§d", Color.fromRGB(255, 85, 255)),
    YELLOW("§e", Color.fromRGB(255, 255, 85)),
    WHITE("§f", Color.fromRGB(255, 255, 255));

    companion object {
        fun format(text: String, color: MinecraftColor) = "${color.code}$text"
    }
}

data class ButtonColor(
    val text: MinecraftColor,
    val background: Color
)

data class ButtonStyle(
    val normal: ButtonColor = ButtonColor(MinecraftColor.WHITE, Color.fromRGB(128, 128, 128)),  // Default gray
    val hovered: ButtonColor = ButtonColor(MinecraftColor.RED, Color.fromRGB(255, 255, 255))    // White when hovered
) {
    companion object {
        // Predefined styles
        val DEFAULT = ButtonStyle()

        val PRIMARY = ButtonStyle(
            normal = ButtonColor(MinecraftColor.WHITE, Color.fromRGB(0, 122, 255)),
            hovered = ButtonColor(MinecraftColor.WHITE, Color.fromRGB(0, 142, 255))
        )

        val SUCCESS = ButtonStyle(
            normal = ButtonColor(MinecraftColor.WHITE, Color.fromRGB(52, 199, 89)),
            hovered = ButtonColor(MinecraftColor.WHITE, Color.fromRGB(72, 219, 109))
        )

        val DANGER = ButtonStyle(
            normal = ButtonColor(MinecraftColor.WHITE, Color.fromRGB(255, 59, 48)),
            hovered = ButtonColor(MinecraftColor.WHITE, Color.fromRGB(255, 79, 68))
        )

        val WARNING = ButtonStyle(
            normal = ButtonColor(MinecraftColor.WHITE, Color.fromRGB(255, 149, 0)),
            hovered = ButtonColor(MinecraftColor.WHITE, Color.fromRGB(255, 169, 20))
        )
    }
}

// Helper class for text styling
data class TextStyle(
    val color: MinecraftColor = MinecraftColor.WHITE,
    val background: Color = Color.fromRGB(0, 0, 0),
    val scale: Float = 1.0f
) {
    companion object {
        val DEFAULT = TextStyle()

        val HEADING = TextStyle(
            color = MinecraftColor.WHITE,
            background = Color.fromRGB(40, 40, 40),
            scale = 1.2f
        )

        val SUBHEADING = TextStyle(
            color = MinecraftColor.GRAY,
            background = Color.fromRGB(40, 40, 40),
            scale = 1.0f
        )

        val LABEL = TextStyle(
            color = MinecraftColor.AQUA,
            background = Color.fromRGB(0, 0, 0),
            scale = 0.8f
        )

        val VALUE = TextStyle(
            color = MinecraftColor.YELLOW,
            background = Color.fromRGB(0, 0, 0),
            scale = 0.8f
        )
    }
}

// Helper object for common color combinations
object ColorScheme {
    val background = Color.fromRGB(30, 30, 30)
    val primary = Color.fromRGB(0, 122, 255)
    val success = Color.fromRGB(52, 199, 89)
    val warning = Color.fromRGB(255, 149, 0)
    val danger = Color.fromRGB(255, 59, 48)
    val inactive = Color.fromRGB(128, 128, 128)

    // Creates a darker version of a color
    fun darken(color: Color, factor: Float = 0.8f): Color {
        return Color.fromRGB(
            (color.red * factor).toInt().coerceIn(0, 255),
            (color.green * factor).toInt().coerceIn(0, 255),
            (color.blue * factor).toInt().coerceIn(0, 255)
        )
    }

    // Creates a lighter version of a color
    fun lighten(color: Color, factor: Float = 1.2f): Color {
        return Color.fromRGB(
            (color.red * factor).toInt().coerceIn(0, 255),
            (color.green * factor).toInt().coerceIn(0, 255),
            (color.blue * factor).toInt().coerceIn(0, 255)
        )
    }
}