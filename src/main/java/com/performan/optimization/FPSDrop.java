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
    private static final int FPS_HISTORY_SIZE = 10;
    private static final double MOVEMENT_THRESHOLD = 2.0;
    private static final int STABILIZATION_DELAY = 20;
    private static final float MIN_SENSITIVITY = 0.5f;
    
    private final Queue<Double> fpsHistory = new LinkedList<>();
    private double lastYaw = 0;
    private double lastPitch = 0;
    private int stabilizationTicks = 0;
    private boolean isRapidMovement = false;
    private float originalSensitivity = 1.0f;
    private int frameSkipCounter = 0;
    
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
    }
    
    private void detectRapidMovement(Minecraft mc) {
        double deltaYaw = Math.abs(mc.thePlayer.rotationYaw - lastYaw);
        double deltaPitch = Math.abs(mc.thePlayer.rotationPitch - lastPitch);
        
        // Detect rapid screen movement
        if (deltaYaw > MOVEMENT_THRESHOLD || deltaPitch > MOVEMENT_THRESHOLD) {
            if (!isRapidMovement) {
                isRapidMovement = true;
                originalSensitivity = mc.gameSettings.mouseSensitivity;
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
    
    private void applyOptimizations(Minecraft mc) {
        if (!isRapidMovement) return;
        
        // Calculate average FPS
        double avgFPS = fpsHistory.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(60.0);
        
        // Apply optimizations based on FPS
        if (avgFPS < 30.0) {
            // Reduce mouse sensitivity during rapid movement
            float targetSensitivity = Math.max(MIN_SENSITIVITY, originalSensitivity * 0.75f);
            mc.gameSettings.mouseSensitivity = targetSensitivity;
            
            // Skip frames if FPS is very low
            frameSkipCounter++;
            if (frameSkipCounter % 2 == 0) {
                return;
            }
            
            // Reduce render distance temporarily
            if (mc.gameSettings.renderDistanceChunks > 4) {
                mc.gameSettings.renderDistanceChunks = 4;
            }
            
            // Disable fancy graphics temporarily
            mc.gameSettings.fancyGraphics = false;
            
            // Reduce particle effects
            mc.gameSettings.particleSetting = 2;
            
            // Optimize GL states
            optimizeGLStates();
        }
    }
    
    private void optimizeGLStates() {
        // Disable expensive GL features during rapid movement
        GlStateManager.disableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        
        // Enable basic features needed for rendering
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        
        // Set basic GL hints for performance
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_FASTEST);
        GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_FASTEST);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_FASTEST);
    }
    
    private void restoreSettings(Minecraft mc) {
        // Restore original settings
        mc.gameSettings.mouseSensitivity = originalSensitivity;
        mc.gameSettings.renderDistanceChunks = 8; // Default or saved value
        mc.gameSettings.fancyGraphics = true; // Or saved value
        mc.gameSettings.particleSetting = 0; // Or saved value
        
        // Reset frame skip counter
        frameSkipCounter = 0;
        
        // Restore GL states
        GlStateManager.enableBlend();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableFog();
        
        // Reset GL hints
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_DONT_CARE);
        GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_DONT_CARE);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_DONT_CARE);
    }
}