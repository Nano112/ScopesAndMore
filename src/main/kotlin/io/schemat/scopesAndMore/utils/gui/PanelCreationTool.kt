package io.schemat.scopesAndMore.utils.gui

import io.schemat.scopesAndMore.utils.heledron.*
import io.schemat.scopesAndMore.utils.heledron.rendering.*
import org.bukkit.Material
import org.bukkit.entity.Player
import org.joml.Matrix4f
import org.bukkit.Location
import org.bukkit.entity.Display


class PanelCreationTool(
    private val panelManager: PanelManager,
) {
    private var firstCorner: Location? = null
    private var previewPanel: Panel? = null

    init {
        println("Panel tool initialized")
        val tool = CustomItemComponent("panel_create_tool")
        customItemRegistry += createNamedItem(Material.BLAZE_ROD, "Panel Creation Tool").apply {
            tool.attach(this)
        }

        tool.onGestureUse { player, item ->
            handleToolUse(player)
        }

        tool.onHeldTick { player, item ->
            renderPointer(player)
        }

        onTick {
            renderPreview()
        }
    }

    private fun getPlayerTargetPosition(player: Player): Location {
        val lookingVector = player.location.direction.clone().multiply(3)
        val eyeLocation = player.location.clone().add(0.0, player.eyeHeight, 0.0)
        val targetLocation = eyeLocation.clone().add(lookingVector)

        // Round to nearest cellSize
        return targetLocation.clone().apply {
            x = (Math.round(x / cellSize) * cellSize).toDouble()
            y = (Math.round(y / cellSize) * cellSize).toDouble()
            z = (Math.round(z / cellSize) * cellSize).toDouble()
        }
    }

    private fun generatePanelCorners(first: Location, second: Location): List<Location> {
        //        corner 1 will be bottom left
        //        corner 2 will be bottom right
        //        corner 3 will be top right
        //        corner 4 will be top left
        val corner1 = if (first.y < second.y) first else second
        val corner3 = if (first.y > second.y) first else second
        val height = corner3.y - corner1.y
        val corner2 = corner3.clone().apply {
            y = corner3.y  - height
        }
        val corner4 = corner1.clone().apply {
            y = corner1.y + height
        }
        return listOf(corner1, corner2, corner3, corner4)
    }

    private fun renderPointer(player: Player) {
        val lookingAt = getPlayerTargetPosition(player)
        SharedEntityRenderer.render("panel_tool_pointer", blockRenderEntity(
            world = lookingAt.world!!,
            position = lookingAt.toVector(),
            init = {
                it.block = Material.REDSTONE_BLOCK.createBlockData()
                it.brightness = Display.Brightness(15, 15)
            },
            update = {
                it.setTransformationMatrix(
                    Matrix4f()
                        .scale(0.1f)
                )
            }
        ))

        if (previewPanel != null) {
            previewPanel!!.corners = generatePanelCorners(firstCorner!!, lookingAt)
        }
    }

    private fun handleToolUse(player: Player) {
        val lookingAt = getPlayerTargetPosition(player)

        if (firstCorner == null) {
            firstCorner = lookingAt
            val corners = generatePanelCorners(firstCorner!!, firstCorner!!)
            previewPanel = Panel(
                corners = corners,
                name = "Unnamed panel"
            )
            player.sendMessage("First corner set")
            return
        }

        player.sendMessage("Second corner set")


        panelManager.addPanel(previewPanel!!)
        firstCorner = null
        previewPanel = null
    }

    private fun renderPreview() {
        firstCorner?.let { corner ->
            SharedEntityRenderer.render("panel_preview", blockRenderEntity(
                world = corner.world!!,
                position = corner.toVector(),
                init = {
                    it.block = Material.LIGHT_BLUE_STAINED_GLASS.createBlockData()
                    it.brightness = Display.Brightness(15, 15)
                },
                update = {
                    it.setTransformationMatrix(
                        Matrix4f()
                            .scale(0.1f)
                    )
                }
            ))
        }

        previewPanel?.let { panel ->
            SharedEntityRenderer.render("panel_preview", panel.createRenderGroup())
        }


    }
}