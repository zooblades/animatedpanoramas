package dev.nazarbatrider.animatedpanorama.mixin;

import dev.nazarbatrider.animatedpanorama.OverlayAnimation;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * PanoramaRenderer.render reads PANORAMA_OVERLAY to draw the overlay. We hand it the current frame instead.
 * The method is matched by name only, because its parameters change in 1.21.6 (the intermediary name does not).
 */
@Mixin(PanoramaRenderer.class)
public class PanoramaRendererMixin {
    @Redirect(
        method = "render",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/PanoramaRenderer;PANORAMA_OVERLAY:Lnet/minecraft/resources/ResourceLocation;"
        )
    )
    private ResourceLocation animatedpanorama$swapOverlayFrame() {
        return OverlayAnimation.current(PanoramaRenderer.PANORAMA_OVERLAY);
    }
}
