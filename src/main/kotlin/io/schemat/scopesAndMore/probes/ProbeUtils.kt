package io.schemat.scopesAndMore.probe

import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.joml.Matrix4f
import org.joml.Quaternionf
import kotlin.math.abs

object ProbeUtils {
    // Get the most appropriate face based on player position relative to block
    fun getProbeface(block: Location, player: Player): BlockFace {
        val playerLoc = player.eyeLocation
        val dx = playerLoc.x - (block.x + 0.5)
        val dy = playerLoc.y - (block.y + 0.5)
        val dz = playerLoc.z - (block.z + 0.5)

        // Find the dominant axis
        return when {
            abs(dx) > abs(dz) -> if (dx > 0) BlockFace.WEST else BlockFace.EAST
            abs(dz) > abs(dx) -> if (dz > 0) BlockFace.SOUTH else BlockFace.NORTH
            abs(dy) > abs(dx) && abs(dy) > abs(dz) -> if (dy > 0) BlockFace.UP else BlockFace.DOWN
            else -> BlockFace.EAST // Default face if no clear dominant direction
        }
    }

    // Get the rotation for a given face
    fun getFaceRotation(face: BlockFace): Quaternionf {
        return when (face) {
            BlockFace.NORTH -> Quaternionf().rotateY(Math.toRadians(180.0).toFloat())
            BlockFace.SOUTH -> Quaternionf()
            BlockFace.EAST -> Quaternionf().rotateY(Math.toRadians(270.0).toFloat())
            BlockFace.WEST -> Quaternionf().rotateY(Math.toRadians(90.0).toFloat())
            BlockFace.UP -> Quaternionf().rotateX(Math.toRadians(-90.0).toFloat())
            BlockFace.DOWN -> Quaternionf().rotateX(Math.toRadians(90.0).toFloat())
            else -> Quaternionf()
        }
    }

    // Get offset vector for a given face
    fun getFaceOffset(face: BlockFace): Triple<Float, Float, Float> {
        return when (face) {
            BlockFace.NORTH -> Triple(-0.5f, 0.5f, 0.01f)
            BlockFace.SOUTH -> Triple(0.5f, 0.5f, 1.01f)
            BlockFace.EAST -> Triple(0.5f, 0.5f, 0.01f)
            BlockFace.WEST -> Triple(-0.5f, 0.5f, 1.01f)
            else -> Triple(0.5f, 0.5f, 0.5f)
        }
    }
}
