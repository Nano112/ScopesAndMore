package io.schemat.scopesAndMore.probe

import io.schemat.scopesAndMore.textBackgroundTransform
import io.schemat.scopesAndMore.utils.ui.*
import io.schemat.scopesAndMore.utils.*
import io.schemat.scopesAndMore.utils.heledron.rendering.RenderEntityGroup
import io.schemat.scopesAndMore.utils.heledron.rendering.blockRenderEntity
import io.schemat.scopesAndMore.utils.heledron.rendering.interpolateTransform
import io.schemat.scopesAndMore.utils.heledron.rendering.textRenderEntity
import org.bukkit.*
import org.bukkit.entity.Display
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Lightable
import org.bukkit.block.data.Powerable
import org.bukkit.block.data.type.RedstoneWire
import org.bukkit.entity.Player
import org.joml.Matrix4f
import java.util.UUID

class Probe(
    val id: UUID = UUID.randomUUID(),
    val location: Location,
    var name: String? = null,
    var labelFace: BlockFace = BlockFace.EAST,
    private var isVisible: Boolean = true
) {
    var lastValue: Int = 0
    private var displayGroup: DisplayGroup? = null

    // Current display mode (can be extended for different visualization types)
    var displayMode: DisplayMode = DisplayMode.SIMPLE

    // Customizable display settings
    var displaySettings = ProbeDisplaySettings()

    fun update(block: Block) {
        if (block.type == Material.REDSTONE_WIRE) {
            var dust = block.blockData as RedstoneWire
            lastValue = dust.power
        } else if (block.type == Material.REDSTONE_TORCH || block.type == Material.REDSTONE_WALL_TORCH) {
            var torch = block.blockData as Lightable
            lastValue = if (torch.isLit) 15 else 0
        } else if (block.type == Material.REDSTONE_BLOCK) {
            lastValue = 15
        } else if (block.type == Material.REDSTONE_LAMP) {
            lastValue = if (block.lightLevel > 0) 15 else 0
        } else if (block.blockData is Powerable) {
            lastValue =if( (block.blockData as Powerable).isPowered ) 15 else 0
        } else {
            lastValue = 0
        }

    }

    fun createDisplayGroup(players: List<Player>): RenderEntityGroup {
        val group = RenderEntityGroup()
        if (!isVisible) return group

        // Get face-specific positioning
        val (x, y, z) = ProbeUtils.getFaceOffset(labelFace)
        val rotation = ProbeUtils.getFaceRotation(labelFace)

        // Add the glass block
        group.add("block", blockRenderEntity(
            world = location.world!!,
            position = location.toVector(),
            init = {
                it.block = if (lastValue > 0)
                    Material.GREEN_STAINED_GLASS.createBlockData()
                else
                    Material.RED_STAINED_GLASS.createBlockData()
                it.brightness = Display.Brightness(15, 15)
            },
            update = {
//                val transform = Matrix4f()
//                    .translate(0.5f, 0.5f, 0.5f)  // Center the block
//                    .scale(1.0f)  // Full block size
//                it.interpolateTransform(transform)
            }
        ))

        // Add the value display on the correct face
        group.add("value", textRenderEntity(
            world = location.world!!,
            position = location.toVector(),
            init = {
                it.text = getHexValue()
                it.backgroundColor = Color.BLACK
                it.brightness = Display.Brightness(15, 15)
            },
            update = {
                it.text = getHexValue()
                val transform = Matrix4f()
                    .rotate(rotation)  // Apply face rotation
                    .translate(x, y, z)  // Position on the face
                    .scale(displaySettings.valueScale)
                    .translate(-0.5f, -0.5f, 0.0f)
                it.interpolateTransform(transform.mul(textBackgroundTransform))
            }
        ))

        // Add name label if present
        name?.let { labelText ->
            group.add("name", textRenderEntity(
                world = location.world!!,
                position = location.toVector(),
                init = {
                    it.text = labelText
                    it.backgroundColor = ColorScheme.background.apply { alpha = 128 }
                    it.brightness = Display.Brightness(15, 15)
                },
                update = {
                    val transform = Matrix4f()
                        .rotate(rotation)  // Apply face rotation
                        .translate(x, y + 0.3f, z)  // Position above value
                        .scale(displaySettings.labelScale)
                        .translate(-0.5f, -0.5f, 0.0f)
                    it.interpolateTransform(transform.mul(textBackgroundTransform))
                }
            ))
        }

        return group
    }


    fun getValue(): Int = lastValue

    fun getHexValue(): String = "${lastValue}"

    fun toggleVisibility() {
        isVisible = !isVisible
        updateDisplay()
    }

    private fun updateDisplay() {
        displayGroup?.let { group ->
            // Update value displays
            group.build() // Trigger a rebuild with new value
        }
    }


}

enum class DisplayMode {
    SIMPLE,
    DETAILED;

    fun next(): DisplayMode = when (this) {
        SIMPLE -> DETAILED
        DETAILED -> SIMPLE
    }

    companion object {
        fun fromString(str: String): DisplayMode? = try {
            valueOf(str.uppercase())
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

data class ProbeDisplaySettings(
    var valueScale: Float = 0.2f,
    var labelScale: Float = 0.1f,
    var interactive: Boolean = true,
    var showName: Boolean = true,
    var backgroundColor: Color = ColorScheme.background.apply { alpha = 200 }
)

// Manager class to handle all probes
class ProbeManager {
    private val probes = mutableMapOf<UUID, Probe>()

    fun createProbe(location: Location, player: Player, name: String? = null): Probe {
        val face = ProbeUtils.getProbeface(location, player)
        val probe = Probe(location = location, name = name, labelFace = face)
        probes[probe.id] = probe
        return probe
    }

    fun getProbe(id: UUID): Probe? = probes[id]

    fun getProbeAt(location: Location): Probe? {
        return probes.values.firstOrNull {
            it.location.blockX == location.blockX &&
                    it.location.blockY == location.blockY &&
                    it.location.blockZ == location.blockZ
        }
    }

    fun removeProbe(id: UUID) = probes.remove(id)

    fun getAllProbes(): List<Probe> = probes.values.toList()

    fun render(players: List<Player>): RenderEntityGroup {
        val group = RenderEntityGroup()

        probes.values.forEach { probe ->
            // Update the probe's value
            probe.update(probe.location.block)

            // Add the probe's render group with a unique identifier
            group.add("probe_${probe.id}", probe.createDisplayGroup(players))
        }

        return group
    }

}