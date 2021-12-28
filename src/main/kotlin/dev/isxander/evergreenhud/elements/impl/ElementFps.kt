/*
 * EvergreenHUD - A mod to improve your heads-up-display.
 * Copyright (c) isXander [2019 - 2021].
 *
 * This work is licensed under the CC BY-NC-SA 4.0 License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/4.0
 */

package dev.isxander.evergreenhud.elements.impl

import dev.isxander.evergreenhud.elements.type.SimpleTextElement
import dev.isxander.evergreenhud.utils.elementmeta.ElementMeta
import net.minecraft.client.util.math.MatrixStack
import kotlin.math.roundToInt

@ElementMeta(id = "FPS", name = "FPS Display", category = "Simple", description = "Display how many times your screen is updating every second.")
class ElementFps : SimpleTextElement("FPS") {
    private var lastTime = System.currentTimeMillis().toDouble()
    private val frameTimes = ArrayList<Double>()

    override fun calculateValue(): String {
        // calculate mean of frame times and convert to FPS
        val fps = (1000 / (frameTimes.average().takeUnless { it.isNaN() } ?: 1.0)).roundToInt().toString()
        frameTimes.clear()
        return fps
    }

    override fun onRenderTick(matrices: MatrixStack, tickDelta: Float) {
        frameTimes += System.currentTimeMillis() - lastTime
        lastTime = System.currentTimeMillis().toDouble()
    }
}
