package io.schemat.scopesAndMore.utils.heledron


import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

fun <T : Entity> spawnEntity(location: Location, clazz: Class<T>, initializer: (T) -> Unit): T {
    return location.world!!.spawn(location, clazz, initializer)
}

fun playSound(location: Location, sound: Sound, volume: Float, pitch: Float) {
    location.world!!.playSound(location, sound, volume, pitch)
}

fun PersistentDataContainer.getInt(key: NamespacedKey) = this.get(key, PersistentDataType.INTEGER)
fun PersistentDataContainer.getFloat(key: NamespacedKey) = this.get(key, PersistentDataType.FLOAT)
fun PersistentDataContainer.getDouble(key: NamespacedKey) = this.get(key, PersistentDataType.DOUBLE)

fun PersistentDataContainer.getString(key: NamespacedKey) = this.get(key, PersistentDataType.STRING)
fun PersistentDataContainer.setString(key: NamespacedKey, value: String) = this.set(key, PersistentDataType.STRING, value)

fun PersistentDataContainer.getColor(key: NamespacedKey): Color? {
    val string = getString(key) ?: return null
    val parsed = string.toLongOrNull(radix = 16)?.toInt() ?: return null
    return Color.fromARGB(parsed)
}

fun PersistentDataContainer.setColor(key: NamespacedKey, value: Color) {
    fun toHex(value: Int) = value.toString(16).padStart(2, '0')

    val string = "${toHex(value.alpha)}${toHex(value.red)}${toHex(value.green)}${toHex(value.blue)}"
    setString(key, string)
}