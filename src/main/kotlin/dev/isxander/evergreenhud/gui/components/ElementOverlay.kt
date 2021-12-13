/*
 * EvergreenHUD - A mod to improve your heads-up-display.
 * Copyright (c) isXander [2019 - 2021].
 *
 * This work is licensed under the CC BY-NC-SA 4.0 License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/4.0
 */

package dev.isxander.evergreenhud.gui.components

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import dev.isxander.evergreenhud.elements.Element
import dev.isxander.evergreenhud.gui.screens.ElementDisplay
import dev.isxander.evergreenhud.utils.Drawable
import net.minecraft.client.gui.AbstractParentElement
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.gui.Drawable as GuiDrawable
import net.minecraft.client.gui.Element as GuiElement

class ElementOverlay(private val element: Element, private val screen: ElementDisplay) : AbstractParentElement(), GuiDrawable, Selectable, Drawable {
    val children = mutableListOf<GuiElement>()

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        val hitbox = element.calculateHitBox(1f, element.position.scale)

        val width = 0.5f

        RenderSystem.blendFuncSeparate(
            GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR,
            GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR,
            GlStateManager.SrcFactor.ONE,
            GlStateManager.DstFactor.ZERO
        )
        matrices.drawBorderLines(hitbox.x - width, hitbox.y - width, hitbox.x + hitbox.width, hitbox.y + hitbox.height, width, -1)
        RenderSystem.defaultBlendFunc()

        for (child in children) {
            if (child is GuiDrawable) {
                child.render(matrices, mouseX, mouseY, delta)
            }
        }
    }

    override fun children(): MutableList<out GuiElement> = children

    override fun appendNarrations(builder: NarrationMessageBuilder?) {}
    override fun getType(): Selectable.SelectionType = Selectable.SelectionType.NONE
}
