package io.schemat.scopesAndMore.scopes

import io.schemat.scopesAndMore.probe.ProbeUtils
import io.schemat.scopesAndMore.probes.ProbeGroup
import io.schemat.scopesAndMore.utils.heledron.RenderEntityGroup
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import java.util.*

enum class ScopeType {
    VALUE,
    TIME_SERIES
}

interface Scope {
    val id: UUID
    val name: String
    val location: Location
    val probeGroup: ProbeGroup
    val labelFace: BlockFace
    var isVisible: Boolean
    fun createDisplayGroup(players: List<Player>): RenderEntityGroup
}

// Base settings interface
interface ScopeSettings {
    val valueScale: Float
    val labelScale: Float
    val backgroundColor: Color
}


abstract class BaseScope(
    override val id: UUID = UUID.randomUUID(),
    override val name: String = "",
    override val location: Location,
    override val probeGroup: ProbeGroup,
    override var labelFace: BlockFace = BlockFace.EAST,
    override var isVisible: Boolean = true
) : Scope {
    abstract val settings: ScopeSettings

    protected val configUpdateListeners = mutableListOf<() -> Unit>()

    fun addConfigUpdateListener(listener: () -> Unit) {
        configUpdateListeners.add(listener)
    }

    protected fun notifyConfigUpdate() {
        configUpdateListeners.forEach { it.invoke() }
    }
}


class ScopeManager {
    private val scopes = mutableMapOf<UUID, Scope>()

    fun createScope(name: String, type: ScopeType, location: Location, group: ProbeGroup, player: Player): Scope {
        val face = ProbeUtils.getProbeface(location, player)
        val scope = when(type) {
            ScopeType.VALUE -> ValueScope(name = name, location = location, probeGroup = group, labelFace = face)
            ScopeType.TIME_SERIES -> TimeSeriesScope(name = name, location = location, probeGroup = group, labelFace = face)
        }
        scopes[scope.id] = scope as Scope
        return scope
    }

    fun render(players: List<Player>): RenderEntityGroup {
        val group = RenderEntityGroup()

        scopes.values.forEach { scope ->
            group.add("scope_${scope.id}", scope.createDisplayGroup(players))
        }

        return group
    }

    fun getScope(idOrName: String): Scope? {
        return scopes.values.firstOrNull { it.id.toString() == idOrName || it.name == idOrName }
    }

    fun removeScope(idOrName: String) {
        val scope = getScope(idOrName)
        if (scope != null) {
            scopes.remove(scope.id)
        }
    }

    fun getAllScopes(): List<Scope> = scopes.values.toList()
}
