package io.schemat.scopesAndMore.scopes

import io.schemat.scopesAndMore.probe.ProbeUtils
import io.schemat.scopesAndMore.probes.ProbeGroup
import io.schemat.scopesAndMore.textBackgroundTransform
import io.schemat.scopesAndMore.utils.ColorScheme
import io.schemat.scopesAndMore.utils.heledron.RenderEntityGroup
import io.schemat.scopesAndMore.utils.heledron.blockRenderEntity
import io.schemat.scopesAndMore.utils.heledron.interpolateTransform
import io.schemat.scopesAndMore.utils.heledron.textRenderEntity
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.joml.Matrix4f
import java.util.*

data class ValueScopeSettings(
    override val valueScale: Float = 0.3f,
    override val labelScale: Float = 0.15f,
    override val backgroundColor: Color = ColorScheme.background.apply { alpha = 200 }
) : ScopeSettings

class ValueScope(
    id: UUID = UUID.randomUUID(),
    name: String = "",
    location: Location,
    probeGroup: ProbeGroup,
    labelFace: BlockFace = BlockFace.EAST,
    isVisible: Boolean = true,
    override val settings: ValueScopeSettings = ValueScopeSettings()
) : BaseScope(id, name, location, probeGroup, labelFace, isVisible) {

    init {
        // Listen for group config changes
        probeGroup.addConfigUpdateListener {
            notifyConfigUpdate()
        }
    }

    override fun createDisplayGroup(players: List<Player>): RenderEntityGroup {
        val group = RenderEntityGroup()
        if (!isVisible) return group

        // Get face-specific positioning
        val (x, y, z) = ProbeUtils.getFaceOffset(labelFace)
        val rotation = ProbeUtils.getFaceRotation(labelFace)

        // Add background block
        group.add("block", blockRenderEntity(
            world = location.world!!,
            position = location.toVector(),
            init = {
                it.block = Material.GRAY_STAINED_GLASS.createBlockData()
                it.brightness = Display.Brightness(15, 15)
            }
        ))

        // Add the value display
        group.add("value", textRenderEntity(
            world = location.world!!,
            position = location.toVector(),
            init = {
                it.text = probeGroup.getInterpretedValue()
                it.backgroundColor = settings.backgroundColor
                it.brightness = Display.Brightness(15, 15)
            },
            update = {
                it.text = probeGroup.getInterpretedValue()
                val transform = Matrix4f()
                    .rotate(rotation)
                    .translate(x, y, z)
                    .scale(settings.valueScale)
                    .translate(-0.5f, -0.5f, 0.0f)
                it.interpolateTransform(transform.mul(textBackgroundTransform))
            }
        ))

        // Add configuration display
        group.add("config", textRenderEntity(
            world = location.world!!,
            position = location.toVector(),
            init = {
                it.text = buildConfigString()
                it.backgroundColor = settings.backgroundColor
                it.brightness = Display.Brightness(15, 15)
            },
            update = {
                it.text = buildConfigString()
                val transform = Matrix4f()
                    .rotate(rotation)
                    .translate(x, y - 0.3f, z)
                    .scale(settings.labelScale)
                    .translate(-0.5f, -0.5f, 0.0f)
                it.interpolateTransform(transform.mul(textBackgroundTransform))
            }
        ))

        return group
    }

    private fun buildConfigString(): String {
        return "${probeGroup.interpreter.name} | ${probeGroup.signalType} | ${probeGroup.endianness}"
    }
}