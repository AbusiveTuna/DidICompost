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


public class PatchOverlay extends Overlay {

    private static final int MAX_DRAW_DISTANCE = 3333;
    private final Client client;
    private final DidICompostPlugin plugin;
    private final DidICompostConfig config;

    public List<WorldPoint> getWorldPoints() {
        return worldPoints;
    }

    public void setWorldPoints(List<WorldPoint> worldPoints) {
        this.worldPoints = worldPoints;
    }

    public List<WorldPoint> worldPoints = new ArrayList<WorldPoint>();
    Color defaultColor = Color.RED;

    @Inject
    private PatchOverlay(Client client, DidICompostConfig config, DidICompostPlugin plugin) {
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics){

        for(int i = 0; i < worldPoints.size(); i++){
            try {
                drawBucket(graphics,worldPoints.get(i));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    private void drawBucket(Graphics2D graphics, WorldPoint worldPoint) throws IOException {
        if(worldPoint.getPlane() != client.getPlane()){
            return;
        }

        LocalPoint lp = LocalPoint.fromWorld(client, worldPoint);
        if(lp == null){
            return;
        }

        Polygon poly = Perspective.getCanvasTilePoly(client,lp);
        if(poly == null){
            return;
        }

        String dir = System.getProperty("user.dir");
        BufferedImage img = ImageIO.read(new File(dir + "\\src\\images\\Bottomless_compost_bucket.png"));

        net.runelite.api.Point point = XYToPoint(worldPoint.getX(),worldPoint.getY(),worldPoint.getPlane());
        graphics.drawImage(img, point.getX() , point.getY(), null);
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


}
