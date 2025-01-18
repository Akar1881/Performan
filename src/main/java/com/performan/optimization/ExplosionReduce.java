package com.performan.optimization;

import net.minecraft.world.World;
import net.minecraft.world.Explosion;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraft.util.EnumParticleTypes;
import java.util.List;
import java.util.Random;

public class ExplosionReduce {
    private static final Random random = new Random();
    private static final float PARTICLE_REDUCTION = 0.6f;
    private static final int MAX_AFFECTED_BLOCKS = 100;
    private static final boolean ENABLE_FAST_RAYTRACING = true;
    private static final float DEFAULT_EXPLOSION_SIZE = 4.0f;
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onExplosionStart(ExplosionEvent.Start event) {
        if (event.world.isRemote) {
            optimizeClientExplosion(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.world.isRemote) {
            optimizeExplosionEffects(event);
        } else {
            limitExplosionDamage(event);
        }
    }

    private void optimizeClientExplosion(ExplosionEvent.Start event) {
        if (random.nextFloat() > PARTICLE_REDUCTION) {
            event.setCanceled(true);
            return;
        }

        if (ENABLE_FAST_RAYTRACING) {
            optimizeRayTracing(event.explosion);
        }
    }

    private void optimizeExplosionEffects(ExplosionEvent.Detonate event) {
        Explosion explosion = event.explosion;
        World world = event.world;

        if (world.isRemote) {
            float size = DEFAULT_EXPLOSION_SIZE;
            int particleCount = (int)(size * 2);
            
            for (int i = 0; i < particleCount; i++) {
                if (random.nextFloat() <= PARTICLE_REDUCTION) {
                    continue;
                }

                double posX = explosion.getPosition().xCoord;
                double posY = explosion.getPosition().yCoord;
                double posZ = explosion.getPosition().zCoord;

                double x = posX + (random.nextFloat() - 0.5) * size;
                double y = posY + (random.nextFloat() - 0.5) * size;
                double z = posZ + (random.nextFloat() - 0.5) * size;
                
                world.spawnParticle(
                    random.nextBoolean() ? EnumParticleTypes.SMOKE_NORMAL : EnumParticleTypes.EXPLOSION_NORMAL,
                    x, y, z,
                    (random.nextFloat() - 0.5) * 0.2,
                    (random.nextFloat() - 0.5) * 0.2,
                    (random.nextFloat() - 0.5) * 0.2
                );
            }
        }
    }

    private void limitExplosionDamage(ExplosionEvent.Detonate event) {
        List<Entity> entityList = event.getAffectedEntities();
        
        if (entityList.size() > MAX_AFFECTED_BLOCKS) {
            int removeCount = entityList.size() - MAX_AFFECTED_BLOCKS;
            for (int i = 0; i < removeCount; i++) {
                entityList.remove(entityList.size() - 1);
            }
        }
    }

    private void optimizeRayTracing(Explosion explosion) {
        double stepSize = 0.3;
        int maxSteps = 7;
        
        Vec3 explosionPos = explosion.getPosition();
        float size = DEFAULT_EXPLOSION_SIZE;
        
        double maxDistance = size * 2.0;
        maxDistance *= maxDistance;
    }
}