package dev.nazarbatrider.animatedpanorama.mixin;

import dev.nazarbatrider.animatedpanorama.OverlayAnimation;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PanoramaRenderer.class)
public class PanoramaRendererMixin {
    @Redirect(
        method = "render",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/PanoramaRenderer;PANORAMA_OVERLAY:Lnet/minecraft/resources/Identifier;"
        )
    )
    private Identifier animatedpanorama$swapOverlayFrame() {
        return OverlayAnimation.current(PanoramaRenderer.PANORAMA_OVERLAY);
    }
}
