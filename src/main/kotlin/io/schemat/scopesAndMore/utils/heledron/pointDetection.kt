package io.schemat.scopesAndMore.utils.heledron


import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.joml.*
import java.util.WeakHashMap

private fun lineAtZ(point1: Vector3f, point2: Vector3f, z: Float): Vector3f {
    val t = (z - point1.z) / (point2.z - point1.z)
    return Vector3f(point1).lerp(point2, t)
}


private fun lineIntersectsPlane(
    point1: Vector3f,
    point2: Vector3f,
    planeTransform: Matrix4f,
    xRange: ClosedRange<Float>,
    yRange: ClosedRange<Float>,
): Boolean {
    val inverted = Matrix4f(planeTransform).invert()

    val point1Transformed = inverted.transform(point1.toVector4f()).toVector3f()
    val point2Transformed = inverted.transform(point2.toVector4f()).toVector3f()

    val point = lineAtZ(point1Transformed, point2Transformed, 0f)

    return point.y in yRange && point.x in xRange
}

class PlanePointDetector(
    players: List<Player>,
    val displayPosition: Vector,
    val xRange: ClosedRange<Float> = 0f..1f,
    val yRange: ClosedRange<Float> = 0f..1f,
) {
    val points = players.map { player ->
        val location = player.eyeLocation
        val point1 = location.toVector().subtract(displayPosition).toVector3f()
        val point2 = location.toVector().add(location.direction).subtract(displayPosition).toVector3f()
        Triple(player, point1, point2)
    }

    fun lookingAt(planeTransform: Matrix4f) = points.filter { (_, point1, point2) ->
        lineIntersectsPlane(point1, point2, planeTransform, yRange, xRange)
    }.map { (player, _, _) -> player }

    fun detectClick(planeTransform: Matrix4f) = lookingAt(planeTransform).map { player ->
        SharedEntityRenderer.render("point_detector" to player, RenderEntity(
            clazz = Interaction::class.java,
            location = player.location,
            init = {
                it.interactionWidth = player.width.toFloat()
                it.interactionHeight = player.height.toFloat()
                it.scoreboardTags.add("plane_point_detector")
            },
        ))

        ClickDetectionResult(player, didClick[player] ?: false)
    }

    companion object {
        private var didClick = WeakHashMap<Player, Boolean>()

        init {
            onTickEnd {
                didClick.clear()
            }
            onInteractEntity { player, entity, _ ->
                if (!entity.scoreboardTags.contains("plane_point_detector")) return@onInteractEntity
                didClick[player] = true
            }
        }
    }
}

class ClickDetectionResult(
    val player: Player,
    val isClicked: Boolean,
)