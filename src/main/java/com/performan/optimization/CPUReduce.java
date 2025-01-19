package com.performan.optimization;

import net.minecraft.client.Minecraft;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.world.World;
import net.minecraft.util.BlockPos;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.LinkedHashSet;
import java.util.Set;

public class CPUReduce {
    private static final int CHUNK_PROCESS_LIMIT = 2;
    private static final int TICK_INTERVAL = 10; // Reduced from 20 for smoother updates
    private static final int MAX_CHUNKS_PER_TICK = 2; // Reduced from 4
    private static final double MIN_FPS_THRESHOLD = 25.0; // Lowered threshold
    private static final int MOVEMENT_DETECTION_THRESHOLD = 2; // blocks
    
    private final Queue<ChunkLoadTask> chunkQueue = new ConcurrentLinkedQueue<>();
    private final Set<String> loadedChunks = new LinkedHashSet<>();
    private int tickCounter = 0;
    private long lastProcessTime = 0;
    private double lastPlayerX = 0;
    private double lastPlayerZ = 0;
    private boolean isMovingFast = false;
    private int movementCooldown = 0;
    
    private static class ChunkLoadTask {
        final int x;
        final int z;
        final int priority;
        
        ChunkLoadTask(int x, int z, int priority) {
            this.x = x;
            this.z = z;
            this.priority = priority;
        }
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;
        
        tickCounter++;
        if (tickCounter % TICK_INTERVAL != 0) return;
        
        // Reset counter to prevent overflow
        tickCounter = 0;
        
        // Check if we're running on Android
        boolean isAndroid = System.getProperty("java.vendor", "").toLowerCase().contains("android");
        if (!isAndroid) return; // Only apply these optimizations on Android
        
        // Detect rapid movement
        detectRapidMovement(mc);
        
        // Adjust processing based on movement
        int processLimit = calculateProcessLimit(mc);
        
        // Process chunk queue with adjusted limits
        processChunkQueue(mc.theWorld, processLimit);
        
        // Update timing and position
        lastProcessTime = System.currentTimeMillis();
        lastPlayerX = mc.thePlayer.posX;
        lastPlayerZ = mc.thePlayer.posZ;
        
        // Optimize memory more frequently during fast movement
        if (isMovingFast || tickCounter % (TICK_INTERVAL * 3) == 0) {
            optimizeMemory();
        }
    }
    
    private void detectRapidMovement(Minecraft mc) {
        double deltaX = Math.abs(mc.thePlayer.posX - lastPlayerX);
        double deltaZ = Math.abs(mc.thePlayer.posZ - lastPlayerZ);
        
        if (deltaX > MOVEMENT_DETECTION_THRESHOLD || deltaZ > MOVEMENT_DETECTION_THRESHOLD) {
            isMovingFast = true;
            movementCooldown = 20; // Cooldown period
        } else if (movementCooldown > 0) {
            movementCooldown--;
            if (movementCooldown == 0) {
                isMovingFast = false;
            }
        }
    }
    
    private int calculateProcessLimit(Minecraft mc) {
        // Base limit for Android
        int processLimit = CHUNK_PROCESS_LIMIT;
        
        // Calculate current FPS
        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - lastProcessTime;
        double currentFPS = 1000.0 / Math.max(1, deltaTime);
        
        // Adjust based on FPS and movement
        if (currentFPS < MIN_FPS_THRESHOLD || isMovingFast) {
            processLimit = 1;
            
            // Reduce render distance temporarily during fast movement
            if (mc.gameSettings.renderDistanceChunks > 4) {
                mc.gameSettings.renderDistanceChunks = 4;
            }
        } else {
            // Gradually restore render distance
            if (mc.gameSettings.renderDistanceChunks < 6) {
                mc.gameSettings.renderDistanceChunks++;
            }
        }
        
        return processLimit;
    }
    
    private void processChunkQueue(World world, int limit) {
        int processed = 0;
        
        while (!chunkQueue.isEmpty() && processed < limit) {
            ChunkLoadTask task = chunkQueue.poll();
            if (task == null) break;
            
            String chunkKey = task.x + ":" + task.z;
            if (loadedChunks.contains(chunkKey)) continue;
            
            // Check if chunk is already loaded
            if (!world.getChunkProvider().chunkExists(task.x, task.z)) {
                // Skip low priority chunks during fast movement
                if (isMovingFast && task.priority > 1) continue;
                
                // Load chunk with reduced processing priority
                world.getChunkFromChunkCoords(task.x, task.z);
                loadedChunks.add(chunkKey);
                processed++;
                
                // Longer delay during fast movement
                try {
                    Thread.sleep(isMovingFast ? 10 : 5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    public void queueChunkLoad(int chunkX, int chunkZ) {
        // Calculate priority based on distance to player
        Minecraft mc = Minecraft.getMinecraft();
        int priority = calculateChunkPriority(chunkX, chunkZ, mc);
        
        // Don't queue if too many chunks are waiting
        if (chunkQueue.size() > MAX_CHUNKS_PER_TICK * 2) return;
        
        chunkQueue.offer(new ChunkLoadTask(chunkX, chunkZ, priority));
    }
    
    private int calculateChunkPriority(int chunkX, int chunkZ, Minecraft mc) {
        if (mc.thePlayer == null) return 3;
        
        int playerChunkX = mc.thePlayer.chunkCoordX;
        int playerChunkZ = mc.thePlayer.chunkCoordZ;
        
        int distance = Math.max(
            Math.abs(chunkX - playerChunkX),
            Math.abs(chunkZ - playerChunkZ)
        );
        
        return distance <= 2 ? 1 : distance <= 4 ? 2 : 3;
    }
    
    private void optimizeMemory() {
        // Clear empty chunks from queue
        chunkQueue.removeIf(task -> task == null);
        
        // Clear old loaded chunks tracking
        if (loadedChunks.size() > 100) {
            loadedChunks.clear();
        }
        
        // More aggressive cleanup during fast movement
        if (isMovingFast) {
            chunkQueue.clear();
            System.gc();
        }
    }
    
    public boolean shouldProcessChunk(int chunkX, int chunkZ) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return false;
        
        // Get player's chunk coordinates
        int playerChunkX = mc.thePlayer.chunkCoordX;
        int playerChunkZ = mc.thePlayer.chunkCoordZ;
        
        // Calculate distance to player
        int distanceX = Math.abs(chunkX - playerChunkX);
        int distanceZ = Math.abs(chunkZ - playerChunkZ);
        
        // More restrictive distance check during fast movement
        int renderDistance = isMovingFast ? 4 : mc.gameSettings.renderDistanceChunks;
        
        return distanceX <= renderDistance && distanceZ <= renderDistance;
    }
}