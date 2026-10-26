package dev.nazarbatrider.animatedpanorama;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;

/** 1.21.5+ style constructor: DynamicTexture(Supplier<String> label, NativeImage). */
final class TextureFactory {
    private TextureFactory() {}

    static DynamicTexture create(String label, NativeImage image) {
        return new DynamicTexture(() -> label, image);
    }
}
