package io.schemat.scopesAndMore.probes

import io.schemat.scopesAndMore.probe.Probe
import io.schemat.scopesAndMore.textBackgroundTransform
import io.schemat.scopesAndMore.utils.heledron.*
import org.bukkit.*
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import org.joml.Matrix4f
import java.util.UUID

class ProbeGroup(
    val id: UUID = UUID.randomUUID(),
    var name: String,
    private var isVisible: Boolean = true
) {
    private val probes = mutableListOf<Probe>()
    private var boundingBox: BoundingBox? = null
    private val configUpdateListeners = mutableListOf<() -> Unit>()

    fun addConfigUpdateListener(listener: () -> Unit) {
        configUpdateListeners.add(listener)
    }

    private fun notifyConfigUpdate() {
        configUpdateListeners.forEach { it.invoke() }
    }

    var signalType: SignalType = SignalType.BINARY
        set(value) {
            field = value
            notifyConfigUpdate()
        }

    var endianness: Endianness = Endianness.LITTLE
        set(value) {
            field = value
            notifyConfigUpdate()
        }

    var readOrder: ReadOrder = ReadOrder.MSB_FIRST
        set(value) {
            field = value
            notifyConfigUpdate()
        }

    var interpreter: ValueInterpreter = UnsignedIntInterpreter
        set(value) {
            field = value
            notifyConfigUpdate()
        }

    fun addProbe(probe: Probe) {
        if (!probes.contains(probe)) {
            probes.add(probe)
            updateBoundingBox()
        }
    }

    fun removeProbe(probe: Probe) {
        probes.remove(probe)
        updateBoundingBox()
    }



    fun getProbes(): List<Probe> = probes.toList()  // Return ordered list

    fun getValues(): List<Int> = probes.map { it.getValue() }

    fun getInterpretedValue(): String {
        val values = when (endianness) {
            Endianness.BIG -> getValues()
            Endianness.LITTLE -> getValues().reversed()
        }

        // Convert to bits based on signal type
        val bits = when (signalType) {
            SignalType.BINARY -> values.map { it > 0 }
            SignalType.HEX -> values.flatMap { value ->
                // Generate bits and optionally reverse them based on read order
                val valueBits = (0..3).map { bit -> (value and (1 shl bit)) != 0 }
                when (readOrder) {
                    ReadOrder.MSB_FIRST -> valueBits.reversed()
                    ReadOrder.LSB_FIRST -> valueBits
                }
            }
        }

        if (bits.size < interpreter.minimumBits()) {
            return "Need ${interpreter.minimumBits()} bits, have ${bits.size}"
        }

        // For multi-bit interpreters, we chunk based on the interpreter's requirements
        val processedBits = when (interpreter.minimumBits()) {
            1 -> bits
            else -> bits.chunked(interpreter.minimumBits()).flatten()
        }

        return interpreter.interpret(processedBits)
    }

    private fun updateBoundingBox() {
        if (probes.isEmpty()) {
            boundingBox = null
            return
        }

        var box = BoundingBox.of(
            probes.first().location.toVector(),
            probes.first().location.toVector().add(Vector(1, 1, 1))
        )

        probes.forEach { probe ->
            box = box.union(BoundingBox.of(
                probe.location.toVector(),
                probe.location.toVector().add(Vector(1, 1, 1))
            ))
        }

        // Add padding
        boundingBox = box.expand(0.1)
    }

    fun render(players: List<Player>): RenderEntityGroup {
        if (!isVisible) return RenderEntityGroup()
        val group = RenderEntityGroup()

        boundingBox?.let { box ->
            // Create outline using glass panes
            group.add("outline", blockRenderEntity(
                world = probes.first().location.world!!,
                position = Vector(
                    box.centerX,
                    box.centerY,
                    box.centerZ
                ),
                init = {
                    it.block = Material.LIME_STAINED_GLASS.createBlockData()
                    it.brightness = Display.Brightness(15, 15)
                },
                update = {
                    val transform = Matrix4f()
                        .translate(0f, 0f, 0f)
                        .scale(
                            box.widthX.toFloat(),
                            box.height.toFloat(),
                            box.widthZ.toFloat()
                        )
                        .translate(-0.5f, -0.5f, -0.5f)
                    it.interpolateTransform(transform)
                }
            ))

            // Add group name above the box
            group.add("name", textRenderEntity(
                world = probes.first().location.world!!,
                position = Vector(
                    box.centerX,
                    box.maxY + 0.5,
                    box.centerZ
                ),
                init = {
                    it.text = name
                    it.backgroundColor = Color.fromARGB(100, 0, 0, 0)
                    it.brightness = Display.Brightness(15, 15)
                },
                update = {
                    val transform = Matrix4f()
                        .scale(0.5f)
                        .translate(-0.5f, -0.5f, 0f)
                    it.interpolateTransform(transform.mul(textBackgroundTransform))
                }
            ))
        }

        return group
    }
}

class ProbeGroupManager {
    private val groups = mutableMapOf<UUID, ProbeGroup>()

    fun createGroup(name: String): ProbeGroup {
        // Check for duplicate names
        if (getGroupByName(name) != null) {
            throw IllegalArgumentException("A group with name '$name' already exists")
        }
        val group = ProbeGroup(name = name)
        groups[group.id] = group
        return group
    }

    fun getGroup(idOrName: String): ProbeGroup? {
        // Try UUID first
        try {
            val id = UUID.fromString(idOrName)
            return groups[id]
        } catch (e: IllegalArgumentException) {
            // If not UUID, try name
            return getGroupByName(idOrName)
        }
    }

    private fun getGroupByName(name: String): ProbeGroup? {
        return groups.values.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    fun removeGroup(idOrName: String) {
        val group = getGroup(idOrName) ?: return
        groups.remove(group.id)
    }

    fun getAllGroups(): List<ProbeGroup> = groups.values.toList()

    fun render(players: List<Player>): RenderEntityGroup {
        val masterGroup = RenderEntityGroup()
        groups.values.forEach { group ->
            masterGroup.add("group_${group.id}", group.render(players))
        }
        return masterGroup
    }
}
