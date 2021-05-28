/*
 * Copyright (C) Evergreen [2020 - 2021]
 * This program comes with ABSOLUTELY NO WARRANTY
 * This is free software, and you are welcome to redistribute it
 * under the certain conditions that can be found here
 * https://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 * If you have any questions or concerns, please create
 * an issue on the github page that can be found here
 * https://github.com/Evergreen-Client/EvergreenHUD
 *
 * If you have a private concern, please contact
 * isXander @ business.isxander@gmail.com
 */

package co.uk.isxander.evergreenhud;

import club.sk1er.mods.core.ModCore;
import club.sk1er.mods.core.gui.notification.Notifications;
import club.sk1er.mods.core.util.MinecraftUtils;
import club.sk1er.mods.core.util.Multithreading;
import co.uk.isxander.evergreenhud.addon.AddonManager;
import co.uk.isxander.evergreenhud.config.convert.impl.ChromaHudConverter;
import co.uk.isxander.evergreenhud.config.convert.impl.SimpleHudConverter;
import co.uk.isxander.evergreenhud.elements.ElementManager;
import co.uk.isxander.evergreenhud.elements.impl.ElementText;
import co.uk.isxander.evergreenhud.repo.BlacklistManager;
import co.uk.isxander.evergreenhud.gui.screens.impl.GuiOldForge;
import co.uk.isxander.xanderlib.XanderLib;
import co.uk.isxander.xanderlib.ui.editor.AbstractGuiModifier;
import co.uk.isxander.xanderlib.utils.Constants;
import co.uk.isxander.evergreenhud.command.EvergreenHudCommand;
import co.uk.isxander.evergreenhud.config.ElementConfig;
import co.uk.isxander.evergreenhud.gui.screens.impl.GuiMain;
import co.uk.isxander.evergreenhud.repo.UpdateChecker;
import net.minecraft.client.gui.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.List;

@Mod(modid = EvergreenHUD.MOD_ID, name = EvergreenHUD.MOD_NAME, version = EvergreenHUD.MOD_VERSION, clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class EvergreenHUD implements Constants {

    public static final String MOD_ID = "evergreenhud";
    public static final String MOD_NAME = "EvergreenHUD";
    public static final String MOD_VERSION = "@BUILD_VER@";
    public static final String UPDATE_NAME = "the next step.";
    public static final boolean RELEASE = false;

    public static final Logger LOGGER = LogManager.getLogger("EvergreenHUD");
    public static final File DATA_DIR = new File(mc.mcDataDir, "config/evergreenhud");

    @Mod.Instance(EvergreenHUD.MOD_ID)
    private static EvergreenHUD instance;

    private ElementManager elementManager;
    private AddonManager addonManager;
    private boolean development;

    private boolean firstLaunch = false;
    private boolean versionTwoFirstLaunch = false;

    private boolean disabled = false;
    private boolean blacklisted = false;

    private boolean reset = false;

    private final KeyBinding keybind = new KeyBinding("Open GUI", Keyboard.KEY_HOME, "EvergreenHUD");

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModCore.getInstance().initialize(mc.mcDataDir);
        XanderLib.getInstance().initPhase();

        ProgressManager.ProgressBar progress = ProgressManager.push("EvergreenHUD", 10);

        progress.step("Blacklist Check");
        blacklisted = BlacklistManager.isVersionBlacklisted(MOD_VERSION);
        if (blacklisted) disable();

        progress.step("Forge Check");
        if (ForgeVersion.getBuildVersion() < 2318 && ForgeVersion.getBuildVersion() != 0) {
            disable();
            ModCore.getInstance().getGuiHandler().open(new GuiOldForge());
        }

        if (disabled) {
            ProgressManager.pop(progress);
            return;
        }

        progress.step("Evergreen Check");
        firstLaunch = !DATA_DIR.exists();
        versionTwoFirstLaunch = !ElementConfig.CONFIG_FILE.exists();

        progress.step("Registering Hooks");
        ClientCommandHandler.instance.registerCommand(new EvergreenHudCommand());
        ClientRegistry.registerKeyBinding(keybind);
        XanderLib.getInstance().getGuiEditor().addModifier(GuiOptions.class, new AbstractGuiModifier() {
            @Override
            public void onInitGuiPost(GuiScreen screen, List<GuiButton> buttonList) {
                if (mc.theWorld != null)
                    buttonList.add(new GuiButton(991, screen.width / 2 + 5, screen.height / 6 + 24 - 6, 150, 20, "EvergreenHUD..."));
            }

            @Override
            public void onActionPerformedPost(GuiScreen screen, List<GuiButton> buttonList, GuiButton button) {
                if (button.id == 991) {
                    mc.displayGuiScreen(new GuiMain());
                }
            }
        });

        progress.step("Discovering Addons");
        addonManager = new AddonManager();
        addonManager.discoverAddons();
        progress.step("Initialising Element Manager");
        MinecraftForge.EVENT_BUS.register(elementManager = new ElementManager());
        progress.step("Initialising Addons");
        addonManager.onInit();
        progress.step("Loading Main Config");
        elementManager.getMainConfig().load();
        progress.step("Loading Element Configs");
        elementManager.getElementConfig().load();
        addonManager.onConfigLoad();

        progress.step("Finishing Up");
        MinecraftForge.EVENT_BUS.register(this);

        if (isFirstLaunch() || isVersionTwoFirstLaunch()) {
            if (new File(ChromaHudConverter.DEFAULT_DIR, ChromaHudConverter.CONFIG_FILE).exists()) {
                Notifications.INSTANCE.pushNotification("ChromaHUD Detected", "An existing ChromaHUD configuration has been detected. Would you like to convert it to EvergreenHUD?", () -> {
                    new ChromaHudConverter(ChromaHudConverter.DEFAULT_DIR).process(EvergreenHUD.getInstance().getElementManager());

                    return null;
                });
            }
            if (SimpleHudConverter.DEFAULT_DIR.exists()) {
                Notifications.INSTANCE.pushNotification("SimpleHUD Detected", "An existing SimpleHUD configuration has been detected. Would you like to convert it to EvergreenHUD?", () -> {
                    new SimpleHudConverter(SimpleHudConverter.DEFAULT_DIR).process(EvergreenHUD.getInstance().getElementManager());

                    return null;
                });
            }

            ElementText textElement = new ElementText();
            textElement.getPosition().setScaledX(0.5f);
            textElement.getPosition().setScaledY(0.5f);
            textElement.text.set("Use /evergreenhud to configure.");
            getElementManager().addElement(textElement);
            getElementManager().getElementConfig().save();
        }

        ProgressManager.pop(progress);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if (blacklisted) {
            Notifications.INSTANCE.pushNotification("EvergreenHUD",
                    "The current version of this mod has been blacklisted.\n"
                    + "Please check the discord server for updates.\n"
                    + "Click to join the discord.",

            () -> {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(new URI("https://discord.gg/AJv5ZnNT8q"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Notifications.INSTANCE.pushNotification("EvergreenHUD", "Unfortunately, your computer does not seem to support web-browsing.");
                }
                return null;
            });
        }
        if (disabled) return;

        Multithreading.runAsync(() -> {
            if (MinecraftUtils.isDevelopment()) {
                LOGGER.warn("Running on non-public version. Skipped update check.");
                development = true;
            } else {
                String version = UpdateChecker.getNeededVersion();
                if (!version.equalsIgnoreCase(EvergreenHUD.MOD_VERSION)) {
                    LOGGER.warn("Mod is out of date. " + EvergreenHUD.MOD_VERSION + " > " + version);
                    notifyUpdate(version);
                }
            }
        });

        if (reset) {
            reset = false;
            Notifications.INSTANCE.pushNotification("EvergreenHUD", "The configuration has been reset due to a version change that makes your configuration incompatible with the current version.");
        }
    }

    public void disable() {
        disabled = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (keybind.isPressed())
            mc.displayGuiScreen(new GuiMain());
    }

    public static void notifyUpdate(String latestVersion) {
        Notifications.INSTANCE.pushNotification("EvergreenHUD", "You are running an outdated version.\nCurrent: " + EvergreenHUD.MOD_VERSION + "\nLatest: " + latestVersion + "\n\nClick here to download.", () -> {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(new URI("https://short.evergreenclient.com/GlYH5z"));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Notifications.INSTANCE.pushNotification("EvergreenHUD", "Unfortunately, your computer does not seem to support web-browsing so the mod could not open the download page.\n\nPlease navigate to \"https://short.evergreenclient.com/GlYH5z\"" );
            }
            return null;
        });
    }

    public static EvergreenHUD getInstance() {
        return instance;
    }

    public ElementManager getElementManager() {
        return elementManager;
    }

    public AddonManager getAddonManager() {
        return addonManager;
    }

    public void notifyConfigReset() {
        reset = true;
    }

    public boolean isDevelopment() {
        return development;
    }

    public boolean isFirstLaunch() {
        return this.firstLaunch;
    }

    public boolean isVersionTwoFirstLaunch() {
        return this.versionTwoFirstLaunch;
    }

}
