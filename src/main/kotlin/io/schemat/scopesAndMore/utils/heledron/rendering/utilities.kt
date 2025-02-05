package io.schemat.scopesAndMore.utils.heledron.rendering


import org.bukkit.entity.Display
import org.bukkit.util.Transformation
import org.joml.Matrix4f


fun Display.interpolateTransform(transformation: Transformation) {
    if (this.transformation == transformation) return
    this.transformation = transformation
    this.interpolationDelay = 0
}

fun Display.interpolateTransform(matrix: Matrix4f) {
    val oldTransform = this.transformation
    setTransformationMatrix(matrix)

    if (oldTransform == this.transformation) return
    this.interpolationDelay = 0
}