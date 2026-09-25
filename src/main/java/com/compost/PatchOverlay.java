package com.compost;

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

public class PatchOverlay extends Overlay
{
    private static final BufferedImage COMPOST_IMG = ImageUtil.loadImageResource(DidICompostPlugin.class, "/Bottomless_compost_bucket.png");
    private static final BufferedImage GRAY_IMG = ImageUtil.loadImageResource(DidICompostPlugin.class, "/icon-gray.png");

    private BufferedImage resizedCompostImage = COMPOST_IMG;
    private BufferedImage resizedGrayImage = GRAY_IMG;

    private final Client client;
    private final DidICompostConfig config;

    @Inject
    PatchOverlay(Client client, DidICompostConfig config)
    {
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    public void updateImages()
    {
        CompostIconSize iconSize = config.iconSize();
        this.resizedCompostImage = resize(COMPOST_IMG, iconSize);
        this.resizedGrayImage = resize(GRAY_IMG, iconSize);
    }

    public void reset()
    {
        this.resizedCompostImage = COMPOST_IMG;
        this.resizedGrayImage = GRAY_IMG;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        WorldView wv = client.getTopLevelWorldView();
        if (wv == null || !config.displayMode().isIcon())
        {
            return null;
        }

        for (FarmingPatches patch : FarmingPatches.values())
        {
            WorldPoint tile = patch.getTile();
            if (tile.getPlane() != wv.getPlane())
            {
                continue;
            }

            LocalPoint lp = LocalPoint.fromWorld(wv, tile);
            if (lp == null)
            {
                continue;
            }

            BufferedImage image;
            if (client.getVarbitValue(patch.getCompostVarbit()) != 0)
            {
                image = config.showAppliedCompost() ? resizedCompostImage : null;
            }
            else
            {
                image = config.showNeedsCompost() ? resizedGrayImage : null;
            }

            if (image != null)
            {
                OverlayUtil.renderImageLocation(client, graphics, lp, image, 0);
            }
        }

        return null;
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
