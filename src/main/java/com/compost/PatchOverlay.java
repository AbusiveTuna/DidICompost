package com.compost;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class PatchOverlay extends Overlay
{
    private static final BufferedImage COMPOST_IMG = ImageUtil.loadImageResource(DidICompostPlugin.class, "/Bottomless_compost_bucket.png");
    private static final BufferedImage GRAY_IMG = ImageUtil.loadImageResource(DidICompostPlugin.class, "/icon-gray.png");

    private final Client client;
    private final DidICompostPlugin plugin;
    private final DidICompostConfig config;

    @Getter
    private final Set<WorldPoint> worldPoints = new CopyOnWriteArraySet<>();

    @Getter
    private final Set<WorldPoint> needsCompostPoints = new CopyOnWriteArraySet<>();

    @Inject
    private PatchOverlay(Client client, DidICompostConfig config, DidICompostPlugin plugin)
    {
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        WorldView wv = client.getTopLevelWorldView();
        if (wv == null)
        {
            return null;
        }

        CompostIconSize iconSize = config.iconSize();
        BufferedImage bucketImage = resize(COMPOST_IMG, iconSize);
        for (WorldPoint point : worldPoints)
        {
            drawImage(client, wv, point, graphics, bucketImage);
        }

        if (config.showNeedsCompost()) {
            BufferedImage compostImage = resize(GRAY_IMG, iconSize);
            for (WorldPoint point : needsCompostPoints) {
                drawImage(client, wv, point, graphics, compostImage);
            }
        }

        return null;
    }

    private void drawImage(Client client, WorldView wv, WorldPoint worldPoint, Graphics2D graphics, BufferedImage image)
    {
        LocalPoint lp = LocalPoint.fromWorld(wv, worldPoint);
        if(lp != null)
        {
            OverlayUtil.renderImageLocation(client, graphics, lp, image, worldPoint.getPlane());
        }
    }

    private static BufferedImage resize(BufferedImage img, CompostIconSize iconSize)
    {
        if (iconSize == CompostIconSize.MEDIUM) return img;
        double multiplier = iconSize == CompostIconSize.LARGE ? 1.3 : 0.7;
        int newWidth = (int) (img.getWidth() * multiplier);
        int newHeight = (int) (img.getHeight() * multiplier);
        return ImageUtil.resizeImage(img, newWidth, newHeight);
    }

}
