package io.schemat.scopesAndMore.listeners

import io.schemat.scopesAndMore.probe.ProbeManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockRedstoneEvent

class RedstoneListener(private val probeManager: ProbeManager) : Listener {

    @EventHandler
    fun onRedstoneChange(event: BlockRedstoneEvent) {
        // Check if any probes are at this location
        probeManager.getAllProbes()
            .filter { it.location == event.block.location }
            .forEach { probe ->
                probe.update(event.block)
            }
    }
}