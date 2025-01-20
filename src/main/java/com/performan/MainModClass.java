package com.performan;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.lwjgl.input.Keyboard;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraftforge.client.event.GuiOpenEvent;
import com.performan.gui.CustomVideoSettings;
import com.performan.optimization.ExplosionReduce;
import com.performan.optimization.CPUReduce;
import com.performan.optimization.FPSDrop;
import com.performan.optimization.OpenGL;

@Mod(
    modid = MainModClass.MODID,
    name = MainModClass.NAME,
    version = MainModClass.VERSION,
    clientSideOnly = true
)
public class MainModClass {
    public static final String MODID = "performan";
    public static final String NAME = "Performan Optimizer";
    public static final String VERSION = "1.3.0";

    private static final KeyBinding TOGGLE_SETTINGS = new KeyBinding(
        "Open Performance Settings",
        Keyboard.KEY_P,
        "Performan Optimizer"
    );

    private int tickCounter = 0;
    private static final int GARBAGE_COLLECTION_INTERVAL = 6000;
    private boolean isAndroid;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        isAndroid = System.getProperty("java.vendor", "").toLowerCase().contains("android");
        
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ExplosionReduce());
        MinecraftForge.EVENT_BUS.register(new CPUReduce());
        MinecraftForge.EVENT_BUS.register(new OpenGL());
        MinecraftForge.EVENT_BUS.register(new FPSDrop());
        
        // Mobile-specific optimizations
        if (isAndroid) {
            System.setProperty("fml.skipFirstTextureLoad", "true");
            System.setProperty("forge.forceNoStencil", "true");
            System.setProperty("fml.ignorePatchDiscrepancies", "true");
            System.setProperty("fml.ignoreInvalidMinecraftCertificates", "true");
            System.setProperty("forge.forgeLightPipelineEnabled", "false");
            System.setProperty("forge.disableMipmapGeneration", "true");
            System.setProperty("forge.disableVboRendering", "true");
            System.setProperty("forge.disableStencilBuffers", "true");
            System.setProperty("forge.disableDepthBuffer", "false");
            System.setProperty("forge.forceGL20", "false");
            System.setProperty("forge.disableShaders", "true");
            System.setProperty("forge.disableTextureAnimations", "true");
            System.setProperty("forge.skipGLStateChecks", "true");
            System.setProperty("forge.disableModelLoading", "false");
            System.setProperty("forge.enableGLDebugLog", "true");
            System.setProperty("forge.disableDisplayLists", "true");
            System.setProperty("forge.forceDirectMemoryAccess", "false");
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        applyBaseOptimizations(mc);
    }

    private void applyBaseOptimizations(Minecraft mc) {
        if (isAndroid) {
            // Mobile-specific settings
            mc.gameSettings.mipmapLevels = 0;
            mc.gameSettings.renderDistanceChunks = 4;
            mc.gameSettings.fancyGraphics = false;
            mc.gameSettings.useVbo = false;
            mc.gameSettings.particleSetting = 2;
            mc.gameSettings.enableVsync = false;
            mc.gameSettings.anaglyph = false;
            mc.gameSettings.ambientOcclusion = 0;
            mc.gameSettings.fboEnable = false;
            mc.gameSettings.clouds = 0;
            mc.gameSettings.guiScale = Math.min(mc.gameSettings.guiScale, 2);
            mc.gameSettings.limitFramerate = 60;
            mc.gameSettings.ofFastRender = true;
            mc.gameSettings.ofFastMath = true;
            mc.gameSettings.ofSmoothFps = false;
            mc.gameSettings.ofSmoothWorld = false;
            mc.gameSettings.ofAnimatedWater = 2;
            mc.gameSettings.ofAnimatedLava = 2;
            mc.gameSettings.ofAnimatedFire = false;
            mc.gameSettings.ofAnimatedPortal = false;
            mc.gameSettings.ofAnimatedRedstone = false;
            mc.gameSettings.ofAnimatedExplosion = false;
            mc.gameSettings.ofAnimatedFlame = false;
            mc.gameSettings.ofAnimatedSmoke = false;
        } else {
            // PC settings
            mc.gameSettings.mipmapLevels = 4;
            mc.gameSettings.renderDistanceChunks = 8;
            mc.gameSettings.fancyGraphics = true;
            mc.gameSettings.useVbo = true;
            mc.gameSettings.particleSetting = 0;
            mc.gameSettings.enableVsync = true;
            mc.gameSettings.ambientOcclusion = 2;
            mc.gameSettings.fboEnable = true;
            mc.gameSettings.clouds = 2;
        }
        
        // Save settings immediately
        mc.gameSettings.saveOptions();
        
        // Force a resource reload with safe settings
        mc.refreshResources();
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.END) return;

        if (TOGGLE_SETTINGS.isPressed()) {
            Minecraft.getMinecraft().displayGuiScreen(
                new CustomVideoSettings(
                    Minecraft.getMinecraft().currentScreen,
                    Minecraft.getMinecraft().gameSettings
                )
            );
        }

        tickCounter++;
        if (tickCounter >= GARBAGE_COLLECTION_INTERVAL) {
            tickCounter = 0;
            System.gc();
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiVideoSettings) {
            event.gui = new CustomVideoSettings(
                Minecraft.getMinecraft().currentScreen,
                Minecraft.getMinecraft().gameSettings
            );
        }
    }
}