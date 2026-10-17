package dev.nazarbatrider.animatedpanorama;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OverlayAnimation implements SimpleSynchronousResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("AnimatedPanorama");
    private static final String DIR = "textures/gui/title/background/";
    private static final Identifier CONFIG = Identifier.ofVanilla(DIR + "panorama_overlay_animation.json");
    private static final Identifier GIF = Identifier.ofVanilla(DIR + "panorama_overlay.gif");
    private static final Identifier OVERLAY_PNG = Identifier.ofVanilla(DIR + "panorama_overlay.png");
    private static final int MAX_FRAMES = 2048;
    private static final long MAX_PIXELS = 64L * 1024 * 1024; // ~256 MB of RGBA

    private record Frame(Identifier id, int durationMs) {}

    private static final List<Identifier> dynamicTextures = new ArrayList<>();
    private static volatile Frame[] frames = new Frame[0];
    private static volatile long totalMs = 0;

    @Override
    public Identifier getFabricId() {
        return Identifier.of("animated_panorama", "overlay");
    }

    @Override
    public void reload(ResourceManager manager) {
        TextureManager tm = MinecraftClient.getInstance().getTextureManager();
        for (Identifier id : dynamicTextures) tm.destroyTexture(id);
        dynamicTextures.clear();
        frames = new Frame[0];
        totalMs = 0;

        Config cfg = readConfig(manager);
        Double cfgFps = cfg.fps();

        Frame[] result = null;
        var gif = manager.getResource(GIF);
        if (gif.isPresent()) {
            try {
                result = loadGif(gif.get(), cfgFps, tm);
            } catch (Throwable e) {
                LOGGER.error("Failed to load panorama_overlay.gif", e);
                for (Identifier id : dynamicTextures) tm.destroyTexture(id);
                dynamicTextures.clear();
            }
        }
        if (result == null) {
            double fps = cfgFps != null ? cfgFps : 10.0;
            result = loadPngFrames(manager, fps);
            if (result.length < 2 && (cfg.frames() != null || cfg.frameHeight() != null)) {
                result = loadStrip(manager, cfg, fps, tm);
            }
        }

        if (result.length > 1) {
            long total = 0;
            for (Frame f : result) total += f.durationMs();
            totalMs = Math.max(1, total);
            frames = result;
        }
        LOGGER.info("Panorama overlay animation: {} frame(s), {} ms loop", result.length, totalMs);
    }

    private record Config(Double fps, Integer frames, Integer frameHeight) {}

    private static Config readConfig(ResourceManager manager) {
        var cfg = manager.getResource(CONFIG);
        if (cfg.isEmpty()) return new Config(null, null, null);
        try (BufferedReader r = cfg.get().getReader()) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            Double fps = o.has("fps") ? Math.max(0.1, Math.min(120.0, o.get("fps").getAsDouble())) : null;
            Integer frames = o.has("frames") ? o.get("frames").getAsInt() : null;
            Integer frameHeight = o.has("frame_height") ? o.get("frame_height").getAsInt() : null;
            return new Config(fps, frames, frameHeight);
        } catch (Exception e) {
            LOGGER.warn("Could not read {}: {}", CONFIG, e.toString());
            return new Config(null, null, null);
        }
    }

    private static Frame[] loadPngFrames(ResourceManager manager, double fps) {
        int duration = Math.max(1, (int) Math.round(1000.0 / fps));
        List<Frame> found = new ArrayList<>();
        for (int i = 0; i < MAX_FRAMES; i++) {
            Identifier id = Identifier.ofVanilla(DIR + "panorama_overlay_" + i + ".png");
            if (manager.getResource(id).isEmpty()) break;
            found.add(new Frame(id, duration));
        }
        return found.toArray(new Frame[0]);
    }

    /** panorama_overlay.png as a vertical strip: frames stacked top to bottom. */
    private static Frame[] loadStrip(ResourceManager manager, Config cfg, double fps, TextureManager tm) {
        var res = manager.getResource(OVERLAY_PNG);
        if (res.isEmpty()) return new Frame[0];
        try (InputStream in = res.get().getInputStream(); NativeImage src = NativeImage.read(NativeImage.Format.RGBA, in)) {
            int w = src.getWidth(), h = src.getHeight();
            int count = cfg.frames() != null ? cfg.frames() : h / Math.max(1, cfg.frameHeight());
            if (count < 2) {
                LOGGER.warn("panorama_overlay.png strip needs at least 2 frames (image is {}x{})", w, h);
                return new Frame[0];
            }
            int fh = h / count;
            if (fh < 1) throw new IllegalStateException("More frames than image rows (" + count + " frames, height " + h + ")");
            if (h % count != 0) {
                LOGGER.warn("panorama_overlay.png height {} is not divisible by {} frames; the last {} row(s) are ignored",
                        h, count, h - fh * count);
            }
            if (count > MAX_FRAMES || (long) w * fh * count > MAX_PIXELS) {
                throw new IllegalStateException("Strip too large (" + w + "x" + fh + ", " + count + " frames).");
            }

            int ms = Math.max(1, (int) Math.round(1000.0 / fps));
            Frame[] out = new Frame[count];
            for (int i = 0; i < count; i++) {
                NativeImage frame = new NativeImage(NativeImage.Format.RGBA, w, fh, false);
                int offset = i * fh;
                for (int y = 0; y < fh; y++) {
                    for (int x = 0; x < w; x++) {
                        frame.setColor(x, y, src.getColor(x, offset + y)); // raw ABGR copy
                    }
                }
                Identifier id = Identifier.of("animated_panorama", "strip/frame_" + i);
                tm.registerTexture(id, new NativeImageBackedTexture(frame));
                dynamicTextures.add(id);
                out[i] = new Frame(id, ms);
            }
            return out;
        } catch (Throwable e) {
            LOGGER.error("Failed to split panorama_overlay.png into frames", e);
            for (Identifier id : dynamicTextures) tm.destroyTexture(id);
            dynamicTextures.clear();
            return new Frame[0];
        }
    }

    private static Frame[] loadGif(Resource res, Double cfgFps, TextureManager tm) throws Exception {
        try (InputStream in = res.getInputStream(); ImageInputStream iis = ImageIO.createImageInputStream(in)) {
            Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("gif");
            if (!it.hasNext()) throw new IllegalStateException("No GIF reader available");
            ImageReader reader = it.next();
            try {
                reader.setInput(iis, false);
                int n = reader.getNumImages(true);
                int w = reader.getWidth(0), h = reader.getHeight(0);
                try {
                    IIOMetadata sm = reader.getStreamMetadata();
                    if (sm != null) {
                        Node lsd = child(sm.getAsTree("javax_imageio_gif_stream_1.0"), "LogicalScreenDescriptor");
                        if (lsd != null) {
                            w = Math.max(w, attr(lsd, "logicalScreenWidth", w));
                            h = Math.max(h, attr(lsd, "logicalScreenHeight", h));
                        }
                    }
                } catch (Exception ignored) {}

                if (n > MAX_FRAMES || (long) w * h * n > MAX_PIXELS) {
                    throw new IllegalStateException("GIF too large (" + w + "x" + h + ", " + n
                            + " frames). Use fewer frames or a lower resolution.");
                }

                BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Frame[] out = new Frame[n];
                for (int i = 0; i < n; i++) {
                    BufferedImage img = reader.read(i);
                    int left = 0, top = 0, delayCs = 10;
                    String disposal = "none";
                    Node root = reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
                    Node desc = child(root, "ImageDescriptor");
                    if (desc != null) {
                        left = attr(desc, "imageLeftPosition", 0);
                        top = attr(desc, "imageTopPosition", 0);
                    }
                    Node gce = child(root, "GraphicControlExtension");
                    if (gce != null) {
                        delayCs = attr(gce, "delayTime", 10);
                        Node dm = gce.getAttributes().getNamedItem("disposalMethod");
                        if (dm != null) disposal = dm.getNodeValue();
                    }

                    BufferedImage previous = null;
                    if ("restoreToPrevious".equals(disposal)) previous = copy(canvas);

                    Graphics2D g = canvas.createGraphics();
                    g.setComposite(AlphaComposite.SrcOver);
                    g.drawImage(img, left, top, null);
                    g.dispose();

                    Identifier id = Identifier.of("animated_panorama", "gif/frame_" + i);
                    tm.registerTexture(id, new NativeImageBackedTexture(toNative(canvas)));
                    dynamicTextures.add(id);

                    int ms = cfgFps != null ? (int) Math.round(1000.0 / cfgFps)
                            : (delayCs <= 1 ? 100 : delayCs * 10);
                    out[i] = new Frame(id, Math.max(1, ms));

                    if ("restoreToBackgroundColor".equals(disposal)) {
                        Graphics2D c = canvas.createGraphics();
                        c.setComposite(AlphaComposite.Clear);
                        c.fillRect(left, top, img.getWidth(), img.getHeight());
                        c.dispose();
                    } else if (previous != null) {
                        canvas = previous;
                    }
                }
                return out;
            } finally {
                reader.dispose();
            }
        }
    }

    private static BufferedImage copy(BufferedImage src) {
        BufferedImage c = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = c.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return c;
    }

    private static NativeImage toNative(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        NativeImage ni = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = px[y * w + x];
                int a = argb >>> 24, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
                ni.setColor(x, y, (a << 24) | (b << 16) | (g << 8) | r); // ABGR
            }
        }
        return ni;
    }

    private static Node child(Node parent, String name) {
        if (parent == null) return null;
        for (Node c = parent.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (name.equals(c.getNodeName())) return c;
        }
        return null;
    }

    private static int attr(Node node, String name, int fallback) {
        NamedNodeMap m = node.getAttributes();
        Node a = m == null ? null : m.getNamedItem(name);
        if (a == null) return fallback;
        try { return Integer.parseInt(a.getNodeValue()); } catch (NumberFormatException e) { return fallback; }
    }

    /** Returns the current frame, or the original texture if the pack has no animation. */
    public static Identifier current(Identifier original) {
        Frame[] f = frames;
        long total = totalMs;
        if (f.length == 0 || total <= 0) return original;
        long t = Util.getMeasuringTimeMs() % total;
        for (Frame fr : f) {
            if (t < fr.durationMs()) return fr.id();
            t -= fr.durationMs();
        }
        return f[f.length - 1].id();
    }
}
