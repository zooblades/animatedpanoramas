package dev.nazarbatrider.animatedpanorama;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

/**
 * DynamicTexture's constructor changed in 1.21.5:
 *   1.21.2 - 1.21.4: DynamicTexture(NativeImage)
 *   1.21.5+:         DynamicTexture(Supplier<String> label, NativeImage)
 * One jar covers both, so the right constructor is picked at runtime.
 */
final class TextureFactory {
    private static final Constructor<DynamicTexture> WITH_LABEL = find(Supplier.class, NativeImage.class);
    private static final Constructor<DynamicTexture> WITHOUT_LABEL = WITH_LABEL == null ? find(NativeImage.class) : null;

    private TextureFactory() {}

    private static Constructor<DynamicTexture> find(Class<?>... types) {
        try {
            return DynamicTexture.class.getConstructor(types);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    static DynamicTexture create(String label, NativeImage image) {
        try {
            if (WITH_LABEL != null) {
                Supplier<String> supplier = () -> label;
                return WITH_LABEL.newInstance(supplier, image);
            }
            return WITHOUT_LABEL.newInstance(image);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create DynamicTexture", e);
        }
    }
}
