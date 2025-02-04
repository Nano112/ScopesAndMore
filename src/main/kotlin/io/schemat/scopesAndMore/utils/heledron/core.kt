package io.schemat.scopesAndMore.utils.heledron


import net.md_5.bungee.api.ChatMessageType
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.entity.minecart.CommandMinecart
import org.bukkit.plugin.java.JavaPlugin

lateinit var currentPlugin: JavaPlugin

private var commandBlockMinecart: CommandMinecart? = null
fun runCommandSilently(command: String, location: Location = Bukkit.getWorlds().first().spawnLocation) {
    val server = Bukkit.getServer()

    val commandBlockMinecart = commandBlockMinecart ?: spawnEntity(location, CommandMinecart::class.java) {
        commandBlockMinecart = it
        it.remove()
    }

    server.dispatchCommand(commandBlockMinecart, command)
}

fun Player.sendActionBar(message: String) {
    this.spigot().sendMessage(ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent(message))
}

fun sendDebugMessage(message: String) {
    // send action bar
    Bukkit.getOnlinePlayers().firstOrNull()?.sendActionBar(message)
}