package com.performan.optimization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import java.util.LinkedList;
import java.util.Queue;

public class FPSDrop {
    private static final int FPS_HISTORY_SIZE = 20;
    private static final double MOVEMENT_THRESHOLD_YAW = 2.5;
    private static final double MOVEMENT_THRESHOLD_PITCH = 1.5;
    private static final int STABILIZATION_DELAY = 30;
    private static final float MIN_SENSITIVITY = 0.4f;
    private static final int MIN_RENDER_DISTANCE = 2;
    private static final int MAX_RENDER_DISTANCE = 12;
    private static final double SEVERE_FPS_DROP = 20.0;
    private static final double LOW_FPS_THRESHOLD = 30.0;
    private static final double MEDIUM_FPS_THRESHOLD = 45.0;
    
    private final Queue<Double> fpsHistory = new LinkedList<>();
    private double lastYaw = 0;
    private double lastPitch = 0;
    private int stabilizationTicks = 0;
    private boolean isRapidMovement = false;
    private float originalSensitivity = 1.0f;
    private int originalRenderDistance = 8;
    private boolean originalFancyGraphics = true;
    private int originalParticleSetting = 0;
    private int frameSkipCounter = 0;
    private long lastOptimizationTime = 0;
    private int consecutiveLowFpsCount = 0;
    private OptimizationLevel currentLevel = OptimizationLevel.NONE;
    
    private enum OptimizationLevel {
        NONE(1.0f, 0),
        LIGHT(0.85f, 1),
        MEDIUM(0.7f, 2),
        HEAVY(0.55f, 3),
        SEVERE(0.4f, 4);
        
        final float sensitivityMultiplier;
        final int renderDistanceReduction;
        
        OptimizationLevel(float sensitivityMultiplier, int renderDistanceReduction) {
            this.sensitivityMultiplier = sensitivityMultiplier;
            this.renderDistanceReduction = renderDistanceReduction;
        }
    }
    
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        // Check if we're running on Android
        boolean isAndroid = System.getProperty("java.vendor", "").toLowerCase().contains("android");
        if (!isAndroid) return;
        
        updateFPSHistory();
        detectRapidMovement(mc);
        applyOptimizations(mc);
    }
    
    private void updateFPSHistory() {
        Minecraft mc = Minecraft.getMinecraft();
        double currentFPS = mc.getDebugFPS();
        
        fpsHistory.offer(currentFPS);
        if (fpsHistory.size() > FPS_HISTORY_SIZE) {
            fpsHistory.poll();
        }
        
        // Update consecutive low FPS counter
        double avgFPS = getAverageFPS();
        if (avgFPS < LOW_FPS_THRESHOLD) {
            consecutiveLowFpsCount++;
        } else {
            consecutiveLowFpsCount = Math.max(0, consecutiveLowFpsCount - 1);
        }
    }
    
    private double getAverageFPS() {
        return fpsHistory.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(60.0);
    }
    
    private void detectRapidMovement(Minecraft mc) {
        double deltaYaw = Math.abs(mc.thePlayer.rotationYaw - lastYaw);
        double deltaPitch = Math.abs(mc.thePlayer.rotationPitch - lastPitch);
        
        // Detect rapid screen movement with different thresholds for yaw and pitch
        if (deltaYaw > MOVEMENT_THRESHOLD_YAW || deltaPitch > MOVEMENT_THRESHOLD_PITCH) {
            if (!isRapidMovement) {
                isRapidMovement = true;
                saveOriginalSettings(mc);
                stabilizationTicks = STABILIZATION_DELAY;
            }
        } else if (stabilizationTicks > 0) {
            stabilizationTicks--;
            if (stabilizationTicks == 0) {
                isRapidMovement = false;
                restoreSettings(mc);
            }
        }
        
        lastYaw = mc.thePlayer.rotationYaw;
        lastPitch = mc.thePlayer.rotationPitch;
    }
    
    private void saveOriginalSettings(Minecraft mc) {
        if (!isRapidMovement) {
            originalSensitivity = mc.gameSettings.mouseSensitivity;
            originalRenderDistance = mc.gameSettings.renderDistanceChunks;
            originalFancyGraphics = mc.gameSettings.fancyGraphics;
            originalParticleSetting = mc.gameSettings.particleSetting;
        }
    }
    
    private void applyOptimizations(Minecraft mc) {
        if (!isRapidMovement && consecutiveLowFpsCount < 3) return;
        
        double avgFPS = getAverageFPS();
        long currentTime = System.currentTimeMillis();
        
        // Only apply optimizations every 500ms to prevent rapid changes
        if (currentTime - lastOptimizationTime < 500) return;
        lastOptimizationTime = currentTime;
        
        // Determine optimization level based on FPS and movement
        OptimizationLevel newLevel = determineOptimizationLevel(avgFPS);
        
        if (newLevel != currentLevel) {
            currentLevel = newLevel;
            applyOptimizationLevel(mc, newLevel);
        }
        
        // Apply frame skipping for severe FPS drops
        if (avgFPS < SEVERE_FPS_DROP) {
            frameSkipCounter++;
            if (frameSkipCounter % 2 == 0) {
                return;
            }
        }
        
        // Always optimize GL states during rapid movement or low FPS
        optimizeGLStates();
    }
    
    private OptimizationLevel determineOptimizationLevel(double avgFPS) {
        if (avgFPS < SEVERE_FPS_DROP) return OptimizationLevel.SEVERE;
        if (avgFPS < LOW_FPS_THRESHOLD) return OptimizationLevel.HEAVY;
        if (avgFPS < MEDIUM_FPS_THRESHOLD) return OptimizationLevel.MEDIUM;
        if (isRapidMovement) return OptimizationLevel.LIGHT;
        return OptimizationLevel.NONE;
    }
    
    private void applyOptimizationLevel(Minecraft mc, OptimizationLevel level) {
        // Apply sensitivity reduction
        float targetSensitivity = originalSensitivity * level.sensitivityMultiplier;
        mc.gameSettings.mouseSensitivity = Math.max(MIN_SENSITIVITY, targetSensitivity);
        
        // Calculate dynamic render distance
        int targetRenderDistance = Math.max(MIN_RENDER_DISTANCE,
            originalRenderDistance - level.renderDistanceReduction);
        mc.gameSettings.renderDistanceChunks = Math.min(MAX_RENDER_DISTANCE, targetRenderDistance);
        
        // Adjust graphics settings based on level
        switch (level) {
            case SEVERE:
                mc.gameSettings.fancyGraphics = false;
                mc.gameSettings.particleSetting = 2;
                mc.gameSettings.mipmapLevels = 0;
                break;
            case HEAVY:
                mc.gameSettings.fancyGraphics = false;
                mc.gameSettings.particleSetting = 2;
                mc.gameSettings.mipmapLevels = 0;
                break;
            case MEDIUM:
                mc.gameSettings.fancyGraphics = false;
                mc.gameSettings.particleSetting = 1;
                break;
            case LIGHT:
                mc.gameSettings.particleSetting = Math.min(1, originalParticleSetting);
                break;
            case NONE:
                restoreSettings(mc);
                break;
        }
    }
    
    private void optimizeGLStates() {
        // Disable expensive GL features during optimization
        GlStateManager.disableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        
        // Enable essential features
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        
        // Set performance-focused GL hints
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_FASTEST);
        GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_FASTEST);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_FASTEST);
        GL11.glHint(GL11.GL_FOG_HINT, GL11.GL_FASTEST);
    }
    
    private void restoreSettings(Minecraft mc) {
        // Restore original settings gradually
        mc.gameSettings.mouseSensitivity = originalSensitivity;
        mc.gameSettings.renderDistanceChunks = originalRenderDistance;
        mc.gameSettings.fancyGraphics = originalFancyGraphics;
        mc.gameSettings.particleSetting = originalParticleSetting;
        
        // Reset counters and states
        frameSkipCounter = 0;
        consecutiveLowFpsCount = 0;
        currentLevel = OptimizationLevel.NONE;
        
        // Restore GL states
        GlStateManager.enableBlend();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableFog();
        GlStateManager.enableAlpha();
        
        // Reset GL hints
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_DONT_CARE);
        GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_DONT_CARE);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_DONT_CARE);
        GL11.glHint(GL11.GL_FOG_HINT, GL11.GL_DONT_CARE);
    }
}