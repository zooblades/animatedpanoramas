package dev.nazarbatrider.animatedpanorama.mixin;

import dev.nazarbatrider.animatedpanorama.OverlayAnimation;
import net.minecraft.client.renderer.Panorama;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Panorama.extractRenderState draws PANORAMA_OVERLAY. We swap that texture for the current animation frame.
 * Targeting the field read (not the blit call) keeps this independent of RenderPipeline's package,
 * which differs between 26.1/26.2 and 26.3.
 */
@Mixin(Panorama.class)
public class PanoramaMixin {
    @Redirect(
        method = "extractRenderState",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/Panorama;PANORAMA_OVERLAY:Lnet/minecraft/resources/Identifier;"
        )
    )
    private Identifier animatedpanorama$swapOverlayFrame() {
        return OverlayAnimation.current(Panorama.PANORAMA_OVERLAY);
    }
}
