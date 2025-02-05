package io.schemat.scopesAndMore.utils.gui

import io.schemat.scopesAndMore.utils.heledron.rendering.RenderEntityGroup
import io.schemat.scopesAndMore.utils.heledron.rendering.blockRenderEntity
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.joml.Matrix4f

interface PanelWidget {
    val id: String
    val panel: Panel
    val position: Vector
    val selectionRadius: Double  // How close player needs to be to select this widget
    var isHovered: Boolean
    var isSelected: Boolean

    // Called every tick while widget is visible
    fun render(group: RenderEntityGroup)

    // Called when player's raycast intersects within selectionRadius of widget position
    fun onHover()

    // Called when player clicks while hovering
    fun onSelect()

    // Called when widget is deselected (by clicking elsewhere or switching tools)
    fun onDeselect()

    // Optional: Called every tick while widget is selected
    fun onSelectedTick(player: Player) {}
}

// Example implementation for move widget
class PanelMoveWidget(
    override val panel: Panel,
    override val selectionRadius: Double = 0.5
) : PanelWidget {
    override val id = "move_widget"
    override val position get() = panel.calculateCenter()
    override var isSelected = false
    override var isHovered = false
    private var dragStartPosition: Vector? = null
    private var panelStartCorners: List<Location>? = null

    override fun render(group: RenderEntityGroup) {
        val normal = panel.calculateNormal()
        val up = Vector(0, 1, 0)
        val rotationAxis = up.getCrossProduct(normal).normalize()
        val angle = up.angle(normal)
        // Center handle
        group.add("$id.center", blockRenderEntity(
            world = panel.corners[0].world!!,
            position = position,
            init = {
                it.block = when {
                    isSelected -> Material.DIAMOND_BLOCK.createBlockData()
                    isHovered -> Material.IRON_BLOCK.createBlockData()
                    else -> Material.GOLD_BLOCK.createBlockData()
                }
                it.brightness = Display.Brightness(15, 15)
            },
            update = {
                it.setTransformationMatrix(
                    Matrix4f().scale(0.2f).translate(0f, 0f, -1f)
                        .rotate(
                            angle.toFloat(),
                            rotationAxis.x.toFloat(),
                            rotationAxis.y.toFloat(),
                            rotationAxis.z.toFloat()
                        )
                )
                it.block = when {
                    isSelected -> Material.DIAMOND_BLOCK.createBlockData()
                    isHovered -> Material.IRON_BLOCK.createBlockData()
                    else -> Material.GOLD_BLOCK.createBlockData()
                }
            }
        ))

        // Normal direction indicator
//        group.add("$id.normal", blockRenderEntity(
//            world = panel.corners[0].world!!,
//            position = position,
//            init = {
//                it.block = Material.EMERALD_BLOCK.createBlockData()
//                it.brightness = Display.Brightness(15, 15)
//            },
//            update = {
//
//
//                it.setTransformationMatrix(
//                    Matrix4f()
//                        .translate(0f, 0f, 0f)
//                        .rotate(
//                            angle.toFloat(),
//                            rotationAxis.x.toFloat(),
//                            rotationAxis.y.toFloat(),
//                            rotationAxis.z.toFloat()
//                        )
//                        .scale(0.1f, 1.0f, 0.1f)
//                )
//            }
//        ))
    }

    override fun onHover() {
        // Could show tooltip or highlight
    }

    override fun onSelect() {
        isSelected = true
        dragStartPosition = position.clone()
        panelStartCorners = panel.corners.map { it.clone() }
    }

    override fun onDeselect() {
        isSelected = false
        dragStartPosition = null
        panelStartCorners = null
    }

    override fun onSelectedTick(player: Player) {
        if (dragStartPosition == null || panelStartCorners == null) return

        // Calculate movement delta from drag start
        val playerLook = player.location.direction
        val targetPos =
            player.eyeLocation.toVector().add(playerLook.multiply(position.distance(player.eyeLocation.toVector())))
        val delta = targetPos.subtract(dragStartPosition!!)

        // Apply movement to all corners
        panel.corners = panelStartCorners!!.mapIndexed { index, corner ->
            corner.clone().add(delta)
        }
    }
}

// Example implementation for corner resize widget
class PanelCornerWidget(
    override val panel: Panel,
    private val cornerIndex: Int,
    override val selectionRadius: Double = 0.3
) : PanelWidget {
    override val id = "corner_widget_$cornerIndex"
    override val position get() = panel.corners[cornerIndex].toVector()
    override var isSelected = false
    override var isHovered = false
    private var dragStartPosition: Vector? = null

    override fun render(group: RenderEntityGroup) {
        group.add(id, blockRenderEntity(
            world = panel.corners[0].world!!,
            position = position,
            init = {
                it.block = when {
                    isSelected -> Material.DIAMOND_BLOCK.createBlockData()
                    isHovered -> Material.IRON_BLOCK.createBlockData()
                    else -> Material.GRAY_STAINED_GLASS.createBlockData()
                }
                it.brightness = Display.Brightness(15, 15)
            },
            update = {
                it.setTransformationMatrix(Matrix4f().scale(0.15f))
                it.block = when {
                    isSelected -> Material.DIAMOND_BLOCK.createBlockData()
                    isHovered -> Material.IRON_BLOCK.createBlockData()
                    else -> Material.GRAY_STAINED_GLASS.createBlockData()
                }
            }
        ))
    }

    override fun onHover() {}

    override fun onSelect() {
        isSelected = true
        dragStartPosition = position.clone()
    }

    override fun onDeselect() {
        isSelected = false
        dragStartPosition = null
    }

    override fun onSelectedTick(player: Player) {
        if (!isSelected || dragStartPosition == null) return

        val playerLook = player.location.direction
        val targetPos = player.eyeLocation.toVector().add(playerLook.multiply(position.distance(player.eyeLocation.toVector())))
        val targetLocation = targetPos.toLocation(panel.corners[0].world!!)

        // Get diagonal corner index (0→2, 1→3, 2→0, 3→1)
        val diagonalIndex = (cornerIndex + 2) % 4
        val newCorners = Panel.generatePanelCorners(targetLocation, panel.corners[diagonalIndex], cornerIndex%2 == 1)

        // Update panel corners but keep the diagonal corner in place
        panel.corners = panel.corners.mapIndexed { index, corner ->
            if (index == cornerIndex) newCorners[cornerIndex]
            else if (index == diagonalIndex) corner
            else newCorners[index]
        }


    }
}