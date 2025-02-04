package io.schemat.scopesAndMore.utils.heledron


import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.io.Closeable

fun addEventListener(listener: Listener): Closeable {
    val plugin = currentPlugin
    plugin.server.pluginManager.registerEvents(listener, plugin)
    return Closeable {
        org.bukkit.event.HandlerList.unregisterAll(listener)
    }
}

fun onInteractEntity(listener: (Player, Entity, EquipmentSlot) -> Unit): Closeable {
    return addEventListener(object : Listener {
        @org.bukkit.event.EventHandler
        fun onInteract(event: org.bukkit.event.player.PlayerInteractEntityEvent) {
            listener(event.player, event.rightClicked, event.hand)
        }
    })
}


fun onInteractEntity(listener: (event: org.bukkit.event.player.PlayerInteractEntityEvent) -> Unit): Closeable {
    return addEventListener(object : Listener {
        @org.bukkit.event.EventHandler
        fun onInteract(event: org.bukkit.event.player.PlayerInteractEntityEvent) {
            listener(event)
        }
    })
}

fun onSpawnEntity(listener: (Entity) -> Unit): Closeable {
    return addEventListener(object : Listener {
        @org.bukkit.event.EventHandler
        fun onSpawn(event: org.bukkit.event.entity.EntitySpawnEvent) {
            listener(event.entity)
        }
    })
}

fun onGestureUseItem(listener: (Player, ItemStack) -> Unit) = addEventListener(object : Listener {
    @org.bukkit.event.EventHandler
    fun onPlayerInteract(event: org.bukkit.event.player.PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.action == Action.RIGHT_CLICK_BLOCK && !(event.clickedBlock?.type?.isInteractable == false || event.player.isSneaking)) return
        listener(event.player, event.item ?: return)
    }
})