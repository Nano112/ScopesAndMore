package io.schemat.scopesAndMore.utils.gui

import io.schemat.scopesAndMore.utils.heledron.CustomItemComponent
import io.schemat.scopesAndMore.utils.heledron.createNamedItem
import io.schemat.scopesAndMore.utils.heledron.customItemRegistry
import io.schemat.scopesAndMore.utils.heledron.onTick
import io.schemat.scopesAndMore.utils.heledron.rendering.RenderEntityGroup
import io.schemat.scopesAndMore.utils.heledron.rendering.SharedEntityRenderer
import io.schemat.scopesAndMore.utils.heledron.rendering.blockRenderEntity
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.joml.Matrix4f

class PanelEditTool(private val panelManager: PanelManager) {
    private var activeWidgets = mutableListOf<PanelWidget>()
    private var selectedWidget: PanelWidget? = null

    init {
        println("Panel tool initialized")
        val tool = CustomItemComponent("panel_edit_tool")
        customItemRegistry += createNamedItem(Material.SHEARS, "Panel Edit Tool").apply {
            tool.attach(this)
        }

        tool.onGestureUse { player, item ->
            // First check if we're hovering over a widget
            val nearestWidget = findNearestWidget(player)

            if (nearestWidget != null) {
                // If hovering over a widget, select/deselect it
                if (selectedWidget == nearestWidget) {
                    selectedWidget?.onDeselect()
                    selectedWidget = null
                } else {
                    selectedWidget?.onDeselect()
                    selectedWidget = nearestWidget
                    nearestWidget.onSelect()
                }
                return@onGestureUse
            }

            // If not hovering over a widget, toggle widget visibility for the panel
            val targetPanel = panelManager.getFirstPanelForPlayer(player)
            if (targetPanel != null) {
                if (activeWidgets.isEmpty()) {
                    // Create widgets for the panel
                    activeWidgets.add(PanelMoveWidget(targetPanel))
                    // Add corner widgets if needed
                    for (i in 0..3) {
                        activeWidgets.add(PanelCornerWidget(targetPanel, i))
                    }
                    println("Panel selected: ${targetPanel.id}")
                    println("Active widgets: ${activeWidgets.map { it.id }}")
                } else {
                    // Clear widgets
                    activeWidgets.clear()
                    selectedWidget?.onDeselect()
                    selectedWidget = null
                }
            }
        }

        tool.onHeldTick { player, item ->
            handleToolHeld(player)
        }
    }

    private fun handleToolHeld(player: Player) {
        val targetPanel = panelManager.getFirstPanelForPlayer(player)
        if (targetPanel != null) {
            targetPanel.isHovered = true
        }

        // Update selected widget
        selectedWidget?.onSelectedTick(player)

        // Render all active widgets
        val group = RenderEntityGroup()
        activeWidgets.forEach { widget ->
            widget.render(group)
        }
        SharedEntityRenderer.render("panel_edit_widgets", group)

        // Update widget hover states
        findNearestWidget(player)?.onHover()
    }

    private fun findNearestWidget(player: Player): PanelWidget? {
        val eyeLocation = player.eyeLocation
        val lookDirection = eyeLocation.direction

        return activeWidgets
            .mapNotNull { widget ->
                // Calculate distance from player's look ray to widget position
                val toWidget = widget.position.clone().subtract(eyeLocation.toVector())
                val projection = toWidget.dot(lookDirection) / lookDirection.lengthSquared()
                val closestPoint = eyeLocation.toVector().add(lookDirection.clone().multiply(projection))
                val distance = closestPoint.distance(widget.position)

                if (distance <= widget.selectionRadius && projection > 0) {
                    Pair(widget, distance)
                } else null
            }
            .minByOrNull { it.second }?.first
    }
}