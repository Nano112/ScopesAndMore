package io.schemat.scopesAndMore.utils.gui

import io.schemat.scopesAndMore.textBackgroundTransform
import io.schemat.scopesAndMore.utils.CharacterWidths
import io.schemat.scopesAndMore.utils.heledron.rendering.RenderEntityGroup
import io.schemat.scopesAndMore.utils.heledron.rendering.blockRenderEntity
import io.schemat.scopesAndMore.utils.heledron.rendering.textRenderEntity
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.util.Vector
import org.joml.Matrix4f
import org.joml.Vector4f
import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.util.*
import kotlin.math.atan2
import kotlin.math.sqrt


class Panel(
    var corners: List<Location>,
    val id: UUID = UUID.randomUUID(),
    val name: String = "",
    var backgroundColor: Color = Color.fromRGB(0, 0, 0),
    var hoverColor: Color = Color.fromRGB(0, 255, 255),
    var isHovered: Boolean = false,
    var app: PanelApp? = null  // Add app property
) {
    init {
        require(corners.size == 4) { "Panel must be defined by exactly 4 corners" }
        require(corners.all { it.world == corners[0].world }) { "All corners must be in the same world" }
    }

    fun getBasisTransform(): Matrix4f {
        val normal = calculateNormal()
        val worldUp = Vector(0, 1, 0)
        val right = if (Math.abs(normal.dot(worldUp)) > 0.99) {
            normal.getCrossProduct(Vector(0, 0, 1)).normalize()
        } else {
            normal.getCrossProduct(worldUp).normalize()
        }
        val up = normal.getCrossProduct(right).normalize()

        return Matrix4f(
            right.x.toFloat(), up.x.toFloat(), normal.x.toFloat(), 0f,
            right.y.toFloat(), up.y.toFloat(), normal.y.toFloat(), 0f,
            right.z.toFloat(), up.z.toFloat(), normal.z.toFloat(), 0f,
            0f, 0f, 0f, 1f
        )
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

    fun calculateDimensions(): Pair<Double, Double> {
        val width = corners[0].distance(corners[1])
        val height = corners[0].distance(corners[3])
        return Pair(width, height)
    }

    fun getStringWidth(text: String): Float {
        return text.lines().maxOfOrNull { line ->
            if (line.isEmpty()) 0f else {
                // Sum advances for all characters except last one
                val advanceSum = line.dropLast(1).sumOf { char ->
                    CharacterWidths.getInstance().getCharacterAdvance(char)
                }
                // Add just the width of the last character (no advance needed)
                val lastCharWidth = CharacterWidths.getInstance().getCharacterWidth(line.last())

                (advanceSum + lastCharWidth).toFloat()
            }
        } ?: 0f
    }



//    val textBackgroundTransform: Matrix4f; get() = Matrix4f()
//        .translate(-0.1f + .5f,-0.5f + .5f,0f)
//        .scale(8.0f,4.0f,1f) //  + 0.003f  + 0.001f
    fun createTextBackgroundTransform(pixelWidth: Float): Matrix4f {
        val horizontalScaling = 4/pixelWidth
        return Matrix4f()
            .translate(-0.1f * horizontalScaling + .5f,-0.5f + .5f,0f)
            .scale(horizontalScaling*40f, 4f, 1f)
    }



    fun createRenderGroup(): RenderEntityGroup {
        val group = RenderEntityGroup()
        if (corners[0] == corners[2]) return group

        var (width, height) = calculateDimensions()
        val bottomEdge = corners[1].toVector().subtract(corners[0].toVector()).normalize()
        val upEdge = corners[3].toVector().subtract(corners[0].toVector()).normalize()
        val normal = calculateNormal()

        if (width < 0.1 || height < 0.1) return group

        val fontSize = 0.5
        val charWidth = (width / fontSize).toInt()
        val charHeight = (height / fontSize).toInt()


        //if the charWidth or charHeight is smaller than 5, return an empty group
//        if (charWidth < 5 || charHeight < 5) return group


        //6 is the average width of a character
        val advanceWidth = charWidth * 7
        val content = app?.getContent(advanceWidth, charHeight) ?: " "
        val stringWidth = getStringWidth(content)
        val lineCount = content.lines().size
        group.add("background", textRenderEntity(
            world = corners[0].world!!,
            position = corners[0].toVector(),
            init = {
                it.lineWidth = advanceWidth * 80
                it.text = content
                it.alignment = TextDisplay.TextAlignment.LEFT
                it.backgroundColor = backgroundColor
                it.brightness = Display.Brightness(15, 15)
            },
            update = {
                it.text = content
                it.lineWidth = advanceWidth * 80
                val rotationMatrix = Matrix4f(
                    bottomEdge.x.toFloat(), upEdge.x.toFloat(), normal.x.toFloat(), 0f,
                    bottomEdge.y.toFloat(), upEdge.y.toFloat(), normal.y.toFloat(), 0f,
                    bottomEdge.z.toFloat(), upEdge.z.toFloat(), normal.z.toFloat(), 0f,
                    0f, 0f, 0f, 1f
                ).transpose()

                it.backgroundColor = if (isHovered) hoverColor else backgroundColor
                it.setTransformationMatrix(
                    Matrix4f()
                        .mul(rotationMatrix)
                        .scale(width.toFloat(), height.toFloat() / lineCount, 1f)
                        // Recenter the panel coordinate system: move (0,0) to (0.5, 0.5)
//                        .translate(0.5f, 0.5f, 0f)
                        .mul(createTextBackgroundTransform(stringWidth))
                )
                isHovered = false
            }
        ))
        return group
    }



    fun rayIntersectsPanel(rayOrigin: Vector, rayDirection: Vector, maxDistance: Double = 100.0): Double? {
        val normal = calculateNormal()
        val point = corners[0].toVector()
        val denominator = normal.dot(rayDirection)

        if (Math.abs(denominator) < 0.0001) return null

        val t = (normal.dot(point.subtract(rayOrigin))) / denominator
        if (t < 0 || t > maxDistance) return null

        val intersection = rayOrigin.clone().add(rayDirection.clone().multiply(t))
        val bottomEdge = corners[1].toVector().subtract(corners[0].toVector())
        val leftEdge = corners[3].toVector().subtract(corners[0].toVector())
        val relativePoint = intersection.subtract(corners[0].toVector())

        val projBottom = relativePoint.dot(bottomEdge) / bottomEdge.lengthSquared()
        val projLeft = relativePoint.dot(leftEdge) / leftEdge.lengthSquared()

        return if (projBottom in 0.0..1.0 && projLeft in 0.0..1.0) t else null
    }


    fun calculateTransformMatrix(): Matrix4f {
        val bottomEdge = corners[1].toVector().subtract(corners[0].toVector()).normalize()
        val upEdge = corners[3].toVector().subtract(corners[0].toVector()).normalize()
        val normal = calculateNormal()

        return Matrix4f(
            bottomEdge.x.toFloat(), upEdge.x.toFloat(), normal.x.toFloat(), corners[0].x.toFloat(),
            bottomEdge.y.toFloat(), upEdge.y.toFloat(), normal.y.toFloat(), corners[0].y.toFloat(),
            bottomEdge.z.toFloat(), upEdge.z.toFloat(), normal.z.toFloat(), corners[0].z.toFloat(),
            0f, 0f, 0f, 1f
        ).transpose()
    }

    fun localToWorld(localPos: Vector4f): Vector {
        val (width, height) = calculateDimensions()
        val transform = calculateTransformMatrix()

        val scaledPos = Vector4f(
            localPos.x * width.toFloat(),
            localPos.y * height.toFloat(),
            localPos.z,
            1f
        )

        transform.transform(scaledPos)
        return Vector(scaledPos.x.toDouble(), scaledPos.y.toDouble(), scaledPos.z.toDouble())
    }



    companion object {
        fun generatePanelCorners(first: Location, second: Location, flipped: Boolean = false): List<Location> {
            if (!flipped) {
                val corner1 = if (first.y < second.y) first else second
                val corner3 = if (first.y > second.y) first else second
                val height = corner3.y - corner1.y
                val corner2 = corner3.clone().apply { y = corner3.y - height }
                val corner4 = corner1.clone().apply { y = corner1.y + height }
                return listOf(corner1, corner2, corner3, corner4)
            }
            val corner2 = if (first.y < second.y) first else second
            val corner4 = if (first.y > second.y) first else second
            val height = corner4.y - corner2.y
            val corner1 = corner4.clone().apply { y = corner4.y - height }
            val corner3 = corner2.clone().apply { y = corner2.y + height }
            return listOf(corner1, corner2, corner3, corner4)
        }


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