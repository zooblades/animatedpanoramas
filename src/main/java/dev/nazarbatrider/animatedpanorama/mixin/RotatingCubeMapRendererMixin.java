package dev.nazarbatrider.animatedpanorama.mixin;

import dev.nazarbatrider.animatedpanorama.OverlayAnimation;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(RotatingCubeMapRenderer.class)
public class RotatingCubeMapRendererMixin {
    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIFFIIII)V"
        ),
        index = 0
    )
    private Identifier animatedpanorama$swapOverlayFrame(Identifier original) {
        return OverlayAnimation.current(original);
    }
}
