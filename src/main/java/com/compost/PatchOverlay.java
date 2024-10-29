package com.compost;

import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.util.ImageUtil;

import java.awt.Color;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static net.runelite.api.Perspective.LOCAL_TILE_SIZE;


public class PatchOverlay extends Overlay
{
    private final Client client;
    private final DidICompostPlugin plugin;
    private final DidICompostConfig config;

    public List<WorldPoint> getWorldPoints()
    {
        return worldPoints;
    }

    public void setWorldPoints(List<WorldPoint> worldPoints)
    {
        this.worldPoints = worldPoints;
    }

    public List<WorldPoint> worldPoints = new ArrayList<WorldPoint>();
    Color defaultColor = Color.RED;

    private List<WorldPoint> needsCompostPoints = new ArrayList<>();

    public List<WorldPoint> getNeedsCompostPoints() {
        return needsCompostPoints;
    }

    public void setNeedsCompostPoints(List<WorldPoint> points) {
        this.needsCompostPoints = points;
    }

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
        for(int i = 0; i < worldPoints.size(); i++)
        {
            try
            {
                drawBucket(graphics,worldPoints.get(i));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (config.showNeedsCompost()) {
            for (WorldPoint point : needsCompostPoints) {
                try {
                    drawNeedsCompost(graphics, point);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return null;
    }

    private void drawBucket(Graphics2D graphics, WorldPoint worldPoint) throws IOException
    {
        if(worldPoint.getPlane() != client.getPlane())
        {
            return;
        }

        LocalPoint lp = LocalPoint.fromWorld(client, worldPoint);
        if(lp == null)
        {
            return;
        }

        Polygon poly = Perspective.getCanvasTilePoly(client,lp);
        if(poly == null)
        {
            return;
        }


        BufferedImage img = ImageUtil.loadImageResource(DidICompostPlugin.class, "/Bottomless_compost_bucket.png");
        BufferedImage resizedImage = img;

        if(config.iconSize() == CompostIconSize.LARGE) {
            int newWidth = (int)(img.getWidth() * 1.3);
            int newHeight = (int)(img.getHeight() * 1.3);
            resizedImage = resizeImage(img, newWidth, newHeight);
        }
        else if(config.iconSize() == CompostIconSize.SMALL) {
            int newWidth = (int)(img.getWidth() * 0.7);
            int newHeight = (int)(img.getHeight() * 0.7);
            resizedImage = resizeImage(img, newWidth, newHeight);
        }

        net.runelite.api.Point point = XYToPoint(worldPoint.getX(),worldPoint.getY(),worldPoint.getPlane());
        graphics.drawImage(resizedImage, point.getX() , point.getY(), null);
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int newWidth, int newHeight) {
        Image tmp = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return resizedImage;
    }
    private Point XYToPoint(int x, int y, int z)
    {
        LocalPoint localPoint = LocalPoint.fromWorld(client, x, y);

        if (localPoint == null)
        {
            return null;
        }

        return Perspective.localToCanvas(
                client,
                new LocalPoint(localPoint.getX() - LOCAL_TILE_SIZE / 2, localPoint.getY() - LOCAL_TILE_SIZE / 2),
                z);
    }

    private void drawNeedsCompost(Graphics2D graphics, WorldPoint worldPoint) throws IOException {
        if (worldPoint.getPlane() != client.getPlane()) {
            return;
        }

        LocalPoint lp = LocalPoint.fromWorld(client, worldPoint);
        if (lp == null) {
            return;
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly == null) {
            return;
        }

        BufferedImage img = ImageUtil.loadImageResource(DidICompostPlugin.class, "/icon-gray.png");
        BufferedImage resizedImage = img;

        if(config.iconSize() == CompostIconSize.LARGE) {
            int newWidth = (int)(img.getWidth() * 1.3);
            int newHeight = (int)(img.getHeight() * 1.3);
            resizedImage = resizeImage(img, newWidth, newHeight);
        }
        else if(config.iconSize() == CompostIconSize.SMALL) {
            int newWidth = (int)(img.getWidth() * 0.7);
            int newHeight = (int)(img.getHeight() * 0.7);
            resizedImage = resizeImage(img, newWidth, newHeight);
        }

        net.runelite.api.Point point = XYToPoint(worldPoint.getX(), worldPoint.getY(), worldPoint.getPlane());
        graphics.drawImage(resizedImage, point.getX(), point.getY(), null);
    }

}
