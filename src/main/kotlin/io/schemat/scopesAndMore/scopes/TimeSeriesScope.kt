package io.schemat.scopesAndMore.scopes

import io.schemat.scopesAndMore.probe.ProbeUtils
import io.schemat.scopesAndMore.probes.ProbeGroup
import io.schemat.scopesAndMore.textBackgroundTransform
import io.schemat.scopesAndMore.utils.ColorScheme
import io.schemat.scopesAndMore.utils.gui.TextDisplay
import io.schemat.scopesAndMore.utils.heledron.rendering.RenderEntityGroup
import io.schemat.scopesAndMore.utils.heledron.rendering.blockRenderEntity
import io.schemat.scopesAndMore.utils.heledron.rendering.interpolateTransform
import io.schemat.scopesAndMore.utils.heledron.rendering.textRenderEntity
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.joml.Matrix4f
import java.util.*

data class TimeSeriesScopeSettings(
    override val valueScale: Float = 0.05f,
    override val labelScale: Float = 0.15f,
    override val backgroundColor: Color = ColorScheme.background.apply { alpha = 200 },
    val lookbackSeconds: Int = 10,
    val updateIntervalTicks: Int = 2,
    val lineColor: Color = Color.GREEN,
    val gridColor: Color = Color.GRAY,
    val showGrid: Boolean = true
) : ScopeSettings

class TimeSeriesScope(
    id: UUID = UUID.randomUUID(),
    name: String = "",
    location: Location,
    probeGroup: ProbeGroup,
    labelFace: BlockFace = BlockFace.EAST,
    isVisible: Boolean = true,
    override val settings: TimeSeriesScopeSettings = TimeSeriesScopeSettings()
) : BaseScope(id, name, location, probeGroup, labelFace, isVisible) {

    private val values = mutableListOf<Pair<Long, String>>()
    private var lastUpdateTick = 0L

    init {
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

        // Update values if needed
        val currentTick = System.currentTimeMillis()
        if (currentTick - lastUpdateTick >= settings.updateIntervalTicks * 50) {
            values.add(currentTick to probeGroup.getInterpretedValue())
            // Remove old values
            val cutoffTime = currentTick - (settings.lookbackSeconds * 1000)
            values.removeAll { it.first < cutoffTime }
            lastUpdateTick = currentTick
        }

        // Add background block
        group.add("block", blockRenderEntity(
            world = location.world!!,
            position = location.toVector(),
            init = {
                it.block = Material.BLACK_STAINED_GLASS.createBlockData()
                it.brightness = Display.Brightness(15, 15)
            }
        ))

        // Add the current value display
        group.add("value", textRenderEntity(
            world = location.world!!,
            position = location.toVector(),
            init = {
                it.text = buildValueString()
                it.backgroundColor = settings.backgroundColor
                it.brightness = Display.Brightness(15, 15)
            },
            update = {
                it.text = buildValueString()
                val transform = Matrix4f()
                    .rotate(rotation)
                    .translate(x, y, z)
                    .scale(settings.valueScale)
                    .translate(-0.5f, -0.5f, 0.0f)
                it.interpolateTransform(transform.mul(textBackgroundTransform))
            }
        ))

        // Add configuration/stats display
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

    private fun buildValueString(): String {
        val samplesPerSecond = 20 / settings.updateIntervalTicks
        val expectedSamples = (settings.lookbackSeconds * samplesPerSecond).toInt()
        val width = minOf(expectedSamples, 30) // Cap at 30 chars wide
        val numericValues = values.takeLast(width).mapNotNull { it.second.toDoubleOrNull() }
        if (numericValues.isEmpty()) return "No data (${values.size} samples)"



        val config = TextDisplay.Companion.LinePlotConfig(
            useBraille = false,
            interpolate = false,
            color = "§a", // Minecraft green color code
            width = width,
            height = 16,
            maxValue = (numericValues.maxOrNull() ?: 1.0f).toFloat(),
        )

        val currentValue = values.lastOrNull()?.second ?: "No data"
        return buildString {
            append("$currentValue (${values.size} samples)\n\n")
            append(TextDisplay.linePlot(numericValues, config))
        }
    }

    private fun buildConfigString(): String {
        return "${probeGroup.interpreter.name} | ${settings.lookbackSeconds}s history"
    }
}