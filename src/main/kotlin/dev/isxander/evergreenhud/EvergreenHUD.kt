/*
 * EvergreenHUD - A mod to improve your heads-up-display.
 * Copyright (c) isXander [2019 - 2022].
 *
 * This work is licensed under the GPL-3 License.
 * To view a copy of this license, visit https://www.gnu.org/licenses/gpl-3.0.en.html
 */

package dev.isxander.evergreenhud

import cc.woverflow.wcore.utils.command
import dev.isxander.evergreenhud.addons.AddonLoader
import dev.isxander.evergreenhud.config.profile.ProfileManager
import dev.isxander.evergreenhud.elements.Element
import dev.isxander.evergreenhud.elements.ElementManager
import dev.isxander.evergreenhud.event.EventBus
import dev.isxander.evergreenhud.event.Events
import dev.isxander.evergreenhud.event.ServerDamageEntityEventManager
import dev.isxander.evergreenhud.gui.screens.BlacklistedScreen
import dev.isxander.evergreenhud.gui.screens.ElementDisplay
import dev.isxander.evergreenhud.gui.screens.UpdateScreen
import dev.isxander.evergreenhud.gui.screens.test.PositionTest
import dev.isxander.evergreenhud.repo.ReleaseChannel
import dev.isxander.evergreenhud.repo.RepoManager
import dev.isxander.evergreenhud.utils.hypixel.locraw.LocrawManager
import dev.isxander.evergreenhud.utils.logger
import dev.isxander.evergreenhud.utils.mc
import gg.essential.api.EssentialAPI
import gg.essential.api.utils.Multithreading
import kotlinx.coroutines.runBlocking
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import org.bundleproject.libversion.Version
import java.io.File


@Mod(name = EvergreenHUD.NAME, modid = EvergreenHUD.ID, version = EvergreenHUD.VERSION_STR, modLanguageAdapter = "gg.essential.api.utils.KotlinAdapter")
object EvergreenHUD {
    const val NAME = "__GRADLE_NAME__"
    const val ID = "__GRADLE_ID__"
    const val REVISION = "__GRADLE_REVISION__"
    const val VERSION_STR = "__GRADLE_VERSION__"
    val VERSION = Version.of(VERSION_STR)

    val RELEASE_CHANNEL: ReleaseChannel
        get() =
            if (VERSION.prerelease == null) ReleaseChannel.RELEASE
            else ReleaseChannel.BETA

    val dataDir = File(mc.mcDataDir, "evergreenhud")
    val eventBus = EventBus()
    val locrawManager = LocrawManager()

    lateinit var profileManager: ProfileManager private set
    lateinit var elementManager: ElementManager private set
    lateinit var addonLoader: AddonLoader private set

    val isReplayModLoaded by lazy { Loader.isModLoaded("replaymod") }

    var postInitialized = false
        private set

    /**
     * Initialises the whole mod
     *
     * @since 2.0
     * @author isXander
     */
    @Mod.EventHandler
    fun onInitializeClient(event: FMLInitializationEvent) {
        logger.info("Starting EvergreenHUD $VERSION_STR for ${MinecraftForge.MC_VERSION}")

        val startTime = System.currentTimeMillis()

        dataDir.mkdirs()

        logger.debug("Initialising element manager...")
        elementManager = ElementManager()

        logger.debug("Discovering addons...")
        addonLoader = AddonLoader()
        logger.debug("Invoking pre-initialization addon entrypoints...")
        addonLoader.invokePreinitEntrypoints()

        logger.debug("Loading configs...")
        profileManager = ProfileManager().apply { load() }
        elementManager.apply {
            globalConfig.load()
            elementConfig.load()
        }

        logger.debug("Registering hooks...")

        command("evergreenhud", aliases = arrayListOf("evergreen", "egh")) {
            main {
                EssentialAPI.getGuiUtil().openScreen(ElementDisplay(mc.currentScreen))
            }
            if (EssentialAPI.getMinecraftUtil().isDevelopment()) {
                "test" {
                    action = {
                        if (it.isNotEmpty() && it[0] == "position") {
                            EssentialAPI.getGuiUtil().openScreen(PositionTest())
                        }
                    }
                }
            }
            "add" {
                action = { array ->
                    val availableIds = elementManager.availableElements.values.map { it.id }
                    println(availableIds)
                    if (array.isNotEmpty()) {
                        println(array[0])
                        for (id in availableIds) {
                            if (array[0].lowercase() == id.lowercase()) {
                                val element = elementManager.getNewElementInstance<Element>(id)!!
                                elementManager.addElement(element)
                                elementManager.elementConfig.save()
                            }
                        }
                    }
                }
            }
            "remove" {
                action = {
                    val elements = elementManager.currentElements
                    if (it.size == 2) {
                        for (id in elements) {
                            if (it[0] == id.metadata.id) {
                                if (it[1].toIntOrNull() != null) {
                                    elementManager.removeElement(elementManager.currentElements.filter { yes -> yes.metadata.id == id.toString() }[it[1].toInt()])
                                    elementManager.elementConfig.save()
                                }
                            }
                        }
                    }
                }
            }
        }

        logger.debug("Registering events...")
        registerEvents()

        //logger.debug("Registering packet listeners...")
        //registerElementsPacket()

        logger.debug("Invoking addon entrypoints...")
        addonLoader.invokeInitEntrypoints()

        logger.info("Finished loading EvergreenHUD. Took ${System.currentTimeMillis() - startTime} ms.")
    }

    @Mod.EventHandler
    fun onPostInitialize(event: FMLPostInitializationEvent) {
        if (!postInitialized) {
            if (!EssentialAPI.getMinecraftUtil().isDevelopment()) {
                if (elementManager.checkForUpdates || elementManager.checkForSafety) {
                    logger.info("Getting information from API...")
                    Multithreading.runAsync {
                        val response = runBlocking { RepoManager.getResponse() }

                        val latest = response.latest[RELEASE_CHANNEL]!!
                        if (elementManager.checkForUpdates && latest < VERSION) {
                            logger.info("Found update.")
                            mc.displayGuiScreen(UpdateScreen(latest.toString(), mc.currentScreen))
                        }

                        if (elementManager.checkForSafety && REVISION in response.blacklisted) {
                            logger.warn("Mod version has been marked as dangerous.")
                            mc.displayGuiScreen(BlacklistedScreen(mc.currentScreen))
                        }
                    }
                }
            } else {
                logger.info("Skipping update and blacklisting check due to being in a development environment.")
            }
        }

        postInitialized = true
    }

    private fun registerEvents() {
        MinecraftForge.EVENT_BUS.register(Events)
        ServerDamageEntityEventManager(eventBus)
    }
}
