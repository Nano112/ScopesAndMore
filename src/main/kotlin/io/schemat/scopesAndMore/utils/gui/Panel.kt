package io.schemat.scopesAndMore.utils.gui

import io.schemat.scopesAndMore.textBackgroundTransform
import io.schemat.scopesAndMore.utils.heledron.rendering.RenderEntityGroup
import io.schemat.scopesAndMore.utils.heledron.rendering.blockRenderEntity
import io.schemat.scopesAndMore.utils.heledron.rendering.textRenderEntity
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.joml.Matrix4f
import org.joml.Vector4f
import java.util.*
import kotlin.math.atan2
import kotlin.math.sqrt

const val cellSize = 0.125f

class Panel(
    //        corner 1 will be bottom left
    //        corner 2 will be bottom right
    //        corner 3 will be top right
    //        corner 4 will be top left
    var corners: List<Location>,
    val id: UUID = UUID.randomUUID(),
    val name: String = "",
    var backgroundColor: Color = Color.fromRGB(255, 0, 255),
    var hoverColor: Color = Color.fromRGB(0, 255, 255),
    var isHovered: Boolean = false


    ) {
    init {
        require(corners.size == 4) { "Panel must be defined by exactly 4 corners" }
        require(corners.all { it.world == corners[0].world }) { "All corners must be in the same world" }
    }

    fun calculateCenter(): Vector {
        val sum = corners.fold(Vector(0, 0, 0)) { acc, loc ->
            acc.add(loc.toVector())
        }
        return sum.multiply(1.0 / corners.size)
    }

    fun calculateNormal(): Vector {
        val edge1 = corners[1].toVector().subtract(corners[0].toVector())
        val edge2 = corners[3].toVector().subtract(corners[0].toVector())
        return edge1.getCrossProduct(edge2).normalize()
    }

    fun rayIntersectsPanel(rayOrigin: Vector, rayDirection: Vector, maxDistance: Double = 100.0): Double? {
        val normal = calculateNormal()
        val point = corners[0].toVector()

        // Calculate denominator for intersection check
        val denominator = normal.dot(rayDirection)

        // If denominator is close to 0, ray is parallel to panel
        if (Math.abs(denominator) < 0.0001) {
            return null
        }

        // Calculate distance along ray to intersection point
        val t = (normal.dot(point.subtract(rayOrigin))) / denominator

        // If t is negative or beyond maxDistance, intersection is invalid
        if (t < 0 || t > maxDistance) {
            return null
        }

        // Calculate intersection point
        val intersection = rayOrigin.clone().add(rayDirection.clone().multiply(t))

        // Check if intersection point is within panel bounds
        val bottomEdge = corners[1].toVector().subtract(corners[0].toVector())
        val leftEdge = corners[3].toVector().subtract(corners[0].toVector())
        val relativePoint = intersection.subtract(corners[0].toVector())

        // Project relative point onto edges
        val projBottom = relativePoint.dot(bottomEdge) / bottomEdge.lengthSquared()
        val projLeft = relativePoint.dot(leftEdge) / leftEdge.lengthSquared()

        // Check if projection is within bounds (0 to 1 for both axes)
        return if (projBottom in 0.0..1.0 && projLeft in 0.0..1.0) t else null
    }



    private fun calculateDimensions(): Pair<Double, Double> {
        // Calculate width (distance between corners 1 and 2)
        val width = corners[0].distance(corners[1])

        // Calculate height (distance between corners 1 and 4)
        val height = corners[0].distance(corners[3])

        return Pair(width, height)
    }

    fun createRenderGroup(): RenderEntityGroup {
        val group = RenderEntityGroup()
        if (corners[0] == corners[2]) return group
        // Debug: Render corner markers to verify positions
//        corners.forEachIndexed { index, corner ->
//            group.add("corner_$index", blockRenderEntity(
//                world = corner.world!!,
//                position = corner.toVector(),
//                init = {
//                    it.block = Material.GRAY_STAINED_GLASS.createBlockData()
//                    it.brightness = Display.Brightness(15, 15)
//                },
//                update = {
//                    it.setTransformationMatrix(Matrix4f().scale(0.1f))
//                }
//            ))
//        }

        // Add normal indicator
        val center = calculateCenter()
        val normal = calculateNormal()
//
//        group.add("normal", blockRenderEntity(
//            world = corners[0].world!!,
//            position = center,
//            init = {
//                it.block = Material.GOLD_BLOCK.createBlockData()
//                it.brightness = Display.Brightness(15, 15)
//            },
//            update = {
//                val up = Vector(0, 1, 0)
//                val rotationAxis = up.getCrossProduct(normal).normalize()
//                val angle = up.angle(normal)
//
//                it.setTransformationMatrix(
//                    Matrix4f()
//                        .translate(0f, 0f, 0f)
//                        .rotate(angle.toFloat(), rotationAxis.x.toFloat(), rotationAxis.y.toFloat(), rotationAxis.z.toFloat())
//                        .scale(0.1f, 2.0f, 0.1f)
//                )
//            }
//        ))

        // Add panel background using text entity
        val (width, height) = calculateDimensions()

        // Get the bottom and left edges for correct orientation
        val bottomEdge = corners[1].toVector().subtract(corners[0].toVector()).normalize()
        val upEdge = corners[3].toVector().subtract(corners[0].toVector()).normalize()

        group.add("background", textRenderEntity(
            world = corners[0].world!!,
            position = corners[0].toVector(),
            init = {
                it.text = " "
                it.backgroundColor = backgroundColor  // Light blue color with some transparency
                it.brightness = Display.Brightness(15, 15)
            },
            update = {
                val rotationMatrix = Matrix4f(
                    bottomEdge.x.toFloat(), upEdge.x.toFloat(), normal.x.toFloat(), 0f,
                    bottomEdge.y.toFloat(), upEdge.y.toFloat(), normal.y.toFloat(), 0f,
                    bottomEdge.z.toFloat(), upEdge.z.toFloat(), normal.z.toFloat(), 0f,
                    0f, 0f, 0f, 1f
                ).transpose()

                if (isHovered) {
                    it.backgroundColor = hoverColor
                } else {
                    it.backgroundColor = backgroundColor
                }

                it.setTransformationMatrix(
                    Matrix4f()
                        .mul(rotationMatrix)
                        .scale(width.toFloat(), height.toFloat(), 1f)
                        .mul(textBackgroundTransform)
                )
                isHovered = false
            }
        ))
        return group
    }


}


// Rest of the code (PanelComponent interface and PanelManager class) remains the same
interface PanelComponent {
    fun render(location: Location, scale: Vector)
}

class PanelManager {
    private val panels = mutableMapOf<UUID, Panel>()
    private var activePanel: UUID? = null

    fun addPanel(panel: Panel) {
        panels[panel.id] = panel
        activePanel = panel.id
    }

    fun getActivePanel() = activePanel?.let { panels[it] }

    fun render(players: List<Player>): RenderEntityGroup {
        val group = RenderEntityGroup()
        panels.values.forEach { panel ->
            group.add("panel_${panel.id}", panel.createRenderGroup())
        }
        return group
    }

    fun getFirstPanelForPlayer(player: Player, maxDistance: Double = 100.0): Panel? {
        val eyeLocation = player.eyeLocation
        val direction = eyeLocation.direction

        return panels.values
            .mapNotNull { panel ->
                val distance = panel.rayIntersectsPanel(eyeLocation.toVector(), direction, maxDistance)
                if (distance != null) Pair(panel, distance) else null
            }
            .minByOrNull { it.second }
            ?.first
    }
}