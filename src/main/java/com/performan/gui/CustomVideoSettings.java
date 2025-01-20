package com.performan.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlider;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.gui.GuiPageButtonList.GuiResponder;
import org.lwjgl.input.Mouse;
import java.io.IOException;

public class CustomVideoSettings extends GuiScreen implements GuiResponder {
    private final GuiScreen parentScreen;
    private String screenTitle = "Performan Settings";
    private final GameSettings gameSettings;
    private boolean isAndroid;
    private boolean settingsChanged = false;
    private long lastSettingChange = 0;
    private static final long SAVE_DELAY = 500; // 500ms delay before saving

    // Settings states
    private int renderDistance;
    private int particleLevel;
    private int shadowQuality;
    private boolean useAdrenoOptimizations;
    private int textureQuality;
    private boolean useVsync;
    private int fpsLimit;
    private boolean useFastMath;
    private int maxChunkUpdates;
    private float guiScale;
    private int brightnessLevel;

    private GuiSlider renderDistanceSlider;
    private GuiSlider guiScaleSlider;
    private GuiSlider chunkUpdatesSlider;

    // Scrolling
    private float scrollY = 0;
    private boolean isScrolling = false;
    private final int contentHeight = 300;
    private final int buttonSpacing;
    private final int buttonWidth;
    private final int buttonHeight;

    public CustomVideoSettings(GuiScreen parentScreenIn, GameSettings gameSettingsIn) {
        this.parentScreen = parentScreenIn;
        this.gameSettings = gameSettingsIn;
        this.isAndroid = System.getProperty("java.vendor", "").toLowerCase().contains("android");
        
        // Adjust button dimensions for Android
        if (isAndroid) {
            this.buttonWidth = 300;
            this.buttonHeight = 40;
            this.buttonSpacing = 44;
        } else {
            this.buttonWidth = 200;
            this.buttonHeight = 20;
            this.buttonSpacing = 24;
        }
        
        loadSettings();
    }

    private void loadSettings() {
        // Load current settings
        renderDistance = gameSettings.renderDistanceChunks;
        particleLevel = gameSettings.particleSetting;
        shadowQuality = gameSettings.fancyGraphics ? 2 : 1;
        useAdrenoOptimizations = isAndroid;
        textureQuality = gameSettings.mipmapLevels > 2 ? 2 : 1;
        useVsync = gameSettings.enableVsync;
        fpsLimit = gameSettings.limitFramerate;
        useFastMath = true;
        maxChunkUpdates = isAndroid ? 2 : 3;
        guiScale = gameSettings.guiScale;
        brightnessLevel = (int)(gameSettings.gammaSetting * 2);

        // Apply Android-specific limits
        if (isAndroid) {
            renderDistance = Math.min(renderDistance, 8);
            particleLevel = Math.max(particleLevel, 1);
            shadowQuality = Math.min(shadowQuality, 1);
            textureQuality = Math.min(textureQuality, 1);
            fpsLimit = Math.min(fpsLimit, 90);
            maxChunkUpdates = Math.min(maxChunkUpdates, 2);
            guiScale = Math.min(guiScale, 2.0f);
        }
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        
        ScaledResolution sr = new ScaledResolution(mc);
        int scaledWidth = sr.getScaledWidth();
        int scaledHeight = sr.getScaledHeight();
        
        int totalContentHeight = buttonSpacing * 12;
        int baseY = (scaledHeight - totalContentHeight) / 2;
        int currentY = baseY - (int)scrollY;
        int leftPos = (scaledWidth - buttonWidth) / 2;

        // Render Distance
        int maxRenderDistance = isAndroid ? 8 : 16;
        renderDistanceSlider = new GuiSlider(this, 0, leftPos, currentY, 
            I18n.format("options.renderDistance"), 2.0F, maxRenderDistance, renderDistance, 
            (id, name, value) -> name + ": " + (int)value);
        renderDistanceSlider.width = buttonWidth;
        this.buttonList.add(renderDistanceSlider);

        currentY += buttonSpacing;
        
        // GUI Scale
        float maxGuiScale = isAndroid ? 2.0f : 3.0f;
        guiScaleSlider = new GuiSlider(this, 1, leftPos, currentY, 
            I18n.format("options.guiScale"), 1.0F, maxGuiScale, guiScale,
            (id, name, value) -> name + ": " + String.format("%.1f", value));
        guiScaleSlider.width = buttonWidth;
        this.buttonList.add(guiScaleSlider);

        currentY += buttonSpacing;

        // Chunk Updates
        int maxUpdates = isAndroid ? 2 : 5;
        chunkUpdatesSlider = new GuiSlider(this, 2, leftPos, currentY, 
            I18n.format("options.chunkUpdates"), 1.0F, maxUpdates, maxChunkUpdates,
            (id, name, value) -> name + ": " + (int)value);
        chunkUpdatesSlider.width = buttonWidth;
        this.buttonList.add(chunkUpdatesSlider);

        currentY += buttonSpacing;
        
        // Particles
        this.buttonList.add(new GuiButton(3, leftPos, currentY, buttonWidth, buttonHeight,
            "Particle Level: " + getParticleLevelString()));

        currentY += buttonSpacing;
        
        // Shadows
        this.buttonList.add(new GuiButton(4, leftPos, currentY, buttonWidth, buttonHeight,
            "Shadow Quality: " + getShadowQualityString()));

        // Adreno Optimizations (Android only)
        if (isAndroid) {
            currentY += buttonSpacing;
            this.buttonList.add(new GuiButton(5, leftPos, currentY, buttonWidth, buttonHeight,
                "Adreno Optimizations: " + (useAdrenoOptimizations ? "ON" : "OFF")));
        }

        currentY += buttonSpacing;
        
        // Texture Quality
        this.buttonList.add(new GuiButton(6, leftPos, currentY, buttonWidth, buttonHeight,
            "Texture Quality: " + getTextureQualityString()));

        currentY += buttonSpacing;
        
        // VSync
        this.buttonList.add(new GuiButton(7, leftPos, currentY, buttonWidth, buttonHeight,
            "VSync: " + (useVsync ? "ON" : "OFF")));

        currentY += buttonSpacing;
        
        // Fast Math
        this.buttonList.add(new GuiButton(8, leftPos, currentY, buttonWidth, buttonHeight,
            "Fast Math: " + (useFastMath ? "ON" : "OFF")));

        currentY += buttonSpacing;
        
        // FPS Limit
        this.buttonList.add(new GuiButton(10, leftPos, currentY, buttonWidth, buttonHeight,
            "FPS Limit: " + getFpsLimitString()));

        currentY += buttonSpacing;
        
        // Brightness
        this.buttonList.add(new GuiButton(11, leftPos, currentY, buttonWidth, buttonHeight,
            "Brightness: " + getBrightnessLevelString()));

        currentY += buttonSpacing;
        
        // Done button
        this.buttonList.add(new GuiButton(9, leftPos, currentY, buttonWidth, buttonHeight, "Done"));
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        
        int mouseWheel = Mouse.getEventDWheel();
        if (mouseWheel != 0) {
            float scrollSpeed = isAndroid ? 40 : 20;
            
            if (mouseWheel > 0) {
                scrollY = Math.max(0, scrollY - scrollSpeed);
            } else {
                float maxScroll = Math.max(0, contentHeight - height);
                scrollY = Math.min(maxScroll, scrollY + scrollSpeed);
            }
            initGui();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        
        String platformIndicator = isAndroid ? " (Mobile)" : " (PC)";
        int titleColor = isAndroid ? 0x55FF55 : 0xFFFFFF;
        
        this.drawCenteredString(this.fontRendererObj, this.screenTitle + platformIndicator, 
            this.width / 2, 15, titleColor);
        
        if (contentHeight > height) {
            int scrollbarWidth = isAndroid ? 8 : 6;
            int scrollbarHeight = (int)((float)height / contentHeight * height);
            int scrollbarY = (int)((float)scrollY / contentHeight * height);
            int scrollbarX = width - scrollbarWidth - (isAndroid ? 4 : 2);
            
            drawRect(scrollbarX, 0, scrollbarX + scrollbarWidth, height, 0x33FFFFFF);
            drawRect(scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, 
                    scrollbarY + scrollbarHeight, 0xFFFFFFFF);
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Auto-save settings after delay
        if (settingsChanged && System.currentTimeMillis() - lastSettingChange > SAVE_DELAY) {
            saveSettings();
            settingsChanged = false;
        }
    }

    @Override
    public void onTick(int id, float value) {
        switch(id) {
            case 0:
                renderDistance = (int)value;
                break;
            case 1:
                guiScale = value;
                break;
            case 2:
                maxChunkUpdates = (int)value;
                break;
        }
        markSettingChanged();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;

        switch (button.id) {
            case 3: // Particles
                particleLevel = (particleLevel + 1) % (isAndroid ? 2 : 3);
                button.displayString = "Particle Level: " + getParticleLevelString();
                break;
            case 4: // Shadows
                shadowQuality = (shadowQuality + 1) % (isAndroid ? 2 : 3);
                button.displayString = "Shadow Quality: " + getShadowQualityString();
                break;
            case 5: // Adreno
                if (isAndroid) {
                    useAdrenoOptimizations = !useAdrenoOptimizations;
                    button.displayString = "Adreno Optimizations: " + (useAdrenoOptimizations ? "ON" : "OFF");
                }
                break;
            case 6: // Textures
                textureQuality = (textureQuality + 1) % (isAndroid ? 2 : 3);
                button.displayString = "Texture Quality: " + getTextureQualityString();
                break;
            case 7: // VSync
                useVsync = !useVsync;
                button.displayString = "VSync: " + (useVsync ? "ON" : "OFF");
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
            case 11: // Brightness
                brightnessLevel = (brightnessLevel + 1) % 3;
                button.displayString = "Brightness: " + getBrightnessLevelString();
                break;
        }
        
        if (button.id != 9) {
            markSettingChanged();
        }
    }

    private void markSettingChanged() {
        settingsChanged = true;
        lastSettingChange = System.currentTimeMillis();
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

    private String getBrightnessLevelString() {
        switch (brightnessLevel) {
            case 0: return "Moody";
            case 1: return "50%";
            case 2: return "Night Vision";
            default: return "Unknown";
        }
    }

    private void saveSettings() {
        try {
            // Apply settings to game
            gameSettings.renderDistanceChunks = this.renderDistance;
            gameSettings.particleSetting = this.particleLevel;
            gameSettings.fancyGraphics = this.shadowQuality > 0;
            gameSettings.mipmapLevels = getTextureQualityMipmap();
            gameSettings.enableVsync = this.useVsync;
            gameSettings.limitFramerate = this.fpsLimit;
            gameSettings.guiScale = (int)this.guiScale;
            
            // Apply brightness
            float brightnessValue;
            switch (brightnessLevel) {
                case 0: brightnessValue = 0.0f; break;
                case 1: brightnessValue = 0.5f; break;
                case 2: brightnessValue = 1.0f; break;
                default: brightnessValue = 0.5f;
            }
            gameSettings.gammaSetting = brightnessValue;

            // Apply Android optimizations
            if (isAndroid && useAdrenoOptimizations) {
                gameSettings.useVbo = false;
                gameSettings.fboEnable = false;
                gameSettings.mipmapLevels = 0;
                System.setProperty("fml.ignorePatchDiscrepancies", "true");
                System.setProperty("fml.ignoreInvalidMinecraftCertificates", "true");
                System.setProperty("forge.forceNoStencil", "true");
                System.setProperty("forge.forgeLightPipelineEnabled", "false");
            }

            // Save chunk loading settings
            System.setProperty("chunk.loading.threads", String.valueOf(maxChunkUpdates));
            System.setProperty("chunk.loading.batch.size", String.valueOf(maxChunkUpdates * 2));

            // Save settings to disk
            gameSettings.saveOptions();
            
            // Force resource reload if needed
            if (mc.getTextureManager() != null) {
                mc.refreshResources();
            }
        } catch (Exception e) {
            System.err.println("Failed to save settings: " + e.getMessage());
        }
    }

    private int getTextureQualityMipmap() {
        if (isAndroid) {
            switch (textureQuality) {
                case 0: return 0;
                case 1: return 1;
                default: return 0;
            }
        } else {
            switch (textureQuality) {
                case 0: return 0;
                case 1: return 2;
                case 2: return 4;
                default: return 2;
            }
        }
    }

    @Override
    public void func_175321_a(int id, boolean value) {}

    @Override
    public void func_175319_a(int id, String value) {}
}