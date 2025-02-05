package io.schemat.scopesAndMore.utils.heledron.rendering


import io.schemat.scopesAndMore.utils.heledron.onDisablePlugin
import io.schemat.scopesAndMore.utils.heledron.onTickEnd
import io.schemat.scopesAndMore.utils.heledron.spawnEntity
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Entity
import org.bukkit.entity.TextDisplay
import org.bukkit.util.Vector
import java.io.Closeable

class RenderEntity <T : Entity> (
    val clazz : Class<T>,
    val location : Location,
    val init : (T) -> Unit = {},
    val preUpdate : (T) -> Unit = {},
    val update : (T) -> Unit = {},
)

class RenderEntityGroup {
    val items = mutableMapOf<Any, RenderEntity<*>>()

    fun add(id: Any, item: RenderEntity<*>) {
        items[id] = item
    }

    fun add(id: Any, item: RenderEntityGroup) {
        for ((subId, part) in item.items) {
            items[id to subId] = part
        }
    }

    fun merge(other: RenderEntityGroup) {
        for ((id, item) in other.items) {
            items[id] = item
        }
    }
}

fun blockRenderEntity(
    location: Location,
    init: (BlockDisplay) -> Unit = {},
    update: (BlockDisplay) -> Unit = {}
) = RenderEntity(
    clazz = BlockDisplay::class.java,
    location = location,
    init = init,
    update = update
)

fun blockRenderEntity(
    world: World,
    position: Vector,
    init: (BlockDisplay) -> Unit = {},
    update: (BlockDisplay) -> Unit = {}
) = RenderEntity(
    clazz = BlockDisplay::class.java,
    location = position.toLocation(world),
    init = init,
    update = update,
)

//fun lineRenderEntity(
//    world: World,
//    position: Vector,
//    vector: Vector,
//    upVector: Vector = if (vector.x + vector.z != 0.0) UP_VECTOR else FORWARD_VECTOR,
//    thickness: Float = .1f,
//    interpolation: Int = 1,
//    init: (BlockDisplay) -> Unit = {},
//    update: (BlockDisplay) -> Unit = {}
//) = blockRenderEntity(
//    world = world,
//    position = position,
//    init = {
//        it.teleportDuration = interpolation
//        it.interpolationDuration = interpolation
//        init(it)
//    },
//    update = {
//        val matrix = Matrix4f().rotateTowards(vector.toVector3f(), upVector.toVector3f())
//            .translate(-thickness / 2, -thickness / 2, 0f)
//            .scale(thickness, thickness, vector.length().toFloat())
//
//        it.applyTransformationWithInterpolation(matrix)
//        update(it)
//    }
//)

fun textRenderEntity(
    location: Location,
    init: (TextDisplay) -> Unit = {},
    preUpdate: (TextDisplay) -> Unit = {},
    update: (TextDisplay) -> Unit = {},
) = RenderEntity(
    clazz = TextDisplay::class.java,
    location = location,
    init = {
        init(it)
    },
    preUpdate = {
        preUpdate(it)
    },
    update = {
        update(it)
    }
)

fun textRenderEntity(
    world: World,
    position: Vector,
    init: (TextDisplay) -> Unit = {},
    preUpdate: (TextDisplay) -> Unit = {},
    update: (TextDisplay) -> Unit = {},
) = textRenderEntity(
    location = position.toLocation(world),
    init = init,
    preUpdate = preUpdate,
    update = update
)

class SingleEntityRenderer<T : Entity>: Closeable {
    var entity: T? = null

    fun render(part: RenderEntity<T>) {
        entity = (entity ?: spawnEntity(part.location, part.clazz) {
            part.init(it)
        }).apply {
            this.teleport(part.location)
            part.preUpdate(this)
            part.update(this)
        }
    }

    override fun close() {
        entity?.remove()
        entity = null
    }
}

class GroupEntityRenderer: Closeable {
    val rendered = mutableMapOf<Any, Entity>()

    private val used = mutableSetOf<Any>()

    fun detachEntity(id: Any) {
        rendered.remove(id)
    }

    override fun close() {
        for (entity in rendered.values) {
            entity.remove()
        }
        rendered.clear()
        used.clear()
    }

    fun render(group: RenderEntityGroup) {

        @Suppress("UNCHECKED_CAST")
        fun <T : Entity>update(renderEntity: RenderEntity<T>, entity: Entity) = renderEntity.update(entity as T)

        @Suppress("UNCHECKED_CAST")
        fun <T : Entity>preUpdate(renderEntity: RenderEntity<T>, entity: Entity) = renderEntity.preUpdate(entity as T)

        for ((id, template) in group.items) renderPart(id, template)
        for ((id, template) in group.items) preUpdate(template, rendered[id]!!)
        for ((id, template) in group.items) update(template, rendered[id]!!)

        val toRemove = rendered.keys - used
        for (key in toRemove) {
            val entity = rendered[key]!!
            entity.remove()
            rendered.remove(key)
        }
        used.clear()
    }


    fun <T: Entity>render(part: RenderEntity<T>) {
        render(RenderEntityGroup().apply { add(0, part) })
    }

    private fun <T: Entity>renderPart(id: Any, template: RenderEntity<T>) {
        used.add(id)

        val oldEntity = rendered[id]
        if (oldEntity != null) {
            // check if the entity is of the same type
            if (oldEntity.type.entityClass == template.clazz) {
                oldEntity.teleport(template.location)
                return
            }

            oldEntity.remove()
            rendered.remove(id)
        }

        val entity = spawnEntity(template.location, template.clazz) {
            template.init(it)
        }
        rendered[id] = entity
    }
}


object SharedEntityRenderer {
    private val renderer = GroupEntityRenderer()
    private var group = RenderEntityGroup()

    fun render(id: Any, group: RenderEntityGroup) {
        SharedEntityRenderer.group.add(id, group)
    }

    fun render(id: Any, entity: RenderEntity<out Entity>) {
        group.add(id, entity)
    }

    fun flush() {
        renderer.render(group)
        group = RenderEntityGroup()
    }

    val rendered: Map<Any, Entity> get() = renderer.rendered

    fun detach(id: Any) {
        renderer.detachEntity(id)
    }

    init {
        onTickEnd {
            flush()
        }

        onDisablePlugin {
            renderer.close()
        }
    }
}