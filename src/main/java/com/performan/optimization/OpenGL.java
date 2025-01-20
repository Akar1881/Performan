package com.performan.optimization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class OpenGL {
    private static final int MIN_BATCH_SIZE = 256;
    private static final int MAX_BATCH_SIZE = 4096;
    private static final int BATCH_SIZE_STEP = 256;
    private static final int TEXTURE_CACHE_SIZE = 128;
    private static final int STATE_CACHE_SIZE = 64;
    private static final int LOW_FPS_THRESHOLD = 30;
    private static final int TARGET_FPS = 60;
    private static final long PERFORMANCE_CHECK_INTERVAL = 1000;
    
    private final Queue<Integer> textureCache = new ArrayDeque<>(TEXTURE_CACHE_SIZE);
    private final Queue<GLState> stateCache = new ArrayDeque<>(STATE_CACHE_SIZE);
    private final Map<Integer, TextureInfo> textureInfoMap = new ConcurrentHashMap<>();
    
    private int currentBatchSize = MIN_BATCH_SIZE;
    private int lastBoundTexture = -1;
    private boolean isInBatch = false;
    private GLState currentState = new GLState();
    private boolean isAndroid;
    private long lastPerformanceCheck = 0;
    private double averageFPS = 60.0;
    private int fpsAccumulator = 0;
    private int fpsSampleCount = 0;
    
    private static class TextureInfo {
        int minFilter;
        int magFilter;
        int wrapS;
        int wrapT;
        boolean mipmapped;
        long lastUsed;
        
        TextureInfo() {
            this.lastUsed = System.currentTimeMillis();
        }
    }
    
    private static class GLState {
        boolean blend;
        boolean depth;
        boolean lighting;
        boolean fog;
        boolean cullFace;
        boolean texture2D;
        boolean alpha;
        int blendFunc;
        int depthFunc;
        float alphaValue;
        
        GLState copy() {
            GLState state = new GLState();
            state.blend = this.blend;
            state.depth = this.depth;
            state.lighting = this.lighting;
            state.fog = this.fog;
            state.cullFace = this.cullFace;
            state.texture2D = this.texture2D;
            state.alpha = this.alpha;
            state.blendFunc = this.blendFunc;
            state.depthFunc = this.depthFunc;
            state.alphaValue = this.alphaValue;
            return state;
        }
    }
    
    public OpenGL() {
        detectPlatform();
        initializeOptimizations();
    }
    
    private void detectPlatform() {
        isAndroid = System.getProperty("java.vendor", "").toLowerCase().contains("android");
    }
    
    private void initializeOptimizations() {
        try {
            if (isAndroid) {
                initializeAndroidOptimizations();
            } else {
                initializePCOptimizations();
            }
            setCommonOptimizations();
        } catch (Exception e) {
            System.err.println("Failed to initialize OpenGL optimizations: " + e.getMessage());
        }
    }
    
    private void initializeAndroidOptimizations() {
        try {
            // Basic state setup
            GlStateManager.disableLighting();
            GlStateManager.disableFog();
            GlStateManager.disableDepth();
            GlStateManager.enableTexture2D();
            
            // Set minimal texture parameters
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            
            // Disable problematic features
            GL11.glDisable(GL11.GL_DITHER);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            
            // Set basic blend mode
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            
            // Disable VBOs
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            
            // Performance hints
            GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_FASTEST);
            GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_FASTEST);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_FASTEST);
            GL11.glHint(GL11.GL_FOG_HINT, GL11.GL_FASTEST);
            
            // Disable shaders
            if (OpenGlHelper.shadersSupported) {
                GL20.glUseProgram(0);
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize Android OpenGL optimizations: " + e.getMessage());
        }
    }
    
    private void initializePCOptimizations() {
        try {
            GlStateManager.enableTexture2D();
            GlStateManager.enableDepth();
            GlStateManager.enableBlend();
            
            if (OpenGlHelper.shadersSupported) {
                GL20.glUseProgram(0);
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize PC OpenGL optimizations: " + e.getMessage());
        }
    }
    
    private void setCommonOptimizations() {
        try {
            // Basic state setup
            GlStateManager.enableTexture2D();
            GlStateManager.enableCull();
            
            // Set blend mode
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            
            // Performance hints
            GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_FASTEST);
            GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_FASTEST);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_FASTEST);
            GL11.glHint(GL11.GL_FOG_HINT, GL11.GL_FASTEST);
        } catch (Exception e) {
            System.err.println("Failed to set common OpenGL optimizations: " + e.getMessage());
        }
    }
    
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        
        try {
            updatePerformanceMetrics();
            applyDynamicOptimizations();
        } catch (Exception e) {
            System.err.println("Error in render tick: " + e.getMessage());
        }
    }
    
    private void updatePerformanceMetrics() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            long currentTime = System.currentTimeMillis();
            
            fpsAccumulator += mc.getDebugFPS();
            fpsSampleCount++;
            
            if (currentTime - lastPerformanceCheck >= PERFORMANCE_CHECK_INTERVAL) {
                if (fpsSampleCount > 0) {
                    averageFPS = (double)fpsAccumulator / fpsSampleCount;
                    adjustBatchSize();
                }
                
                fpsAccumulator = 0;
                fpsSampleCount = 0;
                lastPerformanceCheck = currentTime;
                
                cleanupTextureInfo();
            }
        } catch (Exception e) {
            System.err.println("Error updating performance metrics: " + e.getMessage());
        }
    }
    
    private void adjustBatchSize() {
        if (averageFPS < LOW_FPS_THRESHOLD) {
            currentBatchSize = Math.max(MIN_BATCH_SIZE, currentBatchSize - BATCH_SIZE_STEP);
        } else if (averageFPS > TARGET_FPS) {
            currentBatchSize = Math.min(MAX_BATCH_SIZE, currentBatchSize + BATCH_SIZE_STEP);
        }
    }
    
    private void cleanupTextureInfo() {
        try {
            long currentTime = System.currentTimeMillis();
            textureInfoMap.entrySet().removeIf(entry -> 
                currentTime - entry.getValue().lastUsed > 30000);
        } catch (Exception e) {
            System.err.println("Error cleaning up texture info: " + e.getMessage());
        }
    }
    
    private void applyDynamicOptimizations() {
        try {
            if (averageFPS < LOW_FPS_THRESHOLD) {
                // Low FPS optimizations
                GlStateManager.disableBlend();
                GlStateManager.disableDepth();
                GlStateManager.disableLighting();
                GlStateManager.disableFog();
                GlStateManager.disableAlpha();
                GlStateManager.enableTexture2D();
                GlStateManager.enableCull();
                
                GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_FASTEST);
                GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_FASTEST);
                GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_FASTEST);
                GL11.glHint(GL11.GL_FOG_HINT, GL11.GL_FASTEST);
            }
        } catch (Exception e) {
            System.err.println("Error applying dynamic optimizations: " + e.getMessage());
        }
    }
    
    public void pushState() {
        try {
            GLState newState = currentState.copy();
            if (stateCache.size() >= STATE_CACHE_SIZE) {
                stateCache.poll();
            }
            stateCache.offer(newState);
        } catch (Exception e) {
            System.err.println("Error pushing GL state: " + e.getMessage());
        }
    }
    
    public void popState() {
        try {
            GLState state = stateCache.poll();
            if (state != null) {
                applyState(state);
                currentState = state;
            }
        } catch (Exception e) {
            System.err.println("Error popping GL state: " + e.getMessage());
        }
    }
    
    private void applyState(GLState state) {
        try {
            if (state.blend != currentState.blend) {
                if (state.blend) GlStateManager.enableBlend();
                else GlStateManager.disableBlend();
            }
            
            if (state.depth != currentState.depth) {
                if (state.depth) GlStateManager.enableDepth();
                else GlStateManager.disableDepth();
            }
            
            if (state.texture2D != currentState.texture2D) {
                if (state.texture2D) GlStateManager.enableTexture2D();
                else GlStateManager.disableTexture2D();
            }
            
            if (state.cullFace != currentState.cullFace) {
                if (state.cullFace) GlStateManager.enableCull();
                else GlStateManager.disableCull();
            }
        } catch (Exception e) {
            System.err.println("Error applying GL state: " + e.getMessage());
        }
    }
}