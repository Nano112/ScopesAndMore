package io.schemat.scopesAndMore

import com.sk89q.worldedit.WorldEdit
import io.schemat.scopesAndMore.commands.ProbeCommands
import io.schemat.scopesAndMore.listeners.RedstoneListener
import io.schemat.scopesAndMore.probe.ProbeManager
import io.schemat.scopesAndMore.probes.ProbeGroupManager
import io.schemat.scopesAndMore.scopes.ScopeManager
import io.schemat.scopesAndMore.utils.heledron.SharedEntityRenderer
import io.schemat.scopesAndMore.utils.heledron.currentPlugin
import io.schemat.scopesAndMore.utils.heledron.onTick
import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.java.JavaPlugin
import org.joml.Matrix4f


val textBackgroundTransform: Matrix4f; get() = Matrix4f()
    .translate(-0.1f + .5f,-0.5f + .5f,0f)
    .scale(8.0f,4.0f,1f) //  + 0.003f  + 0.001f

class ScopesAndMore : JavaPlugin() {

    lateinit var worldEditInstance: WorldEdit
        private set

    private lateinit var probeManager: ProbeManager
    private lateinit var probeGroupManager: ProbeGroupManager
    private lateinit var scopeManager: ScopeManager


    // Expose probeManager through a getter
    fun getProbeManager() = probeManager
    fun getProbeGroupManager() = probeGroupManager
    fun getScopeManager() = scopeManager

    companion object {
        lateinit var instance: ScopesAndMore
            private set
    }


    override fun onEnable() {
        instance = this
        currentPlugin = this
        logger.info("ScopesAndMore enabled, loading configuration")
        saveDefaultConfig()

        val config = config

        // Initialize WorldEdit instance
        worldEditInstance = WorldEdit.getInstance()

        // Initialize probe system
        probeManager = ProbeManager()
        probeGroupManager = ProbeGroupManager()
        scopeManager = ScopeManager()
        setupProbeSystem()

        // Check connection on plugin enable
        runBlocking {
            logger.info("Successfully started")
        }


    }
    private fun setupProbeSystem() {
        // Register commands
        getCommand("probe")?.let { command ->
            val probeCommands = ProbeCommands(probeManager, probeGroupManager, scopeManager)
            command.setExecutor(probeCommands)
            command.tabCompleter = probeCommands
        }

        // Register redstone listener
        server.pluginManager.registerEvents(
            RedstoneListener(probeManager),
            this
        )

        // Set up rendering
        onTick {
            val players = server.onlinePlayers.toList()
            SharedEntityRenderer.render(this to "probes", probeManager.render(players))
            SharedEntityRenderer.render(this to "probe_groups", probeGroupManager.render(players))
            SharedEntityRenderer.render(this to "scopes", scopeManager.render(players))
        }

        logger.info("Probe system initialized")
    }


    override fun onDisable() {
        logger.info("ScopesAndMore disabled")
    }
}