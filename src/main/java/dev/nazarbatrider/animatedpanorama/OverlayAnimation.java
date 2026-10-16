package dev.nazarbatrider.animatedpanorama;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

public class OverlayAnimation implements SimpleSynchronousResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("AnimatedPanorama");
    private static final String DIR = "textures/gui/title/background/";
    private static final Identifier CONFIG = Identifier.ofVanilla(DIR + "panorama_overlay_animation.json");
    private static final int MAX_FRAMES = 2048;

    private static volatile List<Identifier> frames = List.of();
    private static volatile double fps = 10.0;

    @Override
    public Identifier getFabricId() {
        return Identifier.of("animated_panorama", "overlay");
    }

    @Override
    public void reload(ResourceManager manager) {
        double newFps = 10.0;
        var cfg = manager.getResource(CONFIG);
        if (cfg.isPresent()) {
            try (BufferedReader r = cfg.get().getReader()) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                JsonElement f = o.get("fps");
                if (f != null) newFps = Math.max(0.1, Math.min(120.0, f.getAsDouble()));
            } catch (Exception e) {
                LOGGER.warn("Could not read {}: {}", CONFIG, e.toString());
            }
        }

        List<Identifier> found = new ArrayList<>();
        for (int i = 0; i < MAX_FRAMES; i++) {
            Identifier id = Identifier.ofVanilla(DIR + "panorama_overlay_" + i + ".png");
            if (manager.getResource(id).isEmpty()) break;
            found.add(id);
        }

        fps = newFps;
        frames = found.size() > 1 ? List.copyOf(found) : List.of();
        LOGGER.info("Panorama overlay animation: {} frame(s), {} fps", found.size(), fps);
    }

    /** Returns the current frame, or the original texture if the pack has no animation. */
    public static Identifier current(Identifier original) {
        List<Identifier> f = frames;
        if (f.isEmpty()) return original;
        int index = (int) ((long) (Util.getMeasuringTimeMs() * fps / 1000.0) % f.size());
        return f.get(index);
    }
}
