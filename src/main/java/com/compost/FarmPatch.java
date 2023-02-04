package com.compost;

import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;

public class FarmPatch{

    public String farmPatch = "";

    public String compostType = "";
    public boolean didICompost = false;
    private static ArrayList<WorldPoint> compostIconTiles = new ArrayList<>();

    public FarmPatch(String patchName, boolean composted, ArrayList<WorldPoint> tiles, String compost){
        farmPatch = patchName;
        didICompost = composted;
        compostIconTiles = tiles;
        compostType = compost;

    }
}
