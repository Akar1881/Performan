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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

public class CPUReduce {
    // Constants for performance tuning
    private static final int BASE_CHUNK_PROCESS_LIMIT = 2;
    private static final int TICK_INTERVAL = 5;
    private static final int MAX_CHUNKS_PER_TICK = 2;
    private static final double MIN_FPS_THRESHOLD = 20.0;
    private static final int MOVEMENT_DETECTION_THRESHOLD = 2;
    private static final int QUEUE_SIZE_LIMIT = 64;
    private static final long PERFORMANCE_CHECK_INTERVAL = 500; // ms
    
    // Performance tracking
    private final Queue<ChunkLoadTask> chunkQueue = new ConcurrentLinkedQueue<>();
    private final Set<ChunkPosition> loadedChunks = new LinkedHashSet<>();
    private final AtomicInteger processedChunksCount = new AtomicInteger(0);
    private int tickCounter = 0;
    private long lastProcessTime = 0;
    private long lastPerformanceCheck = 0;
    private double lastPlayerX = 0;
    private double lastPlayerZ = 0;
    private boolean isMovingFast = false;
    private int movementCooldown = 0;
    private int currentProcessLimit = BASE_CHUNK_PROCESS_LIMIT;
    private double averageFPS = 60.0;
    private double fpsAccumulator = 0;
    private int fpsSampleCount = 0;
    
    private static class ChunkPosition {
        final int x;
        final int z;
        
        ChunkPosition(int x, int z) {
            this.x = x;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ChunkPosition)) return false;
            ChunkPosition that = (ChunkPosition) o;
            return x == that.x && z == that.z;
        }
        
        @Override
        public int hashCode() {
            return 31 * x + z;
        }
    }
    
    private static class ChunkLoadTask {
        final int x;
        final int z;
        final int priority;
        final long timestamp;
        
        ChunkLoadTask(int x, int z, int priority) {
            this.x = x;
            this.z = z;
            this.priority = priority;
            this.timestamp = System.currentTimeMillis();
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
        if (!isAndroid) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Update performance metrics
        updatePerformanceMetrics(mc, currentTime);
        
        // Detect rapid movement
        detectRapidMovement(mc);
        
        // Process chunk queue with dynamic limits
        processChunkQueue(mc.theWorld);
        
        // Cleanup and optimization
        if (currentTime - lastProcessTime > TimeUnit.SECONDS.toMillis(5)) {
            performCleanup();
        }
        
        lastProcessTime = currentTime;
        lastPlayerX = mc.thePlayer.posX;
        lastPlayerZ = mc.thePlayer.posZ;
    }
    
    private void updatePerformanceMetrics(Minecraft mc, long currentTime) {
        // Update FPS metrics
        fpsAccumulator += mc.getDebugFPS();
        fpsSampleCount++;
        
        if (currentTime - lastPerformanceCheck >= PERFORMANCE_CHECK_INTERVAL) {
            if (fpsSampleCount > 0) {
                averageFPS = fpsAccumulator / fpsSampleCount;
                
                // Adjust process limit based on FPS
                updateProcessLimit();
            }
            
            fpsAccumulator = 0;
            fpsSampleCount = 0;
            lastPerformanceCheck = currentTime;
        }
    }
    
    private void updateProcessLimit() {
        if (averageFPS < MIN_FPS_THRESHOLD) {
            currentProcessLimit = 1;
        } else if (averageFPS < 30) {
            currentProcessLimit = Math.max(1, BASE_CHUNK_PROCESS_LIMIT - 1);
        } else {
            currentProcessLimit = BASE_CHUNK_PROCESS_LIMIT;
        }
        
        if (isMovingFast) {
            currentProcessLimit = Math.max(1, currentProcessLimit - 1);
        }
    }
    
    private void detectRapidMovement(Minecraft mc) {
        double deltaX = Math.abs(mc.thePlayer.posX - lastPlayerX);
        double deltaZ = Math.abs(mc.thePlayer.posZ - lastPlayerZ);
        double movementSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        
        if (movementSpeed > MOVEMENT_DETECTION_THRESHOLD) {
            isMovingFast = true;
            movementCooldown = 20;
            
            // Reduce render distance during fast movement
            if (mc.gameSettings.renderDistanceChunks > 4) {
                mc.gameSettings.renderDistanceChunks = 4;
            }
        } else if (movementCooldown > 0) {
            movementCooldown--;
            if (movementCooldown == 0) {
                isMovingFast = false;
                
                // Gradually restore render distance
                if (mc.gameSettings.renderDistanceChunks < 6) {
                    mc.gameSettings.renderDistanceChunks++;
                }
            }
        }
    }
    
    private void processChunkQueue(World world) {
        int processed = 0;
        long startTime = System.currentTimeMillis();
        long timeLimit = 50; // Maximum processing time per tick (ms)
        
        while (!chunkQueue.isEmpty() && processed < currentProcessLimit) {
            if (System.currentTimeMillis() - startTime > timeLimit) {
                break;
            }
            
            ChunkLoadTask task = chunkQueue.poll();
            if (task == null) break;
            
            ChunkPosition pos = new ChunkPosition(task.x, task.z);
            if (loadedChunks.contains(pos)) continue;
            
            // Skip expired tasks
            if (System.currentTimeMillis() - task.timestamp > TimeUnit.SECONDS.toMillis(5)) {
                continue;
            }
            
            // Process chunk with priority consideration
            if (processChunk(world, task)) {
                loadedChunks.add(pos);
                processed++;
                processedChunksCount.incrementAndGet();
            }
        }
    }
    
    private boolean processChunk(World world, ChunkLoadTask task) {
        try {
            if (!world.getChunkProvider().chunkExists(task.x, task.z)) {
                // Skip low priority chunks during fast movement
                if (isMovingFast && task.priority > 1) {
                    return false;
                }
                
                world.getChunkFromChunkCoords(task.x, task.z);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error loading chunk at " + task.x + "," + task.z + ": " + e.getMessage());
        }
        return false;
    }
    
    public void queueChunkLoad(int chunkX, int chunkZ) {
        if (chunkQueue.size() >= QUEUE_SIZE_LIMIT) {
            return;
        }
        
        Minecraft mc = Minecraft.getMinecraft();
        int priority = calculateChunkPriority(chunkX, chunkZ, mc);
        
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
        
        // Higher priority for chunks closer to player
        if (distance <= 2) return 1;
        if (distance <= 4) return 2;
        return 3;
    }
    
    private void performCleanup() {
        // Remove expired tasks
        long currentTime = System.currentTimeMillis();
        chunkQueue.removeIf(task -> 
            currentTime - task.timestamp > TimeUnit.SECONDS.toMillis(5));
        
        // Clear old loaded chunks tracking
        if (loadedChunks.size() > 100) {
            loadedChunks.clear();
        }
        
        // Reset counters
        processedChunksCount.set(0);
        
        // Aggressive cleanup during fast movement
        if (isMovingFast) {
            chunkQueue.clear();
            System.gc();
        }
    }
    
    public boolean shouldProcessChunk(int chunkX, int chunkZ) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return false;
        
        int playerChunkX = mc.thePlayer.chunkCoordX;
        int playerChunkZ = mc.thePlayer.chunkCoordZ;
        
        int distanceX = Math.abs(chunkX - playerChunkX);
        int distanceZ = Math.abs(chunkZ - playerChunkZ);
        
        int renderDistance = isMovingFast ? 4 : mc.gameSettings.renderDistanceChunks;
        
        return distanceX <= renderDistance && distanceZ <= renderDistance;
    }
}