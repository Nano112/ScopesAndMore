package io.schemat.scopesAndMore.utils

import io.schemat.scopesAndMore.ScopesAndMore

class CharacterWidths private constructor() {
    private val widthMap = mutableMapOf<Char, Int>()
    private val advanceMap = mutableMapOf<Char, Int>()

    fun initialize(plugin: ScopesAndMore) {
        try {
            // Load from plugin's data folder
            val widthsFile = plugin.dataFolder.resolve("character_widths.txt")
            if (!widthsFile.exists()) {
                plugin.saveResource("character_widths.txt", false)
            }

            widthsFile.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (line.length >= 2) {
                        val char = line[0]
                        val width = line.substring(1).toIntOrNull()
                        if (width != null) {
                            widthMap[char] = width
                            // By default, add 1 pixel space after each character
                            advanceMap[char] = width + 1
                        }
                    }
                }
            }


            plugin.logger.info("Character widths and advances loaded successfully")
        } catch (e: Exception) {
            plugin.logger.warning("Error loading character widths: ${e.message}")
        }
    }

    fun getCharacterWidth(char: Char): Int {
        return widthMap.getOrDefault(char, 6)
    }

    fun getCharacterAdvance(char: Char): Int {
        return advanceMap.getOrDefault(char, 7) // Default 6 + 1 spacing
    }

    fun getStringAdvance(str: String): Int {
        return str.sumBy { getCharacterAdvance(it) }
    }


    fun getStringAdvanceNoAnsi(str: String): Int {
        return str.replace(Regex("§[0-9a-f]"), "").sumBy { getCharacterAdvance(it) }
    }

    companion object {
        @JvmStatic
        private var instance: CharacterWidths? = null

        @JvmStatic
        fun getInstance(): CharacterWidths {
            return instance ?: synchronized(this) {
                instance ?: CharacterWidths().also { instance = it }
            }
        }
    }
}