package dev.nazarbatrider.animatedpanorama;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public class AnimatedPanoramaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // ResourceManagerHelper exists in Fabric API for every version from 1.21.2 to 1.21.10.
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new Reloader());
    }

    private static final class Reloader implements SimpleSynchronousResourceReloadListener {
        private final OverlayAnimation delegate = new OverlayAnimation();

        @Override
        public ResourceLocation getFabricId() {
            return ResourceLocation.fromNamespaceAndPath("animated_panorama", "overlay");
        }

        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            delegate.onResourceManagerReload(manager);
        }
    }
}
