/*
 * EvergreenHUD - A mod to improve your heads-up-display.
 * Copyright (c) isXander [2019 - 2022].
 *
 * This work is licensed under the GPL-3 License.
 * To view a copy of this license, visit https://www.gnu.org/licenses/gpl-3.0.en.html
 */

package dev.isxander.evergreenhud.utils

import dev.isxander.evergreenhud.EvergreenHUD
import net.minecraft.util.ResourceLocation
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*

fun resource(path: String) = ResourceLocation(EvergreenHUD.ID, path)

val InputStream.base64: String
    get() = Base64.getEncoder().encodeToString(this.readBytes())

fun fromBase64(string: String): InputStream =
    ByteArrayInputStream(Base64.getDecoder().decode(string))
