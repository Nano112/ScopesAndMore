package io.schemat.scopesAndMore.utils.gui

import io.schemat.scopesAndMore.utils.heledron.rendering.RenderEntityGroup
import io.schemat.scopesAndMore.utils.heledron.rendering.blockRenderEntity
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.joml.Matrix4f
import org.joml.Vector4f

interface PanelWidget {
    val id: String
    val panel: Panel
    val position: Vector
    val selectionRadius: Double
    var isHovered: Boolean
    var isSelected: Boolean

    fun render(group: RenderEntityGroup)
    fun onHover()
    fun onSelect()
    fun onDeselect()
    fun onSelectedTick(player: Player) {}
}



abstract class BasePanelWidget(
    override val panel: Panel,
    override val selectionRadius: Double,
    protected val blockSize: Float = 0.15f
) : PanelWidget {
    override var isSelected = false
    override var isHovered = false
    protected var dragStartPosition: Vector? = null
    override fun onSelect() {
        isSelected = true
        dragStartPosition = position.clone()
        onSelectionChanged(true)
    }

    override fun onDeselect() {
        isSelected = false
        dragStartPosition = null
        onSelectionChanged(false)
    }

    protected open fun onSelectionChanged(selected: Boolean) {}

    protected fun calculateBlockOrientation(): Triple<Vector, Vector, Vector> {
        val normal = panel.calculateNormal()
        val worldUp = Vector(0, 1, 0)
        val right = if (Math.abs(normal.dot(worldUp)) > 0.99) {
            normal.getCrossProduct(Vector(0, 0, 1)).normalize()
        } else {
            normal.getCrossProduct(worldUp).normalize()
        }
        val up = normal.getCrossProduct(right).normalize()
        return Triple(right, up, normal)
    }

    override fun onHover() {} // Default empty implementation
}

class PanelMoveWidget(
    panel: Panel,
    selectionRadius: Double = 0.5,
    blockSize: Float = 0.2f
) : BasePanelWidget(panel, selectionRadius, blockSize) {
    override val id = "move_widget"
    override val position: Vector get() = panel.localToWorld(Vector4f(0.5f, 0.5f, 0.25f, 1f))
    private var panelStartCorners: List<Location>? = null


    private fun getBlockPosition(): Vector {
        val width = panel.calculateDimensions().first
        val height = panel.calculateDimensions().second
        if (width.isNaN() || height.isNaN()) return Vector(Double.NaN, Double.NaN, Double.NaN)
        if (width == 0.0 || height == 0.0) return Vector(Double.NaN, Double.NaN, Double.NaN)
        val offset = Vector4f(
            -blockSize/2.0f,
            -blockSize/2.0f,
            -blockSize/2.0f,
            1.0f
        )

        return panel.localToWorld(Vector4f(0.5f, 0.5f, 0.25f, 1f).add(offset))
    }
    override fun render(group: RenderEntityGroup) {
        if (position.x.isNaN() || position.y.isNaN() || position.z.isNaN()) return
        group.add("$id.center", blockRenderEntity(
            world = panel.corners[0].world!!,
            position = getBlockPosition(),
            init = {
                it.block = when {
                    isSelected -> Material.DIAMOND_BLOCK.createBlockData()
                    isHovered -> Material.IRON_BLOCK.createBlockData()
                    else -> Material.GOLD_BLOCK.createBlockData()
                }
                it.brightness = Display.Brightness(15, 15)
            },
            update = {
                it.setTransformationMatrix(panel.getBasisTransform().scale(blockSize))
                it.block = when {
                    isSelected -> Material.DIAMOND_BLOCK.createBlockData()
                    isHovered -> Material.IRON_BLOCK.createBlockData()
                    else -> Material.GOLD_BLOCK.createBlockData()
                }
            }
        ))
    }

    override fun onSelectionChanged(selected: Boolean) {
        panelStartCorners = if (selected) panel.corners.map { it.clone() } else null
    }

    override fun onSelectedTick(player: Player) {
        if (dragStartPosition == null || panelStartCorners == null) return
        val targetPos = calculateDragTarget(player)
        val delta = targetPos.subtract(dragStartPosition!!)
        panel.corners = panelStartCorners!!.map { it.clone().add(delta) }
    }

    private fun calculateDragTarget(player: Player): Vector {
        val playerLook = player.location.direction
        return player.eyeLocation.toVector().add(playerLook.multiply(position.distance(player.eyeLocation.toVector())))
    }
}

class PanelCornerWidget(
    panel: Panel,
    private val cornerIndex: Int,
    selectionRadius: Double = 0.3,
    blockSize: Float = 0.15f
) : BasePanelWidget(panel, selectionRadius, blockSize) {
    override val id = "corner_widget_$cornerIndex"

    override val position: Vector get() {
        val x = if (cornerIndex == 1 || cornerIndex == 2) 1.0f else 0.0f
        val y = if (cornerIndex == 2 || cornerIndex == 3) 1.0f else 0.0f
        return panel.localToWorld(Vector4f(x, y, 0f, 1f))
    }

    private fun getBlockPosition(): Vector{
        val x = if (cornerIndex == 1 || cornerIndex == 2) 1.0f else 0.0f
        val y = if (cornerIndex == 2 || cornerIndex == 3) 1.0f else 0.0f
        val (width, height) = panel.calculateDimensions()
        if (width.isNaN() || height.isNaN()) return Vector(Double.NaN, Double.NaN, Double.NaN)
        if (width == 0.0 || height == 0.0) return Vector(Double.NaN, Double.NaN, Double.NaN)
        val offset = Vector4f(
            if (x < 0.5f) blockSize/width.toFloat() else 0.0f,
            if (y < 0.5f) blockSize/height.toFloat() else 0.0f,
            -blockSize/2.0f,
            1.0f
        )

        return panel.localToWorld(Vector4f(x, y, 0f, 1f).add(offset))
    }

    override fun render(group: RenderEntityGroup) {
        val blockPos = getBlockPosition()
        if (blockPos.x.isNaN() || blockPos.y.isNaN() || blockPos.z.isNaN()) return
        group.add(id, blockRenderEntity(
            world = panel.corners[0].world!!,
            position = blockPos,
            init = {
                it.block = when {
                    isSelected -> Material.DIAMOND_BLOCK.createBlockData()
                    isHovered -> Material.IRON_BLOCK.createBlockData()
                    else -> Material.GRAY_STAINED_GLASS.createBlockData()
                }
                it.brightness = Display.Brightness(15, 15)
            },
            update = {
                it.setTransformationMatrix(panel.getBasisTransform().scale(blockSize))
                it.block = when {
                    isSelected -> Material.DIAMOND_BLOCK.createBlockData()
                    isHovered -> Material.IRON_BLOCK.createBlockData()
                    else -> Material.GRAY_STAINED_GLASS.createBlockData()
                }
            }
        ))
    }



    override fun onSelectionChanged(selected: Boolean) {
        dragStartPosition = if (selected) position.clone() else null
    }



    override fun onSelectedTick(player: Player) {
        if (!isSelected || dragStartPosition == null) return

        val targetPos = calculateDragTarget(player)
        val targetLocation = targetPos.toLocation(panel.corners[0].world!!)
        val diagonalIndex = (cornerIndex + 2) % 4
        val isFlipped = cornerIndex % 2 == 1
        val newCorners = Panel.generatePanelCorners(targetLocation, panel.corners[diagonalIndex], isFlipped)
        val width = newCorners[0].distance(newCorners[1])
        val height = newCorners[0].distance(newCorners[3])
        if (width < 0.2 || height < 0.2) return

        println("Width: $width, Height: $height")
        panel.corners = panel.corners.mapIndexed { index, corner ->
            when (index) {
                cornerIndex -> newCorners[cornerIndex]
                diagonalIndex -> corner
                else -> newCorners[index]
            }
        }
    }

    private fun calculateDragTarget(player: Player): Vector {
        val playerLook = player.location.direction
        return player.eyeLocation.toVector().add(playerLook.multiply(position.distance(player.eyeLocation.toVector())))
    }
}