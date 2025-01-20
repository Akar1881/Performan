package com.performan.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlider.FormatHelper;
import net.minecraft.client.gui.GuiPageButtonList.GuiResponder;
import net.minecraft.client.gui.GuiSlider;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.input.Mouse;
import java.io.IOException;

public class CustomVideoSettings extends GuiScreen implements GuiResponder {
    private final GuiScreen parentScreen;
    private String screenTitle = "Performan Settings";
    private final GameSettings gameSettings;
    private boolean gpuOptimizationsEnabled = true;

    // Settings states
    private int renderDistance = 8; // Increased default for PC
    private int particleLevel = 2;
    private boolean dynamicLights = true; // Enabled by default for PC
    private int shadowQuality = 1; // Default to Fast for PC
    private boolean useAdrenoOptimizations = false; // Disabled by default for PC
    private int textureQuality = 1; // Default to Medium for PC
    private boolean useVsync = true; // Enabled by default for PC
    private int fpsLimit = 120; // Higher default for PC
    private boolean useFastMath = true;
    private int chunkUpdateLimit = 2; // Higher default for PC
    private boolean useAggressiveOptimizations = false; // Disabled by default for PC
    private float guiScale = 2.0f;
    private int maxChunkUpdates = 3; // Higher default for PC
    private int brightnessLevel = 1; // Default to 50% for PC

    private GuiSlider renderDistanceSlider;
    private GuiSlider guiScaleSlider;
    private GuiSlider chunkUpdatesSlider;

    // Scrolling
    private float scrollY = 0;
    private boolean isScrolling = false;
    private final int contentHeight = 300;

    // Platform detection
    private final boolean isAndroid;

    public CustomVideoSettings(GuiScreen parentScreenIn, GameSettings gameSettingsIn) {
        this.parentScreen = parentScreenIn;
        this.gameSettings = gameSettingsIn;
        this.isAndroid = System.getProperty("java.vendor", "").toLowerCase().contains("android");
        
        // Load current settings
        loadSettings();
    }

    private void loadSettings() {
        if (isAndroid) {
            loadAndroidSettings();
        } else {
            loadPCSettings();
        }
    }

    private void loadAndroidSettings() {
        this.renderDistance = Math.min(gameSettings.renderDistanceChunks, 6);
        this.particleLevel = 2; // Minimal
        this.shadowQuality = 0; // Off
        this.useAdrenoOptimizations = true;
        this.textureQuality = 0; // Low
        this.useVsync = false;
        this.fpsLimit = 60;
        this.useFastMath = true;
        this.maxChunkUpdates = 1;
        this.guiScale = Math.min(gameSettings.guiScale, 2.0f);
    }

    private void loadPCSettings() {
        this.renderDistance = Math.min(gameSettings.renderDistanceChunks, 12);
        this.particleLevel = gameSettings.particleSetting;
        this.shadowQuality = gameSettings.fancyGraphics ? 2 : 1;
        this.useAdrenoOptimizations = false;
        this.textureQuality = gameSettings.mipmapLevels > 2 ? 2 : 1;
        this.useVsync = gameSettings.enableVsync;
        this.fpsLimit = gameSettings.limitFramerate;
        this.useFastMath = true;
        this.maxChunkUpdates = 3;
        this.guiScale = gameSettings.guiScale;
    }

    private void saveSettings() {
        // Save settings to GameSettings
        gameSettings.renderDistanceChunks = this.renderDistance;
        gameSettings.particleSetting = this.particleLevel;
        gameSettings.fancyGraphics = this.shadowQuality > 0;
        gameSettings.mipmapLevels = getTextureQualityMipmap();
        gameSettings.enableVsync = this.useVsync;
        gameSettings.limitFramerate = this.fpsLimit;
        gameSettings.guiScale = (int)this.guiScale;

        // Apply platform-specific settings
        if (isAndroid && useAdrenoOptimizations) {
            applyAdrenoOptimizations();
        }

        // Save chunk update settings
        System.setProperty("chunk.loading.threads", String.valueOf(maxChunkUpdates));
        System.setProperty("chunk.loading.batch.size", String.valueOf(maxChunkUpdates * 2));

        // Apply brightness
        float brightnessValue;
        switch (brightnessLevel) {
            case 0: brightnessValue = 0.0f; break; // Moody
            case 1: brightnessValue = 0.5f; break; // 50%
            case 2: brightnessValue = 1.0f; break; // Night Vision
            default: brightnessValue = 0.5f;
        }
        gameSettings.gammaSetting = brightnessValue;

        // Save to disk
        gameSettings.saveOptions();

        // Force a resource reload if texture quality changed
        Minecraft.getMinecraft().refreshResources();
    }

    private int getTextureQualityMipmap() {
        if (isAndroid) {
            switch (textureQuality) {
                case 0: return 0; // Low
                case 1: return 1; // Medium
                default: return 0;
            }
        } else {
            switch (textureQuality) {
                case 0: return 0; // Low
                case 1: return 2; // Medium
                case 2: return 4; // High
                default: return 2;
            }
        }
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int baseY = height / 6;
        int buttonWidth = isAndroid ? 250 : 200; // Wider buttons on Android
        int buttonHeight = isAndroid ? 30 : 20; // Taller buttons on Android
        int spacing = isAndroid ? 34 : 24; // More spacing on Android

        // Calculate positions with scroll offset
        int currentY = baseY - (int)scrollY;

        // Adjust slider ranges based on platform
        int maxRenderDistance = isAndroid ? 8 : 16;
        float maxGuiScale = isAndroid ? 2.0f : 3.0f;
        int maxChunkUpdates = isAndroid ? 2 : 5;

        renderDistanceSlider = new GuiSlider(this, 0, width / 2 - buttonWidth/2, currentY, 
            "Render Distance", 2.0F, maxRenderDistance, renderDistance, 
            new FormatHelper() {
                public String getText(int id, String name, float value) {
                    return name + ": " + (int)value;
                }
            });
        this.buttonList.add(renderDistanceSlider);

        currentY += spacing;
        guiScaleSlider = new GuiSlider(this, 1, width / 2 - buttonWidth/2, currentY, 
            "GUI Scale", 1.0F, maxGuiScale, guiScale,
            new FormatHelper() {
                public String getText(int id, String name, float value) {
                    return name + ": " + (int)value;
                }
            });
        this.buttonList.add(guiScaleSlider);

        currentY += spacing;
        chunkUpdatesSlider = new GuiSlider(this, 2, width / 2 - buttonWidth/2, currentY, 
            "Chunk Updates", 1.0F, maxChunkUpdates, maxChunkUpdates,
            new FormatHelper() {
                public String getText(int id, String name, float value) {
                    return name + ": " + (int)value;
                }
            });
        this.buttonList.add(chunkUpdatesSlider);

        currentY += spacing;
        this.buttonList.add(new GuiButton(3, width / 2 - buttonWidth/2, currentY, buttonWidth, buttonHeight,
            "Particle Level: " + getParticleLevelString()));

        currentY += spacing;
        this.buttonList.add(new GuiButton(4, width / 2 - buttonWidth/2, currentY, buttonWidth, buttonHeight,
            "Shadow Quality: " + getShadowQualityString()));

        if (isAndroid) {
            currentY += spacing;
            this.buttonList.add(new GuiButton(5, width / 2 - buttonWidth/2, currentY, buttonWidth, buttonHeight,
                "Adreno Optimizations: " + (useAdrenoOptimizations ? "ON" : "OFF")));
        }

        currentY += spacing;
        this.buttonList.add(new GuiButton(6, width / 2 - buttonWidth/2, currentY, buttonWidth, buttonHeight,
            "Texture Quality: " + getTextureQualityString()));

        currentY += spacing;
        this.buttonList.add(new GuiButton(7, width / 2 - buttonWidth/2, currentY, buttonWidth, buttonHeight,
            "VSync: " + (useVsync ? "ON" : "OFF")));

        currentY += spacing;
        this.buttonList.add(new GuiButton(8, width / 2 - buttonWidth/2, currentY, buttonWidth, buttonHeight,
            "Fast Math: " + (useFastMath ? "ON" : "OFF")));

        currentY += spacing;
        this.buttonList.add(new GuiButton(10, width / 2 - buttonWidth/2, currentY, buttonWidth, buttonHeight,
            "FPS Limit: " + getFpsLimitString()));

        currentY += spacing;
        this.buttonList.add(new GuiButton(11, width / 2 - buttonWidth/2, currentY, buttonWidth, buttonHeight,
            "Brightness: " + getBrightnessLevelString()));

        currentY += spacing;
        this.buttonList.add(new GuiButton(9, width / 2 - buttonWidth/2, currentY, buttonWidth, buttonHeight,
            "Done"));
    }

    private String getBrightnessLevelString() {
        switch (brightnessLevel) {
            case 0: return "Moody";
            case 1: return "50%";
            case 2: return "Night Vision";
            default: return "Unknown";
        }
    }

    private String getFpsLimitString() {
        if (isAndroid) {
            switch (fpsLimit) {
                case 30: return "30";
                case 60: return "60";
                case 90: return "90";
                default: return String.valueOf(fpsLimit);
            }
        } else {
            switch (fpsLimit) {
                case 0: return "Unlimited";
                case 60: return "60";
                case 120: return "120";
                case 240: return "240";
                default: return String.valueOf(fpsLimit);
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int mouseWheel = Mouse.getEventDWheel();
        if (mouseWheel != 0) {
            if (mouseWheel > 0) {
                scrollY = Math.max(0, scrollY - (isAndroid ? 30 : 20));
            } else {
                float maxScroll = Math.max(0, contentHeight - height);
                scrollY = Math.min(maxScroll, scrollY + (isAndroid ? 30 : 20));
            }
            initGui();
        }
    }

    @Override
    public void func_175321_a(int id, boolean value) {
        // Handle boolean value changes
    }

    @Override
    public void onTick(int id, float value) {
        switch(id) {
            case 0: // Render Distance
                renderDistance = (int)value;
                gameSettings.renderDistanceChunks = renderDistance;
                break;
            case 1: // GUI Scale
                guiScale = value;
                gameSettings.guiScale = (int)guiScale;
                updateGuiScale();
                break;
            case 2: // Chunk Updates
                maxChunkUpdates = (int)value;
                updateChunkLoadingSettings();
                break;
        }
        saveSettings();
    }

    @Override
    public void func_175319_a(int id, String value) {
        // Handle string value changes
    }

    private void updateGuiScale() {
        ScaledResolution scaledresolution = new ScaledResolution(mc);
        this.width = scaledresolution.getScaledWidth();
        this.height = scaledresolution.getScaledHeight();
        initGui();
    }

    private void updateChunkLoadingSettings() {
        System.setProperty("chunk.loading.threads", String.valueOf(maxChunkUpdates));
        System.setProperty("chunk.loading.batch.size", String.valueOf(maxChunkUpdates * 2));
        System.setProperty("chunk.loading.queue.size", isAndroid ? "500" : "1000");
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;

        switch (button.id) {
            case 3: // Particle Level
                particleLevel = (particleLevel + 1) % 3;
                button.displayString = "Particle Level: " + getParticleLevelString();
                gameSettings.particleSetting = particleLevel;
                break;
            case 4: // Shadow Quality
                shadowQuality = (shadowQuality + 1) % (isAndroid ? 2 : 3);
                button.displayString = "Shadow Quality: " + getShadowQualityString();
                gameSettings.fancyGraphics = shadowQuality > 0;
                break;
            case 5: // Adreno Optimizations
                if (isAndroid) {
                    useAdrenoOptimizations = !useAdrenoOptimizations;
                    button.displayString = "Adreno Optimizations: " + (useAdrenoOptimizations ? "ON" : "OFF");
                    applyAdrenoOptimizations();
                }
                break;
            case 6: // Texture Quality
                textureQuality = (textureQuality + 1) % (isAndroid ? 2 : 3);
                button.displayString = "Texture Quality: " + getTextureQualityString();
                applyTextureQuality();
                break;
            case 7: // VSync
                useVsync = !useVsync;
                button.displayString = "VSync: " + (useVsync ? "ON" : "OFF");
                gameSettings.enableVsync = useVsync;
                break;
            case 8: // Fast Math
                useFastMath = !useFastMath;
                button.displayString = "Fast Math: " + (useFastMath ? "ON" : "OFF");
                break;
            case 9: // Done
                saveSettings();
                mc.displayGuiScreen(parentScreen);
                break;
            case 10: // FPS Limit
                cycleFpsLimit();
                button.displayString = "FPS Limit: " + getFpsLimitString();
                break;
            case 11: // Brightness Level
                brightnessLevel = (brightnessLevel + 1) % 3;
                button.displayString = "Brightness: " + getBrightnessLevelString();
                break;
        }
        
        saveSettings();
    }

    private void cycleFpsLimit() {
        if (isAndroid) {
            switch (fpsLimit) {
                case 30: fpsLimit = 60; break;
                case 60: fpsLimit = 90; break;
                case 90: fpsLimit = 30; break;
                default: fpsLimit = 60; break;
            }
        } else {
            switch (fpsLimit) {
                case 0: fpsLimit = 60; break;
                case 60: fpsLimit = 120; break;
                case 120: fpsLimit = 240; break;
                case 240: fpsLimit = 0; break;
                default: fpsLimit = 60; break;
            }
        }
        gameSettings.limitFramerate = fpsLimit;
    }

    private void applyAdrenoOptimizations() {
        if (useAdrenoOptimizations && isAndroid) {
            gameSettings.useVbo = false;
            gameSettings.fboEnable = false;
            gameSettings.mipmapLevels = 0;
            
            System.setProperty("fml.ignorePatchDiscrepancies", "true");
            System.setProperty("fml.ignoreInvalidMinecraftCertificates", "true");
            System.setProperty("forge.forceNoStencil", "true");
            System.setProperty("forge.forgeLightPipelineEnabled", "false");
        }
    }

    private void applyTextureQuality() {
        if (isAndroid) {
            switch (textureQuality) {
                case 0: // Low
                    gameSettings.mipmapLevels = 0;
                    break;
                case 1: // Medium
                    gameSettings.mipmapLevels = 1;
                    break;
            }
        } else {
            switch (textureQuality) {
                case 0: // Low
                    gameSettings.mipmapLevels = 0;
                    break;
                case 1: // Medium
                    gameSettings.mipmapLevels = 2;
                    break;
                case 2: // High
                    gameSettings.mipmapLevels = 4;
                    break;
            }
        }
    }

    private String getParticleLevelString() {
        switch (particleLevel) {
            case 0: return "All";
            case 1: return "Decreased";
            case 2: return "Minimal";
            default: return "Unknown";
        }
    }

    private String getShadowQualityString() {
        switch (shadowQuality) {
            case 0: return "OFF";
            case 1: return "Fast";
            case 2: return isAndroid ? "OFF" : "Fancy";
            default: return "Unknown";
        }
    }

    private String getTextureQualityString() {
        if (isAndroid) {
            switch (textureQuality) {
                case 0: return "Low";
                case 1: return "Medium";
                default: return "Unknown";
            }
        } else {
            switch (textureQuality) {
                case 0: return "Low";
                case 1: return "Medium";
                case 2: return "High";
                default: return "Unknown";
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        
        // Draw title with platform-specific styling
        String platformIndicator = isAndroid ? " (Mobile)" : " (PC)";
        this.drawCenteredString(this.fontRendererObj, this.screenTitle + platformIndicator, 
            this.width / 2, 15, 16777215);
        
        // Draw scrollbar if needed
        int contentHeight = this.contentHeight;
        if (contentHeight > height) {
            int scrollbarHeight = (int)((float)height / contentHeight * height);
            int scrollbarY = (int)((float)scrollY / contentHeight * height);
            drawRect(width - 10, scrollbarY, width - 5, scrollbarY + scrollbarHeight, 0x80FFFFFF);
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}