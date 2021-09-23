/*
 * EvergreenHUD - A mod to improve on your heads-up-display.
 * Copyright (C) isXander [2019 - 2021]
 *
 * This program comes with ABSOLUTELY NO WARRANTY
 * This is free software, and you are welcome to redistribute it
 * under the certain conditions that can be found here
 * https://www.gnu.org/licenses/lgpl-2.1.en.html
 *
 * If you have any questions or concerns, please create
 * an issue on the github page that can be found here
 * https://github.com/isXander/EvergreenHUD
 *
 * If you have a private concern, please contact
 * isXander @ business.isxander@gmail.com
 */

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()

        maven(url = "https://maven.minecraftforge.net")
        maven(url = "https://jitpack.io")
        maven(url = "https://repo.spongepowered.org/maven/")
        maven(url = "https://maven.fabricmc.net")
    }

}

rootProject.name = "EvergreenHUD"

include(
    ":core",
    ":processor",
    ":annotations",
)

listOf(
    "fabric-1.17.1",
    "forge-1.8.9",
).forEach {
    include(":$it")
    project(":$it").apply {
        projectDir = file("compat/$it")
    }
}
