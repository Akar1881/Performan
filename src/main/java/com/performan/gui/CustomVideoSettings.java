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
    private int renderDistance = 6;
    private int particleLevel = 2;
    private boolean dynamicLights = false;
    private int shadowQuality = 0;
    private boolean useAdrenoOptimizations = true;
    private int textureQuality = 0;
    private boolean useVsync = false;
    private int fpsLimit = 60;
    private boolean useFastMath = true;
    private int chunkUpdateLimit = 1;
    private boolean useAggressiveOptimizations = true;
    private float guiScale = 2.0f;
    private int maxChunkUpdates = 2;

    private GuiSlider renderDistanceSlider;
    private GuiSlider guiScaleSlider;
    private GuiSlider chunkUpdatesSlider;

    // Scrolling
    private float scrollY = 0;
    private boolean isScrolling = false;
    private final int contentHeight = 300; // Adjust based on total content height

    public CustomVideoSettings(GuiScreen parentScreenIn, GameSettings gameSettingsIn) {
        this.parentScreen = parentScreenIn;
        this.gameSettings = gameSettingsIn;
        
        // Load current settings
        loadSettings();
    }

    private void loadSettings() {
        this.renderDistance = gameSettings.renderDistanceChunks;
        this.particleLevel = gameSettings.particleSetting;
        this.useVsync = gameSettings.enableVsync;
        this.fpsLimit = gameSettings.limitFramerate;
        this.textureQuality = gameSettings.mipmapLevels > 2 ? 2 : gameSettings.mipmapLevels > 0 ? 1 : 0;
        this.guiScale = gameSettings.guiScale;
        this.shadowQuality = gameSettings.fancyGraphics ? 2 : 0;
        
        // Load additional settings from system properties
        String adrenoOpt = System.getProperty("performan.adreno_optimizations", "true");
        this.useAdrenoOptimizations = Boolean.parseBoolean(adrenoOpt);
        
        String fastMath = System.getProperty("performan.fast_math", "true");
        this.useFastMath = Boolean.parseBoolean(fastMath);
        
        String chunkUpdates = System.getProperty("performan.chunk_updates", "2");
        this.maxChunkUpdates = Integer.parseInt(chunkUpdates);
    }

    private void saveSettings() {
        // Save to GameSettings
        gameSettings.renderDistanceChunks = this.renderDistance;
        gameSettings.particleSetting = this.particleLevel;
        gameSettings.enableVsync = this.useVsync;
        gameSettings.limitFramerate = this.fpsLimit;
        gameSettings.fancyGraphics = this.shadowQuality > 0;
        gameSettings.guiScale = (int)this.guiScale;
        
        // Set mipmapLevels based on texture quality
        switch (this.textureQuality) {
            case 0: gameSettings.mipmapLevels = 0; break;
            case 1: gameSettings.mipmapLevels = 2; break;
            case 2: gameSettings.mipmapLevels = 4; break;
        }
        
        // Save additional settings to system properties
        System.setProperty("performan.adreno_optimizations", String.valueOf(this.useAdrenoOptimizations));
        System.setProperty("performan.fast_math", String.valueOf(this.useFastMath));
        System.setProperty("performan.chunk_updates", String.valueOf(this.maxChunkUpdates));
        
        // Apply Adreno optimizations if enabled
        if (this.useAdrenoOptimizations) {
            applyAdrenoOptimizations();
        }
        
        // Update chunk loading settings
        updateChunkLoadingSettings();
        
        // Save to disk
        gameSettings.saveOptions();
        
        // Force settings to take effect
        mc.gameSettings.loadOptions();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int baseY = height / 6;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 24;

        // Calculate positions with scroll offset
        int currentY = baseY - (int)scrollY;

        renderDistanceSlider = new GuiSlider(this, 0, width / 2 - 100, currentY, "Render Distance", 2.0F, 12.0F, renderDistance, 
            new FormatHelper() {
                public String getText(int id, String name, float value) {
                    return name + ": " + (int)value;
                }
            });
        this.buttonList.add(renderDistanceSlider);

        currentY += spacing;
        guiScaleSlider = new GuiSlider(this, 1, width / 2 - 100, currentY, "GUI Scale", 1.0F, 3.0F, guiScale,
            new FormatHelper() {
                public String getText(int id, String name, float value) {
                    return name + ": " + (int)value;
                }
            });
        this.buttonList.add(guiScaleSlider);

        currentY += spacing;
        chunkUpdatesSlider = new GuiSlider(this, 2, width / 2 - 100, currentY, "Chunk Updates", 1.0F, 5.0F, maxChunkUpdates,
            new FormatHelper() {
                public String getText(int id, String name, float value) {
                    return name + ": " + (int)value;
                }
            });
        this.buttonList.add(chunkUpdatesSlider);

        currentY += spacing;
        this.buttonList.add(new GuiButton(3, width / 2 - 100, currentY, buttonWidth, buttonHeight,
            "Particle Level: " + getParticleLevelString()));

        currentY += spacing;
        this.buttonList.add(new GuiButton(4, width / 2 - 100, currentY, buttonWidth, buttonHeight,
            "Shadow Quality: " + getShadowQualityString()));

        currentY += spacing;
        this.buttonList.add(new GuiButton(5, width / 2 - 100, currentY, buttonWidth, buttonHeight,
            "Adreno Optimizations: " + (useAdrenoOptimizations ? "ON" : "OFF")));

        currentY += spacing;
        this.buttonList.add(new GuiButton(6, width / 2 - 100, currentY, buttonWidth, buttonHeight,
            "Texture Quality: " + getTextureQualityString()));

        currentY += spacing;
        this.buttonList.add(new GuiButton(7, width / 2 - 100, currentY, buttonWidth, buttonHeight,
            "VSync: " + (useVsync ? "ON" : "OFF")));

        currentY += spacing;
        this.buttonList.add(new GuiButton(8, width / 2 - 100, currentY, buttonWidth, buttonHeight,
            "Fast Math: " + (useFastMath ? "ON" : "OFF")));

        currentY += spacing;
        this.buttonList.add(new GuiButton(9, width / 2 - 100, currentY, buttonWidth, buttonHeight,
            "Done"));
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int mouseWheel = Mouse.getEventDWheel();
        if (mouseWheel != 0) {
            if (mouseWheel > 0) {
                scrollY = Math.max(0, scrollY - 20);
            } else {
                float maxScroll = Math.max(0, contentHeight - height);
                scrollY = Math.min(maxScroll, scrollY + 20);
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
        saveSettings(); // Save settings after each slider change
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
        System.setProperty("chunk.loading.queue.size", "1000");
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
                shadowQuality = (shadowQuality + 1) % 3;
                button.displayString = "Shadow Quality: " + getShadowQualityString();
                gameSettings.fancyGraphics = shadowQuality > 0;
                break;
            case 5: // Adreno Optimizations
                useAdrenoOptimizations = !useAdrenoOptimizations;
                button.displayString = "Adreno Optimizations: " + (useAdrenoOptimizations ? "ON" : "OFF");
                applyAdrenoOptimizations();
                break;
            case 6: // Texture Quality
                textureQuality = (textureQuality + 1) % 3;
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
        }
        
        saveSettings();
    }

    private void applyAdrenoOptimizations() {
        if (useAdrenoOptimizations) {
            gameSettings.useVbo = true;
            gameSettings.fboEnable = true;
            gameSettings.mipmapLevels = 0;
            
            System.setProperty("fml.ignorePatchDiscrepancies", "true");
            System.setProperty("fml.ignoreInvalidMinecraftCertificates", "true");
            System.setProperty("forge.forceNoStencil", "true");
            System.setProperty("forge.forgeLightPipelineEnabled", "false");
        }
    }

    private void applyTextureQuality() {
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
            case 2: return "Fancy";
            default: return "Unknown";
        }
    }

    private String getTextureQualityString() {
        switch (textureQuality) {
            case 0: return "Low";
            case 1: return "Medium";
            case 2: return "High";
            default: return "Unknown";
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, this.screenTitle, this.width / 2, 15, 16777215);
        
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