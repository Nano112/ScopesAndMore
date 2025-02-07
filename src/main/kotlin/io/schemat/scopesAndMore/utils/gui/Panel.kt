package io.schemat.scopesAndMore.utils.gui

import io.schemat.scopesAndMore.textBackgroundTransform
import io.schemat.scopesAndMore.utils.heledron.rendering.RenderEntityGroup
import io.schemat.scopesAndMore.utils.heledron.rendering.blockRenderEntity
import io.schemat.scopesAndMore.utils.heledron.rendering.textRenderEntity
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
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

const val cellSize = 0.125f

class Panel(
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
        val fontFile = object {}.javaClass.getResource("/minecraft-unicode.ttf")?.openStream()
        if (fontFile == null) {
            println("Font file not found")
            return 0f
        }
        val font = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(24f)
        val frc = FontRenderContext(null, true, true)

        // Handle spaces specially
        return text.lines().maxOf { line ->
            // Count spaces and non-spaces separately
            val spaceCount = line.count { it == ' ' }
            val nonSpaceText = line.replace(" ", "")

            // Space width is 4 units in Minecraft
            val spaceWidth = spaceCount * 4f

            // Get width of non-space characters using regular font metrics
            val nonSpaceWidth = if (nonSpaceText.isNotEmpty()) {
                TextLayout(nonSpaceText, font, frc).advance
            } else 0f

            spaceWidth + nonSpaceWidth
        }
    }

    fun createRenderGroup(): RenderEntityGroup {
        val group = RenderEntityGroup()
        if (corners[0] == corners[2]) return group

        var (width, height) = calculateDimensions()
        val bottomEdge = corners[1].toVector().subtract(corners[0].toVector()).normalize()
        val upEdge = corners[3].toVector().subtract(corners[0].toVector()).normalize()
        val normal = calculateNormal()

        if (width < 0.1 || height < 0.1) return group

        val content = "AAA"
        val singleSpaceConstant = 16.60546875
        val stringWidth = getStringWidth(content)
        val longestLineWidth = stringWidth / singleSpaceConstant

        Bukkit.broadcastMessage("stringWidth: $stringWidth, longestLineWidth: $longestLineWidth")
        val lineCount = content.lines().size
        group.add("background", textRenderEntity(
            world = corners[0].world!!,
            position = corners[0].toVector(),
            init = {
                it.text = content
                it.backgroundColor = backgroundColor
                it.brightness = Display.Brightness(15, 15)
            },
            update = {
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
                        .scale((width.toFloat() / longestLineWidth).toFloat(), height.toFloat() / lineCount, 1f)
//                        .translate((longestLineWidth / 2f).toFloat(), 0f, 0f)
                        .mul(textBackgroundTransform)
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