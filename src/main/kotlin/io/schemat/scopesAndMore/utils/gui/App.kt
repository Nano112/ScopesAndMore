package io.schemat.scopesAndMore.utils.gui

import io.schemat.scopesAndMore.utils.CharacterWidths

interface StyledContent {
    val advance: Int
    fun render(): String
}

data class StyledChar(
    val char: Char,
    val color: String = "",
    override val advance: Int = 0
) : StyledContent {
    override fun render(): String = "$color$char"
}

data class StyledString(
    val text: String,
    val color: String = "",
    override val advance: Int = 0
) : StyledContent {
    override fun render(): String = "$color$text"
}

abstract class BasePanelApp : PanelApp {
    protected fun buildPanel(
        width: Int,
        height: Int,
        config: PanelConfig = PanelConfig(width = width),
        builder: PanelContentBuilder.() -> Unit
    ): String {
        return PanelContentBuilder(width, height).apply {
            builder()
        }.build()
    }
}

class PanelContentBuilder(
    private val width: Int,
    private val height: Int
) {
    private val lines = mutableListOf<MutableList<StyledContent>>()
    val charWidths = CharacterWidths.getInstance()

    companion object {
        private val PAD_SINGLE = StyledString("▏", "§0", 1)
    }

    init {
        repeat(height) {
            lines.add(generatePaddedLine(width))
        }
    }

    private fun generatePaddedLine(neededWidth: Int): MutableList<StyledContent> {
        val line = mutableListOf<StyledContent>()
        repeat(neededWidth) {
            line.add(PAD_SINGLE)
        }
        return line
    }

    fun setContent(content: String, advanceX: Int, y: Int, color: String = "") {
        if (y < 0 || y >= lines.size) return
        val line = lines[y]

        val styledContent = StyledString(
            text = content,
            color = color,
            advance = content.sumOf { charWidths.getCharacterAdvance(it) }
        )

        var currentAdvance = 0
        var insertIndex = 0
        while (insertIndex < line.size && currentAdvance < advanceX) {
            currentAdvance += line[insertIndex].advance
            insertIndex++
        }

        var removeCount = 0
        var removedAdvance = 0
        while (removedAdvance < styledContent.advance && (insertIndex + removeCount) < line.size) {
            removedAdvance += line[insertIndex + removeCount].advance
            removeCount++
        }

        repeat(removeCount) { line.removeAt(insertIndex) }
        line.add(insertIndex, styledContent)

        val currentLineAdvance = line.sumOf { it.advance }
        if (currentLineAdvance < width) {
            line.addAll(generatePaddedLine(width - currentLineAdvance))
        }
    }

    fun setChar(char: Char, advanceX: Int, y: Int, color: String = "") {
        setContent(char.toString(), advanceX, y, color)
    }

    fun getAdvanceAt(x: Int, y: Int): Int {
        if (y < 0 || y >= lines.size) return 0
        val line = lines[y]

        var currentAdvance = 0
        for (content in line) {
            if (currentAdvance >= x) break
            currentAdvance += content.advance
        }
        return currentAdvance
    }

    fun getLineAdvance(y: Int): Int {
        if (y < 0 || y >= lines.size) return 0
        return lines[y].sumOf { it.advance }
    }

    fun build(): String {
        return lines.joinToString("\n") { line ->
            line.joinToString("") { it.render() }
        }
    }
}