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

import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.GeneralPath;
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

        Stroke stroke = new BasicStroke((float) 3);

        for(int i = 0; i < worldPoints.size(); i++){
            drawBox(graphics,worldPoints.get(i),0,defaultColor,stroke,1,false);
        }

        return null;
    }

    private void drawBox(Graphics2D graphics, WorldPoint worldPoint, int radius,
                         Color borderColour, Stroke borderStroke, int size, boolean excludeCorner)
    {
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

        graphics.setStroke(borderStroke);
        graphics.setColor(borderColour);
        graphics.draw(getSquare(worldPoint, radius, size, excludeCorner));
    }

    private int x;
    private int y;

    private GeneralPath getSquare(final WorldPoint worldPoint, final int radius, final int size, boolean excludeCorner)
    {
        GeneralPath path = new GeneralPath();

        if (client.getLocalPlayer() == null)
        {
            return path;
        }

        final int startX = worldPoint.getX() - radius;
        final int startY = worldPoint.getY() - radius;
        final int z = worldPoint.getPlane();

        final int diameter = 2 * radius + size;

        excludeCorner = excludeCorner && radius > 0;

        x = startX;
        y = startY;

        final WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

        final int[] xs = new int[4 * diameter + 1];
        final int[] ys = new int[xs.length];

        for (int i = 0; i < xs.length; i++)
        {
            if (i < diameter)
            {
                xs[0 * diameter + i] = startX + i;
                xs[1 * diameter + i] = startX + diameter;
                xs[2 * diameter + i] = startX + diameter - i;
                xs[3 * diameter + i] = startX;
                ys[0 * diameter + i] = startY;
                ys[1 * diameter + i] = startY + i;
                ys[2 * diameter + i] = startY + diameter;
                ys[3 * diameter + i] = startY + diameter - i;
            }
            else if (i == diameter)
            {
                xs[xs.length - 1] = xs[0];
                ys[ys.length - 1] = ys[0];
            }
            if (excludeCorner && i == 0)
            {
                xs[0 * diameter + i] += 1;
                xs[1 * diameter + i] -= 1;
                xs[2 * diameter + i] -= 1;
                xs[3 * diameter + i] += 1;
                ys[0 * diameter + i] += 1;
                ys[1 * diameter + i] += 1;
                ys[2 * diameter + i] -= 1;
                ys[3 * diameter + i] -= 1;
                x = xs[i];
                y = ys[i];
            }

            boolean hasFirst = false;
            if (playerLocation.distanceTo(new WorldPoint(x, y, z)) < MAX_DRAW_DISTANCE)
            {
                hasFirst = moveTo(path, x, y, z);
            }

            x = xs[i];
            y = ys[i];

            if (hasFirst && playerLocation.distanceTo(new WorldPoint(x, y, z)) < MAX_DRAW_DISTANCE)
            {
                lineTo(path, x, y, z);
            }
        }

        return path;
    }

    private boolean moveTo(GeneralPath path, final int x, final int y, final int z)
    {
        net.runelite.api.Point point = XYToPoint(x, y, z);
        if (point != null)
        {
            path.moveTo(point.getX(), point.getY());
            return true;
        }
        return false;
    }

    private void lineTo(GeneralPath path, final int x, final int y, final int z)
    {
        net.runelite.api.Point point = XYToPoint(x, y, z);
        if (point != null)
        {
            path.lineTo(point.getX(), point.getY());
        }
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
