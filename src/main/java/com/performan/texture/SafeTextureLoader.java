package com.performan.texture;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import java.util.Map;
import java.util.List;
import java.lang.reflect.Field;

public class SafeTextureLoader {
    private Field framesTextureDataField;

    public SafeTextureLoader() {
        try {
            framesTextureDataField = TextureAtlasSprite.class.getDeclaredField("framesTextureData");
            framesTextureDataField.setAccessible(true);
        } catch (Exception e) {
            System.err.println("Failed to initialize texture loader: " + e.getMessage());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onTextureStitchPre(TextureStitchEvent.Pre event) {
        // Disable mipmap generation for the texture map
        event.map.setMipmapLevels(0);
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTextureStitchPost(TextureStitchEvent.Post event) {
        try {
            // Get the sprites map using reflection since it's private
            Field spritesField = TextureMap.class.getDeclaredField("mapUploadedSprites");
            spritesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, TextureAtlasSprite> sprites = (Map<String, TextureAtlasSprite>) spritesField.get(event.map);
            
            // Process each sprite
            for (TextureAtlasSprite sprite : sprites.values()) {
                if (sprite != null && sprite.getFrameCount() > 0) {
                    ensureValidTextureData(sprite);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to access texture sprites: " + e.getMessage());
        }
    }
    
    private void ensureValidTextureData(TextureAtlasSprite sprite) {
        try {
            if (sprite.getFrameTextureData(0) == null && framesTextureDataField != null) {
                // Create a blank texture if data is missing
                int[][] data = new int[1][];
                data[0] = new int[sprite.getIconWidth() * sprite.getIconHeight()];
                
                @SuppressWarnings("unchecked")
                List<int[][]> framesTextureData = (List<int[][]>) framesTextureDataField.get(sprite);
                if (framesTextureData != null) {
                    framesTextureData.clear();
                    framesTextureData.add(data);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to ensure valid texture data for sprite: " + sprite.getIconName());
        }
    }
}