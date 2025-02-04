package io.schemat.scopesAndMore.utils.ui

import io.schemat.scopesAndMore.*
import io.schemat.scopesAndMore.utils.ButtonState
import io.schemat.scopesAndMore.utils.ButtonStyle
import io.schemat.scopesAndMore.utils.MinecraftColor
import io.schemat.scopesAndMore.utils.heledron.*
import org.bukkit.*
import org.bukkit.entity.Display
import org.bukkit.entity.Display.Billboard
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.joml.Matrix4f
import org.joml.Quaternionf

class DisplayGroup(
    val world: World,
    position: Vector,  // Remove var here
    var rotation: Quaternionf = Quaternionf(),
    var scale: Float = 1.0f,
    var baseZLayer: Float = 0f
) {
    private val group = RenderEntityGroup()
    private var baseTransform = Matrix4f()

    // Make position private and provide a custom getter/setter
    private var _position: Vector = position
    var position: Vector
        get() = _position
        set(value) {
            _position = value
            updateBaseTransform()
        }

    init {
        updateBaseTransform()
    }

    private fun updateBaseTransform() {
        baseTransform = Matrix4f()
            .rotate(rotation)
            .scale(scale)
    }

    // Add text display with relative positioning
    fun addText(
        id: String,
        text: String,
        x: Float = 0f,
        y: Float = 0f,
        z: Float = 0f,
        scale: Float = 1f,
        color: MinecraftColor = MinecraftColor.WHITE,
        backgroundColor: Color = Color.BLACK,
        billboard: Billboard = Billboard.FIXED
    ): DisplayGroup {
        group.add(id, textRenderEntity(
            world = world,
            position = position,
            init = {
                it.text = MinecraftColor.format(text, color)
                it.billboard = billboard
                it.backgroundColor = backgroundColor
                it.brightness = Display.Brightness(15, 15)
            },
            update = {
                val transform = Matrix4f(baseTransform)
                    .translate(x, y, baseZLayer + z)
                    .scale(scale)
                    .translate(-0.5f, -0.5f, 0f)
                it.interpolateTransform(transform.mul(textBackgroundTransform))
            }
        ))
        return this
    }

    // Add block display with relative positioning
    fun addBlock(
        id: String,
        material: Material,
        x: Float = 0f,
        y: Float = 0f,
        z: Float = 0f,
        scale: Float = 1f,
    ): DisplayGroup {
        group.add(id, blockRenderEntity(
            world = world,
            position = position,
            init = {
                it.block = material.createBlockData()
                it.brightness = Display.Brightness(15, 15)
            },
            update = {
                val transform = Matrix4f(baseTransform)
                    .translate(x, y, baseZLayer + z)
                    .scale(scale)
                    .translate(-0.5f, -0.5f, -0.5f)
                it.interpolateTransform(transform)
            }
        ))
        return this
    }

    // Add interactive button with relative positioning
    fun addButton(
        id: String,
        text: String,
        x: Float = 0f,
        y: Float = 0f,
        z: Float = 0f,
        scale: Float = 1f,
        style: ButtonStyle = ButtonStyle(),
        players: List<Player>,
        onClick: () -> Unit
    ): DisplayGroup {
        val pointDetector = PlanePointDetector(players, position)
        var state = ButtonState.NORMAL

        val buttonTransform = Matrix4f(baseTransform)
            .translate(x, y, baseZLayer + z)
            .scale(scale)
            .translate(-0.5f, -0.5f, 0f)

        pointDetector.lookingAt(buttonTransform).forEach { _ ->
            state = ButtonState.HOVERED
        }

        pointDetector.detectClick(buttonTransform).forEach { result ->
            if (result.isClicked) onClick()
        }

        val colors = if (state == ButtonState.HOVERED) style.hovered else style.normal

        group.add(id, textRenderEntity(
            world = world,
            position = position,
            init = {
                it.text = MinecraftColor.format(text, colors.text)
                it.brightness = Display.Brightness(15, 15)
            },
            update = {
                it.backgroundColor = colors.background
                it.text = MinecraftColor.format(text, colors.text)
                it.interpolateTransform(buttonTransform.mul(textBackgroundTransform))
            }
        ))
        return this
    }

    // Create a sub-group with its own relative transform
    fun createSubGroup(
        x: Float = 0f,
        y: Float = 0f,
        z: Float = 0f,
        scale: Float = 1f,
        rotation: Quaternionf = Quaternionf()
    ): DisplayGroup {
        return DisplayGroup(
            world = world,
            position = position,
            rotation = Quaternionf(this.rotation).mul(rotation),
            scale = this.scale * scale,
            baseZLayer = this.baseZLayer + z
        )
    }

    // Get the final RenderEntityGroup
    fun build(): RenderEntityGroup = group
}