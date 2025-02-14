package io.schemat.scopesAndMore.apps

import io.schemat.scopesAndMore.utils.gui.*
import org.bukkit.entity.Player

class PlayerLocationApp(
    private val player: Player,
    private val borderStyle: BorderStyle = BorderStyle.SINGLE
) : BasePanelApp() {
    override fun getContent(width: Int, height: Int): String = buildPanel(
        width = width,
        height = height,
        config = PanelConfig(width = width)
    ) {
        // Calculate border character advances
        val topLeftAdvance = charWidths.getCharacterAdvance(borderStyle.topLeft)
        val horizontalAdvance = charWidths.getCharacterAdvance(borderStyle.horizontal)
        val verticalAdvance = charWidths.getCharacterAdvance(borderStyle.vertical)

        // Top border
        setChar(borderStyle.topLeft, 0, 0, PanelColor.BLUE.code)
        var currentAdvance = topLeftAdvance

        // Fill top border
        while (currentAdvance + horizontalAdvance < width - horizontalAdvance) {
            setChar(borderStyle.horizontal, currentAdvance, 0, PanelColor.BLUE.code)
            currentAdvance += horizontalAdvance
        }
        setChar(borderStyle.topRight, currentAdvance, 0, PanelColor.BLUE.code)

        // Side borders
        val rightBorderX = currentAdvance - horizontalAdvance
        for (i in 1 until height - 1) {
            setChar(borderStyle.vertical, 0, i, PanelColor.BLUE.code)
            setChar(borderStyle.vertical, rightBorderX, i, PanelColor.BLUE.code)
        }

        // Bottom border
        setChar(borderStyle.bottomLeft, 0, height - 1, PanelColor.BLUE.code)
        currentAdvance = topLeftAdvance
        while (currentAdvance + horizontalAdvance < width - horizontalAdvance) {
            setChar(borderStyle.horizontal, currentAdvance, height - 1, PanelColor.BLUE.code)
            currentAdvance += horizontalAdvance
        }
        setChar(borderStyle.bottomRight, currentAdvance, height - 1, PanelColor.BLUE.code)
    }

    private fun getCardinalDirection(yaw: Float): String {
        val adjustedYaw = (yaw + 180) % 360
        return when {
            adjustedYaw < 45 || adjustedYaw >= 315 -> "NORTH"
            adjustedYaw < 135 -> "EAST"
            adjustedYaw < 225 -> "SOUTH"
            else -> "WEST"
        }
    }
}