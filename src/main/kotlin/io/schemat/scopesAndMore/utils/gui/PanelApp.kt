package io.schemat.scopesAndMore.utils.gui

interface PanelApp   {
    fun getContent(width: Int, height: Int): String
}

enum class PanelColor(val code: String) {
    BLUE("§9"),
    YELLOW("§e"),
    GRAY("§8"),
    AQUA("§b"),
    BLACK("§0"),
    RED("§c")
}

// Border styles available for panels
enum class BorderStyle(
    val topLeft: Char,
    val topRight: Char,
    val bottomLeft: Char,
    val bottomRight: Char,
    val vertical: Char,
    val horizontal: Char
) {
    SINGLE(
        topLeft = '┌',
        topRight = '┐',
        bottomLeft = '└',
        bottomRight = '┘',
        vertical = '│',
        horizontal = '─'
    ),
    DOUBLE(
        topLeft = '╔',
        topRight = '╗',
        bottomLeft = '╚',
        bottomRight = '╝',
        vertical = '║',
        horizontal = '═'
    ),
    ROUNDED(
        topLeft = '╭',
        topRight = '╮',
        bottomLeft = '╰',
        bottomRight = '╯',
        vertical = '│',
        horizontal = '─'
    )
}


enum class TextAlign {
    LEFT, CENTER, RIGHT
}

data class Padding(
    val left: Int = 0,
    val right: Int = 0
)

data class PanelConfig(
    val width: Int,
    val renderWalls: Boolean = true,
    val defaultContentColor: PanelColor = PanelColor.YELLOW,
    val borderStyle: BorderStyle = BorderStyle.DOUBLE
)