package io.schemat.scopesAndMore

import com.sk89q.worldedit.WorldEdit
import io.schemat.scopesAndMore.commands.ProbeCommands
import io.schemat.scopesAndMore.listeners.RedstoneListener
import io.schemat.scopesAndMore.probe.ProbeManager
import io.schemat.scopesAndMore.probes.ProbeGroupManager
import io.schemat.scopesAndMore.scopes.ScopeManager
import io.schemat.scopesAndMore.utils.CharacterWidths
import io.schemat.scopesAndMore.utils.gui.PanelCreationTool
import io.schemat.scopesAndMore.utils.gui.PanelEditTool
import io.schemat.scopesAndMore.utils.gui.PanelManager
import io.schemat.scopesAndMore.utils.heledron.rendering.SharedEntityRenderer
import io.schemat.scopesAndMore.utils.heledron.currentPlugin
import io.schemat.scopesAndMore.utils.heledron.onTick
import io.schemat.scopesAndMore.utils.heledron.openCustomItemInventory
import kotlinx.coroutines.runBlocking
import org.bukkit.entity.Player
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
    private lateinit var panelManager: PanelManager


    // Expose probeManager through a getter
    fun getProbeManager() = probeManager
    fun getProbeGroupManager() = probeGroupManager
    fun getScopeManager() = scopeManager
    fun getPanelManager() = panelManager

    companion object {
        lateinit var instance: ScopesAndMore
            private set
    }



    override fun onEnable() {
        instance = this
        currentPlugin = this
        logger.info("ScopesAndMore enabled, loading configuration")
        saveDefaultConfig()

        CharacterWidths.getInstance().initialize(this)

        val config = config

        // Initialize WorldEdit instance
        worldEditInstance = WorldEdit.getInstance()



        probeManager = ProbeManager()
        probeGroupManager = ProbeGroupManager()
        scopeManager = ScopeManager()
        panelManager = PanelManager()
        PanelCreationTool(panelManager)
        PanelEditTool(panelManager)
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

        getCommand("scope-items")?.setExecutor { sender, _, _, _ ->
            openCustomItemInventory(sender as? Player ?: run {
                sender.sendMessage("Only players can use this command")
                return@setExecutor true
            })
            true
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
            SharedEntityRenderer.render(this to "panels", panelManager.render(players))
        }

        logger.info("Probe system initialized")
    }


    override fun onDisable() {
        logger.info("ScopesAndMore disabled")
    }
}